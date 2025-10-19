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

        Layout layout = new Layout(Layout.Orientation.HORIZONTAL);
        layout.setAnchor(Anchor.CENTER);
        layout.setColor(Color.BLACK);
        layout.setHeight("100px");

        for(int i = 0; i < 5; i++) {
            Panel panel = new Panel();
            panel.setSize("50px", "50px");
            panel.setColor(Color.RED);
            panel.setAnchor(Anchor.CENTER);
            layout.addChildren(panel);
        }

        scene.addChildren(layout);

        setScene(scene);
    }

}
