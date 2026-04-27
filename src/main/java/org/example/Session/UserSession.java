package org.example.Session;
import com.vaadin.flow.server.VaadinSession;

/**
 *   Is used to check what can this user login do
 *   We can't let anyone gave Internet access to a user
 *   We must also have one user for one level
* */
public class UserSession
{

    private static final String ROLE = "role";

    public static void setRole(String role)
    {
        VaadinSession.getCurrent().setAttribute(ROLE, role);
    }

    public static String getRole()
    {
        Object value = VaadinSession.getCurrent().getAttribute(ROLE);
        return (value instanceof String) ? (String) value : null;
    }

    public static boolean isLoggedIn()
    {
        return getRole() != null;
    }

    public static boolean isAdmin()
    {
        return "admin".equals(getRole());
    }
}