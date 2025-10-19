package ru.vext.engine.test;

import ru.vext.engine.VextApplication;
import ru.vext.engine.component.Panel;
import ru.vext.engine.component.Scene;
import ru.vext.engine.component.Text;
import ru.vext.engine.util.Anchor;

import java.awt.*;

public class ButtonTest extends VextApplication {

    public ButtonTest() {
        super("Vext Button Test");
    }

    @Override
    public void initialize() {
        Scene scene = new Scene(this);

        Panel horizontalLine = new Panel();
        horizontalLine.setSize("100%", "5px");
        horizontalLine.setAnchor(Anchor.CENTER);
        horizontalLine.setColor(Color.RED);

        Panel button = new Panel();
        button.setSize("200px", "56px");
        button.setColor(Color.BLUE);
        button.setAnchor(Anchor.CENTER);

        Text text = new Text("Button");
        text.setAnchor(Anchor.CENTER);
        text.setColor(Color.WHITE);

        button.addChildren(text);

        scene.addChildren(horizontalLine, button);

        setScene(scene);
    }
}
