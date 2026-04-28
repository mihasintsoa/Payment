package org.example.Checking;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;


import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.example.CSVHelper.CSVMeth;
import org.example.CSVHelper.CSVRow;
import org.example.Helper.*;
import org.example.Initialiser.DataInitialiser;
import org.example.LoginView;
import org.example.Session.UserSession;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Collections;


@Route("checking")
@CssImport("./styles/checking.css")
public class Checking extends VerticalLayout implements BeforeEnterObserver {

    public List<StudentsPayment> studentsPaymentList;

    Map<Integer, Set<Integer>> allPayments;
    Map<String, String[]> mappingLevel;
    Map<Integer, Map<Integer, Boolean>> modifications = new HashMap<>();

    List<Integer> yearsList;

    Select<Integer> yearSelect;
    MultiSelectComboBox<String> levelSelect;


    Grid<StudentsPayment> grid;
    Upload uploadCSV;
    LocalDate today;
    int currentYear;
    int currentMonth;
    int startYear;

    String[] filter = {""};

    PaymentService paymentService;

    TextField searchField;

    String[] selectableLevel;



    public Checking() {

        setHeightFull();

        today = LocalDate.now();
        currentYear = today.getYear();
        currentMonth = today.getMonthValue();

        startYear = 2026;

        yearsList = new ArrayList<>();
        for (int i = startYear; i <= startYear; i++)
            yearsList.add(i);

        HorizontalLayout hor = new HorizontalLayout();
        hor.setAlignItems(Alignment.CENTER);
        hor.setJustifyContentMode(JustifyContentMode.CENTER);
        hor.setWidthFull();

        yearSelect = new Select<>();
        yearSelect.setPlaceholder("Choisir une année");
        yearSelect.setItems(yearsList);
        yearSelect.setValue(startYear);

        selectableLevel = new String[]{
                "TOUS",
                "L1",
                "L2",
                "L3",
                "M1 INT",
                "M1 MISA",
                "M2"};

        levelSelect = new MultiSelectComboBox<>();
        levelSelect.setPlaceholder("Choisir le ou les niveaux");
        levelSelect.setItems(selectableLevel);
        levelSelect.setValue(selectableLevel[0]);


        mappingLevel = Map.of(
                "L1", new String[]{"L1", null},
                "L2", new String[]{"L2", null},
                "L3", new String[]{"L3", null},
                "M1 INT", new String[]{"M1, Innovation et Technologie"},
                "M1 MISA", new String[]{"M1, Mathematiques Informatique et Statistiques Appliquees"},
                "M2", new String[]{"M2", null}

        );


        try {
            Connection con = DriverManager.getConnection(
                    DataInitialiser.url,
                    DataInitialiser.user,
                    DataInitialiser.pwd);

            paymentService = new PaymentService(con);

            uploadCSV = createCsvUpload(paymentService);

            uploadCSV.setEnabled(UserSession.isAdmin());

            DataProvider<StudentsPayment, Void> provider = DataProvider.fromCallbacks(
                    query ->
                    {
                        String f = "%" + filter[0].toLowerCase() + "%";

                        Set<String> selected = levelSelect.getValue();
                        List<String> dbLevels = new ArrayList<>();

                        System.out.println(selected);

                        if (!selected.contains("TOUS"))
                        {
                            for (String lvl : selected)
                            {
                                String[] mapped = mappingLevel.get(lvl);

                                if (mapped != null)
                                {
                                    for (String m : mapped)
                                    {
                                        if (m != null)
                                            dbLevels.add(m);
                                    }
                                }
                            }
                        }

                        StringBuilder sql = getStringBuilder(dbLevels);

                        Map<Integer, StudentsPayment> map = new HashMap<>();

                        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {

                            int index = 1;

                            ps.setString(index++, f);
                            ps.setString(index++, f);

                            for (String lvl : dbLevels)
                                ps.setString(index++, lvl);

                            ps.setInt(index++, query.getLimit());
                            ps.setInt(index++, query.getOffset());

                            ResultSet rs = ps.executeQuery();

                            while (rs.next()) {
                                int id = rs.getInt("id");

                                StudentsPayment sp = map.computeIfAbsent(id, k ->
                                {
                                    try {
                                        return new StudentsPayment(new Students(
                                                id,
                                                rs.getString("name"),
                                                rs.getString("firstname"),
                                                rs.getString("level")
                                        ));
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                                if (rs.getDate("payment_date") != null) {
                                    sp.addPayment(new Payment(
                                            id,
                                            rs.getInt("paid_month"),
                                            rs.getInt("paid_year"),
                                            rs.getDate("payment_date").toLocalDate(),
                                            rs.getString("status")
                                    ));
                                }
                            }

                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        studentsPaymentList = new ArrayList<>(map.values());
                        return studentsPaymentList.stream();
                    },

                    query -> {

                        String f = "%" + filter[0].toLowerCase() + "%";

                        Set<String> selected = levelSelect.getValue();
                        List<String> dbLevels = new ArrayList<>();

                        if (!selected.contains("TOUS")) {
                            for (String lvl : selected) {
                                String[] mapped = mappingLevel.get(lvl);

                                if (mapped != null) {
                                    for (String m : mapped) {
                                        if (m != null)
                                            dbLevels.add(m);

                                    }
                                }
                            }
                        }

                        StringBuilder countSql = getBuilder(dbLevels);

                        try (PreparedStatement ps = con.prepareStatement(countSql.toString())) {

                            int index = 1;

                            ps.setString(index++, f);
                            ps.setString(index++, f);
                            ps.setString(index++, f);

                            for (String lvl : dbLevels) {
                                ps.setString(index++, lvl);
                            }

                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) return rs.getInt(1);

                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        return 0;
                    }
            );


            grid = new Grid<>(StudentsPayment.class, false);
            upDateGrid();

            //change the year shown based on the selected year
            yearSelect.addValueChangeListener(event ->
            {
                int selectedYears = event.getValue();

                showNotification("Année sélectionnées: " + selectedYears,
                            VaadinIcon.INFO_CIRCLE_O,
                        NotificationVariant.INFO);

                upDateGrid();
                grid.getDataProvider().refreshAll();
            });

            searchField = new TextField();
            searchField.setPlaceholder("Recherchez un etudiant ...");
            searchField.setValueChangeMode(ValueChangeMode.LAZY);
            searchField.addValueChangeListener(e ->
            {
                filter[0] = e.getValue();
                grid.getDataProvider().refreshAll();
            });

            levelSelect.addValueChangeListener(e -> {
                grid.getDataProvider().refreshAll();
            });


            grid.setDataProvider(provider);

            grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            grid.getStyle().set("overflow-x", "auto");


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        hor.add(searchField, levelSelect);
        add(hor);
        add(grid);

        HorizontalLayout hor2 = new HorizontalLayout(validate(modifications, paymentService), yearSelect, uploadCSV);
        hor2.setAlignItems(Alignment.CENTER);
        hor2.setJustifyContentMode(JustifyContentMode.CENTER);
        hor2.setWidthFull();

        add(hor2);
    }

    private static StringBuilder getBuilder(List<String> dbLevels)
    {
        StringBuilder countSql = new StringBuilder(
                """
                            SELECT COUNT(*)
                            FROM users_full u
                            WHERE (LOWER(u.name) LIKE ?
                                OR LOWER(u.firstname) LIKE ?
                                OR LOWER(u.inscription_number) LIKE ?)
                        """);

        if (!dbLevels.isEmpty())
        {
            countSql.append(" AND u.level IN (");
            for (int i = 0; i < dbLevels.size(); i++)
            {
                countSql.append("?");
                if (i < dbLevels.size() - 1)
                    countSql.append(", ");
            }
            countSql.append(")");
        }
        return (countSql);
    }

    private StringBuilder getStringBuilder(List<String> selectedLevels)
    {
        StringBuilder sql = new StringBuilder(
                """
                        SELECT u.id, u.name, u.firstname, u.level, u.parcours,
                               p.paid_month, p.paid_year, p.payment_date, p.status
                        FROM users_full u
                        LEFT JOIN payment p ON u.id = p.student_id
                        WHERE (LOWER(u.name) LIKE ? OR LOWER(u.firstname) LIKE ?)
                        """
        );

        if (!selectedLevels.isEmpty() && !selectedLevels.contains("TOUS"))
        {
            sql.append(" AND (");

            for (int i = 0; i < selectedLevels.size(); i++) {
                String levelKey = selectedLevels.get(i);
                String[] map = mappingLevel.get(levelKey);
                String lvl = map[0];
                String parcours = map[1];

                sql.append("(u.level = ?");

                if (parcours != null) {
                    sql.append(" AND u.parcours = ?");
                }

                sql.append(")");

                if (i < selectedLevels.size() - 1) {
                    sql.append(" OR ");
                }
            }

            sql.append(")");
        }

        sql.append(" ORDER BY u.id ASC LIMIT ? OFFSET ?");
        return (sql);
    }

    private Button validate(Map<Integer, Map<Integer, Boolean>> modifications, PaymentService paymentService)
    {
        Button validerBtn = new Button("Valider");
        validerBtn.setEnabled(UserSession.isAdmin());
        validerBtn.addClassName("validate-btn");

        validerBtn.addClickListener(e ->
        {
            if (modifications.isEmpty())
            {
                Notification.show("Aucune modification à valider.");
                return;
            }

            openConfirmationDialog(modifications, paymentService);
        });

        return (validerBtn);
    }

    private void openConfirmationDialog(Map<Integer, Map<Integer, Boolean>> modifications, PaymentService paymentService)
    {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Confirmer les paiements");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);

        dialogLayout.setMaxHeight("400px");
        dialogLayout.getStyle().set("overflow-y", "auto");
        dialogLayout.setWidthFull();

        modifications.forEach((studentId, moisMap) ->
        {
            StudentsPayment student = findStudentById(studentId);

            moisMap.forEach((mois, valeur) -> {
                String moisNom = Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
                String status = valeur ? "PAYÉ" : "NON-PAYÉ";

                Span line = new Span();

                Span idSpan = new Span("#" + studentId);
                idSpan.getStyle().set("color", "#005fb8");
                idSpan.getStyle().set("font-weight", "bold");
                idSpan.getStyle().set("margin-right", "8px");

                String infoTexte = String.format("%s %s - %s : %s",
                        student.getName(),
                        student.getFirstName(),
                        moisNom,
                        status);

                line.add(idSpan, new Span(infoTexte));

                line.getStyle().set("font-size", "14px");
                dialogLayout.add(line);
            });
        });

        Button confirmer = new Button("Confirmer l'enregistrement", ev -> {
            try {
                int totalChanges = 0;
                for (var entry : modifications.entrySet()) {
                    int studentId = entry.getKey();
                    for (var moisEntry : entry.getValue().entrySet()) {
                        int mois = moisEntry.getKey();
                        paymentService.updatePaymentDB(studentId, yearSelect.getValue(), mois, LocalDate.now());
                        totalChanges++;
                    }
                }

                // Clear modifications BEFORE refreshing UI
                modifications.clear();
                dialog.close();

                // Refresh data
                loadPaymentFromDB();
                grid.getDataProvider().refreshAll();

                // Dynamic success message
                String successMsg = (totalChanges > 1)
                        ? String.format("%d paiements enregistrés avec succès !", totalChanges)
                        : "Le paiement a été enregistré avec succès !";

                showNotification(successMsg, VaadinIcon.CHECK_CIRCLE, NotificationVariant.LUMO_SUCCESS);

            } catch (SQLException ex) {
                // Better error handling: Don't show the whole stack trace to the user
                String errorDetail = ex.getMessage().contains("foreign key")
                        ? "Erreur d'intégrité : L'étudiant n'existe pas dans la base."
                        : "Problème de connexion à la base de données.";

                showNotification(errorDetail, VaadinIcon.EXCLAMATION_CIRCLE, NotificationVariant.LUMO_ERROR);

                // Log the full error for yourself in the console
                ex.printStackTrace();
            }
        });

        confirmer.addClassName("validate-btn");
        confirmer.setEnabled(UserSession.isAdmin());

        Button annuler = new Button("Annuler", ev -> dialog.close());

        dialog.add(dialogLayout);
        dialog.getFooter().add(annuler, confirmer);
        dialog.open();
    }

    private StudentsPayment findStudentById(int id) {
        return studentsPaymentList.stream()
                .filter(s -> s.getID() == id)
                .findFirst()
                .orElse(null);
    }

    private void loadPaymentFromDB() {
        allPayments = paymentService.getAllPaymentsForYear(yearSelect.getValue());
    }

    private void showNotification(String message, VaadinIcon icon, NotificationVariant variant)
    {
        Icon vaadinIcon = icon.create();
        //vaadinIcon.setColor(variant == NotificationVariant.LUMO_ERROR ? "orange"
        //                            : variant == NotificationVariant.INFO ? "yellow": "green" );

        Span text = new Span(message);
        HorizontalLayout content = new HorizontalLayout(vaadinIcon, text);
        content.setAlignItems(Alignment.CENTER);

        Notification notification = new Notification(content);
        notification.addThemeVariants(variant);
        notification.setDuration(3000);
        notification.open();
    }

    private void upDateGrid()
    {
        grid.removeAllColumns();

        today = LocalDate.now();
        currentMonth = today.getMonthValue();
        currentYear = today.getYear();
        int selectYear = yearSelect.getValue();

        //choose before the current year (the start year is 2026 so can't show 2025, ...)

        int monthsToShow = (selectYear < today.getYear()) ? 12 : currentMonth;

        grid.addColumn(StudentsPayment::getID).setHeader("id").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(StudentsPayment::getName).setHeader("Nom").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(StudentsPayment::getFirstName).setHeader("Prénom").setAutoWidth(true).setFlexGrow(1);

        for (int i = 1; i <= monthsToShow; i++)
        {
            final int month = i;
            String moisNom = Month.of(month).getDisplayName(TextStyle.FULL, Locale.FRENCH);

            grid.addComponentColumn(sp ->
            {
                boolean paid = allPayments.getOrDefault(
                                sp.getID(), Collections.emptySet())
                        .contains(month);
                Checkbox cb = new Checkbox(paid);

                if (paid) cb.setEnabled(false);
                if (!UserSession.isAdmin())
                {
                    cb.setReadOnly(true);
                    cb.addClassName("read-only-checkbox");
                }

                cb.addValueChangeListener(e ->
                {
                    boolean newValue = e.getValue();

                    boolean initialValue = allPayments
                            .getOrDefault(sp.getID(), Collections.emptySet())
                            .contains(month);

                    if (newValue == initialValue)
                    {
                        if (modifications.containsKey(sp.getID()))
                        {
                            modifications.get(sp.getID()).remove(month);

                            if (modifications.get(sp.getID()).isEmpty())
                                modifications.remove(sp.getID());
                        }
                    } else
                    {
                        modifications
                                .computeIfAbsent(sp.getID(), k -> new HashMap<>())
                                .put(month, newValue);
                    }
                });
                return (cb);
            }).setHeader(moisNom + " " + startYear).setAutoWidth(true).setFlexGrow(1);

        }

    }

    private Upload createCsvUpload(PaymentService paymentService) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, bytes) ->
        {
            try (InputStream inputStream = new ByteArrayInputStream(bytes))
            {
                List<CSVRow> rows = CSVMeth.parseCSV(inputStream);

                CSVMeth.applyCsv(rows, paymentService, studentsPaymentList);

                showNotification(
                        "Import CSV terminé",
                        VaadinIcon.CHECK_CIRCLE_O,
                        NotificationVariant.LUMO_SUCCESS
                );

                loadPaymentFromDB();
                grid.getDataProvider().refreshAll();

            } catch (Exception e) {
                showNotification(
                        "Erreur import CSV",
                        VaadinIcon.CLOSE_CIRCLE_O,
                        NotificationVariant.LUMO_ERROR
                );
            }
        }));

        upload.setAcceptedFileTypes(".csv");
        return (upload);
    }



    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if (!UserSession.isLoggedIn())
            beforeEnterEvent.rerouteTo(LoginView.class);
        else
        {

            this.today = LocalDate.now();
            this.currentYear = today.getYear();
            this.currentMonth = today.getMonthValue();

            loadPaymentFromDB();
            upDateGrid();
        }
    }
}
