package org.example.Session;
import com.vaadin.flow.server.VaadinSession;

public class UserSession {
    public static void setLoggedIn(boolean loggedIn) {
        VaadinSession.getCurrent().setAttribute("isLoggedIn", loggedIn);
    }

    public static boolean isLoggedIn() {
        Object value = VaadinSession.getCurrent().getAttribute("isLoggedIn");
        return value != null && (boolean) value;
    }
}