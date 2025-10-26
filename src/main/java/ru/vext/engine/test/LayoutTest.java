package ru.vext.engine.test;

import ru.vext.engine.VextApplication;
import ru.vext.engine.component.Layout;
import ru.vext.engine.component.Panel;
import ru.vext.engine.component.Scene;
import ru.vext.engine.util.Anchor;

import java.awt.*;

public class LayoutTest extends VextApplication {

    public LayoutTest() {
        super("Vext Layout Test");
    }

    @Override
    public void initialize() {
        Scene scene = new Scene(this);
        scene.setBackgroundColor(new Color(35, 35, 35));

        Layout mainLayout = new Layout(Layout.Orientation.VERTICAL);
        mainLayout.setAnchor(Anchor.TOP);
        mainLayout.setColor(new Color(60, 60, 60));
        mainLayout.setPadding("20px");
        mainLayout.setMarginTop("100px");
        mainLayout.setSpacing(25);

        Layout layout1 = new Layout(Layout.Orientation.HORIZONTAL);
        layout1.setColor(Color.BLACK);

        for(int i = 0; i < 5; i++) {
            Panel panel = new Panel();
            panel.setSize("50px", "100px");
            panel.setColor(Color.RED);
            panel.setAnchor(Anchor.CENTER);
            layout1.addChildren(panel);
        }

        Panel panel1 = new Panel();
        panel1.setSize("50px", "200px");
        panel1.setColor(Color.RED);
        panel1.setAnchor(Anchor.CENTER);
        layout1.addChildren(panel1);

        for(int i = 0; i < 5; i++) {
            Panel panel = new Panel();
            panel.setSize("50px", "100px");
            panel.setColor(Color.RED);
            panel.setAnchor(Anchor.BOTTOM);
            layout1.addChildren(panel);
        }

        Panel panel2 = new Panel();
        panel2.setSize("50%", "100px");
        panel2.setColor(Color.LIGHT_GRAY);

        Layout layout2 = new Layout(Layout.Orientation.HORIZONTAL);
        layout2.setColor(Color.BLACK);
        layout2.setPadding("50px", "0");
        layout2.setAnchor(Anchor.CENTER);

        Panel panel3 = new Panel();
        panel3.setSize("400px", "100px");
        panel3.setColor(Color.LIGHT_GRAY);
        layout2.addChildren(panel3);

        mainLayout.addChildren(layout1, panel2, layout2);
        scene.addChildren(mainLayout);

        setScene(scene);
    }

}
