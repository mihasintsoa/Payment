package org.example.Checking;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.html.NativeLabel;



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
import com.vaadin.flow.data.renderer.ComponentRenderer;
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

import org.example.Layout.CheckingLayout;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Route(value = "checking", layout = CheckingLayout.class)
@CssImport("./styles/checking.css")
@PageTitle("List&Check")
@PermitAll
public class Checking extends VerticalLayout implements BeforeEnterObserver
{

    /**
     * Temporary list of students used ONLY for Grid display (DataProvider pagination).
     * This list reflects the current fetched page from the Vaadin Grid and is
     * not guaranteed to contain all students in the database.
     * IMPORTANT:
     * This data must NOT be used for backend processing or business logic
     * (e.g. CSV imports, payments updates). It is strictly for UI rendering.
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
     * This is a variable that we use to hold the status of each level
     * There are two status: active and inactive.
     * The status say whether the corresponding level is blocked or not
     * from accessing Internet or not
     * */
    Map<String, Boolean> levelStatus ;


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


    /**
     * To edit the error given by the uploadCSV
     * */
    Button edit;

    /**
     * Used to validate manually which student have paid
     * without using the uploadCSV
     * */
    Button validateBtn;

    /**
     * To block an entire level
     */
    Button manageLevelStatus;


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
                "TOUS", new String[]{null, null},
                "L1", new String[]{"L1", null},
                "L2", new String[]{"L2", null},
                "L3", new String[]{"L3", null},
                "M1 INT", new String[]{"M1", "Innovation et Technologie"},
                "M1 MISA", new String[]{"M1", "Mathematiques Informatique et Statistiques Appliquees"},
                "M2", new String[]{"M2", null}

        );

        levelStatus = new HashMap<>();

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
                        List<String[]> dbFilters = new ArrayList<>();

                        if (!selected.contains("TOUS"))
                        {
                            for (String lvl : selected)
                            {
                                String[] mapped = mappingLevel.get(lvl);

                                if (mapped != null)
                                {
                                    dbFilters.add(mapped);
                                }
                            }
                        }

                        StringBuilder sql = getStringBuilder(dbFilters);

                        Map<Integer, StudentsPayment> map = new HashMap<>();

                        try (PreparedStatement ps = con.prepareStatement(sql.toString()))
                        {

                            int index = 1;

                            ps.setString(index++, f);
                            ps.setString(index++, f);


                            if (!selected.isEmpty() && !selected.contains("TOUS"))
                            {
                                for (String[] filter : dbFilters)
                                {
                                    String lvl = filter[0];
                                    String parcours = filter[1];

                                    ps.setString(index++, lvl);

                                    if (parcours != null)
                                        ps.setString(index++, parcours);
                                }
                            }

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

                        //studentsPaymentList = new ArrayList<>(map.values());
                        return map.values().stream();
                    },

                    query ->
                    {

                        String f = "%" + filter[0].toLowerCase() + "%";

                        Set<String> selected = levelSelect.getValue();
                        List<String[]> dbFilters = new ArrayList<>();

                        if (!selected.contains("TOUS"))
                        {
                            for (String lvl : selected)
                            {
                                String[] mapped = mappingLevel.get(lvl);

                                if (mapped != null)
                                {
                                    dbFilters.add(mapped);
                                }
                            }
                        }

                        StringBuilder countSql = getBuilder(dbFilters);

                        try (PreparedStatement ps = con.prepareStatement(countSql.toString()))
                        {

                            int index = 1;

                            ps.setString(index++, f);
                            ps.setString(index++, f);

                            if (!selected.isEmpty() && !selected.contains("TOUS"))
                            {
                                for (String levelKey : selected)
                                {
                                    String[] map2 = mappingLevel.get(levelKey);

                                    if (map2 == null) continue;

                                    String lvl = map2[0];
                                    String parcours = map2[1];

                                    ps.setString(index++, lvl);

                                    if (parcours != null)
                                        ps.setString(index++, parcours);
                                }
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
            grid.setSelectionMode(Grid.SelectionMode.SINGLE);
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

            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM prom LIMIT 1"))
            {
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                {
                    levelStatus.put("L1", rs.getBoolean("L1"));
                    levelStatus.put("L2", rs.getBoolean("L2"));
                    levelStatus.put("L3", rs.getBoolean("L3"));
                    levelStatus.put("M1 MISA", rs.getBoolean("M1_MISA"));
                    levelStatus.put("M1 INT", rs.getBoolean("M1_INT"));
                    levelStatus.put("M2", rs.getBoolean("M2"));
                }
            }

            manageLevelStatus = manageLevelsButton(con);


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        edit = editBtn();
        validateBtn = validate(modifications, paymentService);

        levelSelect.setRenderer(new ComponentRenderer<>(item -> {
            boolean active = levelStatus.getOrDefault(item, true);
            Badge badge = new Badge(item, (active) ? VaadinIcon.CHECK.create() : VaadinIcon.CLOSE_SMALL.create());
            badge.addThemeVariants((active) ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);

            return (badge);
        }));


        hor.add(searchField, levelSelect);
        add(hor);
        add(grid);

        if (isAdmin)
        {
            HorizontalLayout hor2 = new HorizontalLayout(edit, validateBtn, yearSelect, uploadCSV, manageLevelStatus);
            hor2.setAlignItems(Alignment.CENTER);
            hor2.setJustifyContentMode(JustifyContentMode.CENTER);
            hor2.setWidthFull();

            add(hor2);
        }


    }

    private static StringBuilder getBuilder(List<String[]> dbFilters)
    {
        StringBuilder countSql = new StringBuilder(
                """
                            SELECT COUNT(*)
                            FROM users_full u
                            WHERE (LOWER(u.name) LIKE ?
                                OR LOWER(u.firstname) LIKE ?)
                        """);

        if (!dbFilters.isEmpty())
        {
            countSql.append(" AND (");

            for (int i = 0; i < dbFilters.size(); i++)
            {
                String[] f = dbFilters.get(i);

                String lvl = f[0];
                String parcours = f[1];

                countSql.append("(u.level = ?");

                if (parcours != null)
                    countSql.append(" AND u.parcours = ?");

                countSql.append(")");

                if (i < dbFilters.size() - 1)
                    countSql.append(" OR ");
            }

            countSql.append(")");
        }
        return (countSql);
    }

    private StringBuilder getStringBuilder(List<String[]> selectedLevels)
    {
        StringBuilder inner = new StringBuilder(
                """
                        SELECT id, name, firstname, level, parcours
                        FROM users_full
                        WHERE (LOWER(name) LIKE ? OR LOWER(firstname) LIKE ?)
                """
        );

        if (!selectedLevels.isEmpty())
        {
            inner.append(" AND (");
            for (int i = 0; i < selectedLevels.size(); i++)
            {
                String[] map = selectedLevels.get(i);
                inner.append("(level = ?");

                if (map[1] != null)
                    inner.append(" AND parcours = ?");

                inner.append(")");

                if (i < selectedLevels.size() - 1)
                    inner.append(" OR ");
            }
            inner.append(")");
        }

        inner.append(" ORDER BY id ASC LIMIT ? OFFSET ?");

        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.name, u.firstname, u.level, u.parcours, " +
                        "       p.paid_month, p.paid_year, p.payment_date, p.status " +
                        "FROM (" + inner + ") u " +
                        "LEFT JOIN payment p ON u.id = p.student_id " +
                        "ORDER BY u.id ASC"
        );

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

    /**
     * Displays a confirmation dialog before applying pending payment modifications.

     * This dialog summarizes all changes made by the user and requires explicit
     * confirmation before updating the database.

     * Once confirmed, all modifications are persisted and the grid is refreshed.
     * This step ensures that payment updates are intentional and auditable.
     */
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

    /**
     * Loads all payment data for the currently selected academic year
     * from the database into memory.

     * This data is used as the source of truth for rendering payment status
     * in the grid.
     */
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

    /**
     * Rebuilds the student payment grid dynamically.

     * Columns are generated based on the current date,
     * showing only the relevant months.

     * Each month column contains a checkbox representing payment status,
     * with business rules applied (admin permissions, already-paid constraints,
     * and modification tracking).

     * This method is called whenever the year changes or the grid needs refresh.
     */
    private void upDateGrid()
    {
        int i = 0;
        int tempI = 1;

        grid.removeAllColumns();

        today = LocalDate.now();
        currentMonth = today.getMonthValue();
        currentYear = today.getYear();
        int selectYear = yearSelect.getValue();

        int monthsToShow = (selectYear < today.getYear()) ? 12 : currentMonth;

        grid.addColumn(StudentsPayment::getID).setHeader("id").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(StudentsPayment::getName).setHeader("Nom").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(StudentsPayment::getFirstName).setHeader("Prénom").setAutoWidth(true).setFlexGrow(1);

        if (selectYear == 2026)
            tempI += 2;

        for (i = tempI; i <= monthsToShow; i++)
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

    /**
     * Creates the CSV upload component used by administrators to import payment data.

     * When a file is uploaded, it is parsed into CSV rows and temporarily stored.
     * The actual database update is NOT executed immediately.

     * Instead, a dialog is opened to allow the user to select the target month
     * before applying the imported payments.

     * This ensures that CSV imports remain controlled and reversible before confirmation.
     */
    private Upload createCsvUpload(PaymentService paymentService)
    {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, bytes) ->
        {
            try (InputStream inputStream = new ByteArrayInputStream(bytes))
            {
                List<CSVRow> rows = CSVMeth.parseCSV(inputStream);

                getUI().ifPresent(ui -> ui.access(() -> openCsvDialog(rows)));

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

    private Button editBtn()
    {
        Button edit = new Button("Edit", VaadinIcon.EDIT.create());
        edit.setEnabled(isAdmin);
        edit.addClassName("edit-btn");

        edit.addClickListener(event ->
        {
            StudentsPayment std = grid.asSingleSelect().getValue();

            openEditDialog(std);
        });

        return (edit);
    }

    /**
     * This button allows administrators to manage the activation
     * status of academic levels directly from the UI.

     * When clicked, it opens a dialog containing one checkbox
     * for each level (L1, L2, L3, M1 INT, M1 MISA, M2).

     * - Checked = the level is active (students of this level
     *   are allowed to access the Internet).
     * - Unchecked = the level is inactive (students of this level
     *   are blocked from accessing the Internet).

     * The initial state of each checkbox reflects the current
     * values stored in the database table "prom".

     * When the user clicks "Save":
     * - The database table "prom" is updated with the new values.
     * - The local map "levelStatus" is refreshed to match.
     * - A notification is displayed to confirm the update.

     * This ensures that the UI and the backend remain synchronized,
     * and that changes to level access policies are immediately
     * reflected in the system.
     */

    private Button manageLevelsButton(Connection con)
    {
        Button btn = new Button("Gérer les niveaux", VaadinIcon.TOOLS.create());
        btn.addClassName("manage-level-btn");

        btn.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Activer/Désactiver les niveaux");

            VerticalLayout layout = new VerticalLayout();

            Map<String, Checkbox> checkboxes = new HashMap<>();
            for (String lvl : selectableLevel) {
                if (lvl.equals("TOUS")) continue;
                Checkbox cb = new Checkbox(lvl);
                cb.setValue(levelStatus.getOrDefault(lvl, true));
                checkboxes.put(lvl, cb);
                layout.add(cb);
            }

            Button saveBtn = new Button("Enregistrer", ev ->
            {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE prom SET L1=?, L2=?, L3=?, M1_MISA=?, M1_INT=?, M2=? WHERE id=1"))
                {

                    ps.setBoolean(1, checkboxes.get("L1").getValue());
                    ps.setBoolean(2, checkboxes.get("L2").getValue());
                    ps.setBoolean(3, checkboxes.get("L3").getValue());
                    ps.setBoolean(4, checkboxes.get("M1 MISA").getValue());
                    ps.setBoolean(5, checkboxes.get("M1 INT").getValue());
                    ps.setBoolean(6, checkboxes.get("M2").getValue());

                    ps.executeUpdate();

                    //update the local status
                    levelStatus.put("L1", checkboxes.get("L1").getValue());
                    levelStatus.put("L2", checkboxes.get("L2").getValue());
                    levelStatus.put("L3", checkboxes.get("L3").getValue());
                    levelStatus.put("M1 MISA", checkboxes.get("M1 MISA").getValue());
                    levelStatus.put("M1 INT", checkboxes.get("M1 INT").getValue());
                    levelStatus.put("M2", checkboxes.get("M2").getValue());

                    dialog.close();
                    showNotification("Niveaux mis à jour !", VaadinIcon.CHECK, NotificationVariant.SUCCESS);
                } catch (SQLException ex)
                {
                    showNotification("Erreur lors de la mise à jour !", VaadinIcon.CLOSE_SMALL, NotificationVariant.ERROR);
                    ex.printStackTrace();
                }

                levelSelect.setRenderer(new ComponentRenderer<>(item -> {
                    boolean active = levelStatus.getOrDefault(item, true);
                    Badge badge = new Badge(item, (active) ? VaadinIcon.CHECK.create() : VaadinIcon.CLOSE_SMALL.create());
                    badge.addThemeVariants((active) ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);

                    return (badge);
                }));
            });
            saveBtn.addClassName("validate-btn");

            Button cancelBtn = new Button("Annuler", ev -> dialog.close());

            dialog.add(layout);
            dialog.getFooter().add(cancelBtn, saveBtn);
            dialog.open();
        });

        return (btn);
    }

    private void openEditDialog(StudentsPayment student)
    {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Paid Months");

        dialog.add(new Span("Use the checkboxes below to deselect months that should not be recorded as paid."));

        MultiSelectComboBox<Integer> monthBox = new MultiSelectComboBox<>();
        monthBox.setLabel("Paid Months");
        monthBox.setItems(IntStream.rangeClosed(1, currentMonth).boxed().toList());

        Set<Integer> originMonth = student.getPayments().stream()
                                            .map(Payment::paid_month)
                                            .collect(Collectors.toSet());
        monthBox.setValue(originMonth);

        dialog.add(monthBox);

        Button cancel = new Button("Cancel", e -> dialog.close());
        Button confirm = new Button("Confirm", e ->
        {
            Set<Integer> correctedMonths = monthBox.getValue();

            Set<Integer> monthToDelete = originMonth.stream()
                            .filter(f -> !correctedMonths.contains(f)).collect(Collectors.toSet());
            System.out.println(monthToDelete);

            if (!monthToDelete.isEmpty())
                paymentService.deleteDB(student.getID(), monthToDelete);


            showNotification("Updated months for " + student.getName() + " " + student.getFirstName()
                    + ": " + correctedMonths, VaadinIcon.CHECK, NotificationVariant.SUCCESS);
            loadPaymentFromDB();
            upDateGrid();

            dialog.close();
        });
        confirm.addClassName("validate-btn");

        HorizontalLayout buttons = new HorizontalLayout(cancel, confirm);
        dialog.getFooter().add(buttons);

        dialog.open();
    }

    /**
     * Opens a confirmation dialog after a CSV file has been uploaded.

     * This dialog allows the user to select the month to which the CSV data
     * should be applied, preventing incorrect automatic assignment.

     * Once confirmed, the CSV data is processed and persisted into the database
     * as payment records for the selected period.
     */
    private void openCsvDialog(List<CSVRow> rows)
    {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Choisir le mois d'application");

        Select<Integer> monthSelect = new Select<>();
        monthSelect.setLabel("Mois");

        monthSelect.setItems(IntStream.rangeClosed(1, currentMonth).boxed().toList());

        int defaultMonth = (today.getDayOfMonth() < 18)
                ? Math.max(1, currentMonth - 1)
                : currentMonth;

        monthSelect.setValue(defaultMonth);

        Span info = new Span("L'import va être appliqué à ce mois.");

        Button cancel = new Button("Annuler", e -> dialog.close());

        Button confirm = new Button("Confirmer", e ->
        {
            try
            {
                int selectedMonth = monthSelect.getValue();

                CSVMeth.applyCsv(
                        rows,
                        paymentService,
                        selectedMonth
                );

                showNotification(
                        "Import CSV terminé",
                        VaadinIcon.CHECK_CIRCLE_O,
                        NotificationVariant.LUMO_SUCCESS
                );

                loadPaymentFromDB();
                grid.getDataProvider().refreshAll();

            } catch (Exception ex)
            {
                showNotification(
                        "Erreur lors de l'application",
                        VaadinIcon.CLOSE_CIRCLE_O,
                        NotificationVariant.LUMO_ERROR
                );
            }

            dialog.close();
        });

        confirm.addClassName("validate-btn");

        dialog.add(info, monthSelect);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {

        this.currentYear = today.getYear();
        this.currentMonth = today.getMonthValue();
        System.out.println(today.getDayOfMonth());
        this.today = LocalDate.now();

        loadPaymentFromDB();
        upDateGrid();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        super.onAttach(attachEvent);

        getUI().ifPresent(ui -> {
            if (getParent().orElse(null) instanceof CheckingLayout layout)
            {
                if (isAdmin)
                    layout.addToActionDrawer(edit, validateBtn, yearSelect, uploadCSV, manageLevelStatus);
                else {

                    VerticalLayout guestMessage = new VerticalLayout();
                    guestMessage.setSpacing(true);
                    guestMessage.setPadding(true);
                    guestMessage.setAlignItems(Alignment.CENTER);


                    guestMessage.getStyle()
                            .set("background", "#fff5f6")
                            .set("border", "1px solid #fce4e7")
                            .set("border-radius", "12px")
                            .set("margin", "10px")
                            .set("font-family", "'Outfit', sans-serif");

                    Span icon = new Span(VaadinIcon.INFO.create());
                    icon.getStyle().set("color", "#c8102e").set("font-size", "24px");

                    Span text = new Span("Consultation uniquement");
                    text.getStyle()
                            .set("color", "#8a1528")
                            .set("font-weight", "600")
                            .set("text-align", "center");

                    Span subText = new Span("Aucune action disponible pour votre profil.");
                    subText.getStyle()
                            .set("color", "#4a4a4a")
                            .set("font-size", "12px")
                            .set("text-align", "center");

                    guestMessage.add(icon, text, subText);

                    layout.addToActionDrawer(guestMessage);
                }
            }
        });
    }
}
