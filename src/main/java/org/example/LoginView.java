package org.example;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.example.Session.UserSession;

@Route("")
@PageTitle("Connexion")
@CssImport("./styles/loginView.css")
public class LoginView extends VerticalLayout
{

    public LoginView()
    {
        addClassName("login-view");

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setHeightFull();

        LoginForm loginForm = new LoginForm();
        loginForm.addClassName("red-theme-form");


        loginForm.addLoginListener(e ->
        {
            //we can change this to use a real database but for now, we will stick to it
            if ("admin".equals(e.getUsername()) && "password123".equals(e.getPassword()))
            {
                UserSession.setRole("admin");
                getUI().ifPresent(ui -> ui.navigate("checking"));
            } else if ("guest".equals(e.getUsername() ) && "guest".equals(e.getPassword()))
            {
                UserSession.setRole("guest");
                getUI().ifPresent(ui -> ui.navigate("checking"));
            } else
                loginForm.setError(true);
        });

        H1 title = new H1("Gestion Paiements Cotisation");
        title.addClassName("login-title");

        add(title, loginForm);
    }
}