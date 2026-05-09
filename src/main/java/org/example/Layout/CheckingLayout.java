package org.example.Layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class CheckingLayout extends AppLayout
{
    public CheckingLayout()
    {

        DrawerToggle toggle = new DrawerToggle();
        H1 h1 = new H1("Action");
        h1.getStyle().set("margin", "0");
        h1.getStyle().set("color", "#c8102e");

        Icon icon = VaadinIcon.COGS.create();
        icon.getStyle().set("color", "#c8102e");

        HorizontalLayout titleRow = new HorizontalLayout(icon, h1);
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);



        addToNavbar(toggle, titleRow);
        setDrawerOpened(false);

    }

    public void addToActionDrawer(Component... component)
    {
        addToDrawer(component);
    }
}
