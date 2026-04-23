package org.example;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;


/**
 * This class is used to configure the generated html host page used by the app
 */
@Theme(themeClass = com.vaadin.flow.theme.lumo.Lumo.class)
@PWA(name = "My Application", shortName = "My Application")
public class AppShell implements AppShellConfigurator {
    
}