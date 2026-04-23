package org.example.Checking;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.combobox.ComboBox;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import org.example.Helper.*;
import org.example.Initialiser.DataInitialiser;
import org.example.LoginView;
import org.example.Session.UserSession;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Collections;

@Route("checking")
@CssImport("./styles/checking.css")
public class Checking extends VerticalLayout implements BeforeEnterObserver
{

    public List<StudentsPayment> studentsPaymentList;

    Map<Integer, Set<Integer>> allPayments ;

    List<Integer> yearsList;

    Select<Integer> yearSelect;


    Grid<StudentsPayment> grid;
    LocalDate today ;
    int currentYear;
    int currentMonth;
    int startYear;

    String[] filter = {""};

    PaymentService paymentService;

    TextField searchField ;

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

        yearSelect = new Select<>();
        yearSelect.setPlaceholder("Choisir une année");


        yearSelect.setItems(yearsList);
        yearSelect.setValue(startYear);


        Map<Integer, Map<Integer, Boolean>> modifications = new HashMap<>();

        try
        {
            Connection con = DriverManager.getConnection(
                    DataInitialiser.url,
                    DataInitialiser.user,
                    DataInitialiser.pwd);

            paymentService = new PaymentService(con);
            allPayments = paymentService.getAllPaymentsForYear(yearSelect.getValue());

            DataProvider<StudentsPayment, Void> provider = DataProvider.fromCallbacks(

                    query -> {
                        Map<Integer, StudentsPayment> map = new HashMap<>();
                        try (
                                PreparedStatement ps = con.prepareStatement(
                                    """
                                            SELECT u.id, u.name, u.firstname, u.level,
                                                p.paid_month, p.paid_year , p.payment_date, p.status
                                            FROM users_full u
                                            LEFT JOIN payment p ON u.id = p.student_id
                                            WHERE LOWER(u.name) like ? OR
                                                  LOWER(u.firstname) like ?
                                            LIMIT ? OFFSET ?
                                """)
                        ) {

                            String f = "%" + filter[0].toLowerCase() + "%";

                            ps.setString(1, f);
                            ps.setString(2, f);
                            ps.setInt(3, query.getLimit());
                            ps.setInt(4, query.getOffset());

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
                                        }
                                );

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

                    query ->
                    {
                        try (PreparedStatement ps = con.prepareStatement(
                                """
                                        SELECT COUNT(*)
                                        FROM users_full u
                                        WHERE LOWER(u.name) LIKE ?
                                           OR LOWER(u.firstname) LIKE ?
                                           OR LOWER(u.inscription_number) LIKE ?
                                """))
                        {

                            String f = "%" + filter[0].toLowerCase() + "%";

                            ps.setString(1, f);
                            ps.setString(2, f);
                            ps.setString(3, f);

                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) return rs.getInt(1);

                        } catch (SQLException e)
                        {
                            throw new RuntimeException(e);
                        }
                        return (0);
                    }
            );


            grid = new Grid<>(StudentsPayment.class, false);
            grid.addColumn(StudentsPayment::getID).setHeader("id").setAutoWidth(true).setFlexGrow(1);
            grid.addColumn(StudentsPayment::getName).setHeader("Nom").setAutoWidth(true).setFlexGrow(1);
            grid.addColumn(StudentsPayment::getFirstName).setHeader("Prénom").setAutoWidth(true).setFlexGrow(1);
            /*grid.addColumn(StudentsPayment::getPayementStatus)
                    .setHeader("Statut paiement");*/


            for (int i = 1; i <= currentMonth; i++)
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

                    cb.addValueChangeListener( e ->
                    {
                        modifications
                                .computeIfAbsent(sp.getID(), k -> new HashMap<>())
                                .put(month, e.getValue());
                    });
                    return (cb);
                }).setHeader(moisNom + " " + startYear).setAutoWidth(true).setFlexGrow(1);

            }

            yearSelect.addValueChangeListener(event ->
            {
                int selectedYears = event.getValue();

                Notification.show("Années sélectionnées : " + selectedYears);

                grid.removeAllColumns();

                grid.addColumn(StudentsPayment::getID).setHeader("id").setAutoWidth(true).setFlexGrow(1);
                grid.addColumn(StudentsPayment::getName).setHeader("Nom").setAutoWidth(true).setFlexGrow(1);
                grid.addColumn(StudentsPayment::getFirstName).setHeader("Prénom").setAutoWidth(true).setFlexGrow(1);


                for (int month = 1; month <= currentMonth; month++)
                {
                    String moisNom = Month.of(month).getDisplayName(TextStyle.FULL, Locale.FRENCH);

                    int finalMonth = month;
                    grid.addComponentColumn(sp -> {
                        boolean paid = allPayments.getOrDefault(
                                sp.getID(), Collections.emptySet())
                                    .contains(finalMonth);
                        Checkbox cb = new Checkbox(paid);

                        if (paid) cb.setEnabled(false);

                        cb.addValueChangeListener(e -> {
                            modifications
                                    .computeIfAbsent(sp.getID(), k -> new HashMap<>())
                                    .put(finalMonth, e.getValue());
                        });

                        return cb;
                    }).setHeader(moisNom + " " + selectedYears);
                }

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


            grid.setDataProvider(provider);

            grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            grid.getStyle().set("overflow-x", "auto");


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        add(searchField);
        add(grid);

        HorizontalLayout hor = new HorizontalLayout(validate(modifications, paymentService), yearSelect);
        hor.setAlignItems(Alignment.CENTER);
        hor.setJustifyContentMode(JustifyContentMode.CENTER);
        hor.setWidthFull();

        add(hor);
    }

    private Button validate(Map<Integer, Map<Integer, Boolean>> modifications, PaymentService paymentService)
    {
        Button validerBtn = new Button("Valider");
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
            String studentInfo = student.getName() + " " + student.getFirstName() + "ID: " + studentId;

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
                for (var entry : modifications.entrySet()) {
                    int studentId = entry.getKey();
                    for (var moisEntry : entry.getValue().entrySet()) {
                        int mois = moisEntry.getKey();
                        // On utilise l'année sélectionnée dans le Select pour la cohérence
                        paymentService.updatePaymentDB(studentId, yearSelect.getValue(), mois, LocalDate.now());
                    }
                }

                modifications.clear();
                dialog.close();

                loadPaymentFromDB();
                grid.getDataProvider().refreshAll();

                Notification.show("Mises à jour enregistrées avec succès");
            } catch (SQLException ex) {
                Notification.show("Erreur lors de la validation : " + ex.getMessage());
            }
        });

        confirmer.addClassName("validate-btn");

        Button annuler = new Button("Annuler", ev -> dialog.close());

        dialog.add(dialogLayout);
        dialog.getFooter().add(annuler, confirmer);
        dialog.open();
    }

    private StudentsPayment findStudentById(int id)
    {
        return studentsPaymentList.stream()
                .filter(s -> s.getID() == id)
                .findFirst()
                .orElse(null);
    }

    private void loadPaymentFromDB()
    {
        allPayments = paymentService.getAllPaymentsForYear(yearSelect.getValue());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if (!UserSession.isLoggedIn())
            beforeEnterEvent.rerouteTo(LoginView.class);
    }
}
