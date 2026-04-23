package org.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.example.Checking.Checking;

public class MainView extends VerticalLayout {

    public MainView()
    {
        add(new Checking());
    }
}
