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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;
import org.example.CSVHelper.CSVMeth;
import org.example.CSVHelper.CSVRow;
import org.example.Helper.*;
import org.example.Initialiser.DataInitialiser;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;


@Route("checking")
@CssImport("./styles/checking.css")
@PageTitle("List&Check")
@PermitAll
public class Checking extends VerticalLayout implements BeforeEnterObserver
{

    /**
     * Temporary list of students from the last DataProvider fetch (current page only).
     * WARNING: gets overwritten on every scroll/refresh — only reliable immediately
     * after a fetch. Used exclusively by {@code CSVMeth.applyCsv} during CSV import,
     * which is triggered manually and runs right after a fetch cycle.
     */
    public List<StudentsPayment> studentsPaymentList;

    /**
     * Map of all payments loaded from DB for the selected year.
     * Structure: studentId -> Set of paid month numbers (1-12).
     * Refreshed on year change, page enter, and after validation.
     */
    Map<Integer, Set<Integer>> allPayments;

    /**
     * Maps UI-level names (e.g. "M1 INT") to their DB equivalents.
     * Structure: UI label -> [level, parcours] (parcours may be null).
     * Used to build SQL WHERE clauses from the level filter selection.
     */
    Map<String, String[]> mappingLevel;

    /**
     * Pending checkbox changes not yet confirmed/saved to DB.
     * Structure: studentId -> (monthNumber -> isPaid).
     * Cleared after validation or cancellation.
     */
    Map<Integer, Map<Integer, Boolean>> modifications = new HashMap<>();

    /**
     * Stores the StudentsPayment object for each student with pending modifications.
     * Keyed by student ID. Mirrors the keys in {@code modifications}.
     * Needed because the DataProvider may have scrolled past those students,
     * making them unavailable via the grid. Cleared alongside {@code modifications}.
     */
    Map<Integer, StudentsPayment> modifiedStudents = new HashMap<>();


    /**
     * List of available academic years shown in the year selector.
     * Currently only contains the start year (2026), but structured
     * as a list to allow future expansion.
     */
    List<Integer> yearsList;

    /**
     * Dropdown to select the academic year to display payments for.
     * Changing this triggers a grid column rebuild and data refresh.
     */
    Select<Integer> yearSelect;

    /**
     * Multi-select filter for student levels (L1, L2, M1 INT, etc.).
     * "TOUS" means no level filter is applied.
     * Changing this triggers a data refresh.
     */
    MultiSelectComboBox<String> levelSelect;



    /**
     * Main data grid displaying students and their monthly payment checkboxes.
     * Columns are rebuilt on year change via {@code upDateGrid()}.
     */
    Grid<StudentsPayment> grid;

    /**
     * Upload component for importing payments via CSV file.
     * Only enabled for ADMIN users.
     */
    Upload uploadCSV;

    /**
     * Today's date, used to determine how many months to display
     * (only shows months up to the current month for the current year).
     */
    LocalDate today;

    /**
     * Current calendar year at the time of the last grid update.
     */
    int currentYear;

    /**
     * Current month number (1-12) at the time of the last grid update.
     */
    int currentMonth;

    /**
     * The first supported academic year. Defines the lower bound
     * of the year selector and the column headers.
     */
    int startYear;

    /**
     * Single-element array holding the current search filter string.
     * Array wrapper allows mutation inside lambda expressions.
     */
    String[] filter = {""};

    /**
     * Service handling all DB read/write operations for payments.
     * Initialized once with the shared DB connection.
     */
    PaymentService paymentService;

    /**
     * Text input for searching students by name or firstname.
     * Uses LAZY mode to avoid querying on every keystroke.
     */
    TextField searchField;

    /**
     * Available level options shown in the level multi-select filter.
     * Includes "TOUS" as the default catch-all option.
     */
    String[] selectableLevel;


    /**
     * Current Spring Security authentication context.
     * Used to determine the logged-in user's roles.
     */
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    /**
     * True if the authenticated user has the ROLE_ADMIN authority.
     * Admins can edit checkboxes, validate payments, and upload CSVs.
     */
    boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

    /**
     * True if the authenticated user has the ROLE_GUEST authority.
     * Guests have read-only access to the payment grid.
     */
    boolean isGuest = auth.getAuthorities().stream()
            .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_GUEST"));


