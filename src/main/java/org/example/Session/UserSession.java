package org.example.Session;
import com.vaadin.flow.server.VaadinSession;

/**
 *   Is used to check what can this user login do
 *   We can't let anyone gave Internet access to a user
 *   We must also have one user for one level
* */
public class UserSession
{
    public static void setLoggedIn(boolean loggedIn) {
        VaadinSession.getCurrent().setAttribute("isLoggedIn", loggedIn);
    }

    public static boolean isLoggedIn() {
        Object value = VaadinSession.getCurrent().getAttribute("isLoggedIn");
        return value != null && (boolean) value;
    }
}