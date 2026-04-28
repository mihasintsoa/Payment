package org.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Connexion")
@CssImport("./styles/loginView.css")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    public LoginView()
    {
        addClassName("login-view");

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setHeightFull();

        H1 title = new H1("Gestion Paiements Cotisation");
        title.addClassName("login-title");

        LoginForm loginForm = new LoginForm();
        loginForm.setAction("login");
        loginForm.addClassName("red-theme-form");
        loginForm.addLoginListener(e -> {
            UI.getCurrent().navigate("checking");
        });

        add(title, loginForm);
    }
}