    public Checking()
    {

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

            uploadCSV.setEnabled(isAdmin);

            DataProvider<StudentsPayment, Void> provider = DataProvider.fromCallbacks(
                    query ->
                    {
                        String f = "%" + filter[0].toLowerCase() + "%";

                        Set<String> selected = levelSelect.getValue();
                        List<String> dbLevels = new ArrayList<>();

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

                        try (PreparedStatement ps = con.prepareStatement(sql.toString()))
                        {

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
                                    } catch (SQLException e)
                                    {
                                        throw new RuntimeException(e);
                                    }
                                });

                                if (rs.getDate("payment_date") != null)
                                {
                                    sp.addPayment(new Payment(
                                            id,
                                            rs.getInt("paid_month"),
                                            rs.getInt("paid_year"),
                                            rs.getDate("payment_date").toLocalDate(),
                                            rs.getString("status")
                                    ));
                                }
                            }

                        } catch (SQLException e)
                        {
                            throw new RuntimeException(e);
                        }

                        studentsPaymentList = new ArrayList<>(map.values());
                        return studentsPaymentList.stream();
                    },

                    query ->
                    {

                        String f = "%" + filter[0].toLowerCase() + "%";

                        Set<String> selected = levelSelect.getValue();
                        List<String> dbLevels = new ArrayList<>();

                        if (!selected.contains("TOUS")) {
                            for (String lvl : selected) {
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

                        StringBuilder countSql = getBuilder(dbLevels);

                        try (PreparedStatement ps = con.prepareStatement(countSql.toString()))
                        {

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

                        return (0);
                    }
            );


            grid = new Grid<>(StudentsPayment.class, false);
            upDateGrid();

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

            for (int i = 0; i < selectedLevels.size(); i++)
            {
                String levelKey = selectedLevels.get(i);
                String[] map = mappingLevel.get(levelKey);
                String lvl = map[0];
                String parcours = map[1];

                sql.append("(u.level = ?");

                if (parcours != null)
                    sql.append(" AND u.parcours = ?");

                sql.append(")");

                if (i < selectedLevels.size() - 1)
                    sql.append(" OR ");
            }

            sql.append(")");
        }

        sql.append(" ORDER BY u.id ASC LIMIT ? OFFSET ?");
        return (sql);
    }

    private Button validate(Map<Integer, Map<Integer, Boolean>> modifications, PaymentService paymentService)
    {
        Button validerBtn = new Button("Valider");
        validerBtn.setEnabled(isAdmin);
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
            StudentsPayment student = modifiedStudents.get(studentId);

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

        Button confirmer = new Button("Confirmer l'enregistrement", ev ->
        {
            try {
                int totalChanges = 0;
                for (var entry : modifications.entrySet())
                {
                    int studentId = entry.getKey();
                    for (var moisEntry : entry.getValue().entrySet())
                    {
                        int mois = moisEntry.getKey();
                        paymentService.updatePaymentDB(studentId, yearSelect.getValue(), mois, LocalDate.now());
                        totalChanges++;
                    }
                }

                // Clear modifications BEFORE refreshing UI
                modifications.clear();
                modifiedStudents.clear();
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
                String errorDetail = ex.getMessage().contains("foreign key")
                        ? "Erreur d'intégrité : L'étudiant n'existe pas dans la base."
                        : "Problème de connexion à la base de données.";

                showNotification(errorDetail, VaadinIcon.EXCLAMATION_CIRCLE, NotificationVariant.LUMO_ERROR);

                ex.printStackTrace();
            }
        });

        confirmer.addClassName("validate-btn");
        confirmer.setEnabled(isAdmin);

        Button annuler = new Button("Annuler", ev -> dialog.close());

        dialog.add(dialogLayout);
        dialog.getFooter().add(annuler, confirmer);
        dialog.open();
    }

    private void loadPaymentFromDB()
    {
        allPayments = paymentService.getAllPaymentsForYear(yearSelect.getValue());
    }

    private void showNotification(String message, VaadinIcon icon, NotificationVariant variant)
    {
        Icon vaadinIcon = icon.create();

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
                if (!isAdmin)
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

                            modifiedStudents.remove(sp.getID());
                        }
                    } else
                    {
                        modifications
                                .computeIfAbsent(sp.getID(), k -> new HashMap<>())
                                .put(month, newValue);

                        modifiedStudents.put(sp.getID(), sp);
                    }
                });
                return (cb);
            }).setHeader(moisNom + " " + startYear).setAutoWidth(true).setFlexGrow(1);

        }

    }

    private Upload createCsvUpload(PaymentService paymentService)
    {
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

            } catch (Exception e)
            {
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

        this.currentYear = today.getYear();
        this.currentMonth = today.getMonthValue();
        this.today = LocalDate.now();

        loadPaymentFromDB();
        upDateGrid();
    }
}
