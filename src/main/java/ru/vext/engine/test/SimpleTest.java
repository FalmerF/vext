package ru.vext.engine.test;

import ru.vext.engine.VextApplication;
import ru.vext.engine.component.Panel;
import ru.vext.engine.component.Scene;
import ru.vext.engine.component.Text;
import ru.vext.engine.util.Anchor;

import java.awt.*;

public class SimpleTest extends VextApplication {

    public static void main(String[] args) {
        new SimpleTest();
    }

    public SimpleTest() {
        super("Vext Test Application");
    }

    @Override
    public void initialize() {
        Scene scene = new Scene(this);

//        Panel panel1 = new Panel();
//        panel1.setSize("70%");
//        panel1.setOffset("15%");
//        panel1.setColor(new Color(0.6f, 0.6f, 0.6f, 1));
//
//        Panel panel2 = new Panel();
//        panel2.setSize("150px");
//        panel2.setColor(new Color(0f, 0f, 1f, 0.5f));
//        panel2.setMargin("25px", "25px+25px");
//        panel2.setAnchor(Anchor.CENTER);
//
//        Panel panel3 = new Panel();
//        panel3.setSize("25%");
//        panel3.setColor(new Color(1f, 0f, 0f));
//        panel3.setAnchor(Anchor.RIGHT_BOTTOM);
//        panel3.setMargin("25px");
//
//        panel1.addChildren(panel2);
//        scene.addChildren(panel1);

        Panel horizontalLine = new Panel();
        horizontalLine.setSize("100%", "5px");
        horizontalLine.setAnchor(Anchor.CENTER);
        horizontalLine.setColor(Color.RED);

        Text text1 = new Text("Это тестовое сообщение, которое демонстрирует возможности рендера !!??.../,,,");
        text1.setAnchor(Anchor.LEFT);
        text1.setOffset("50px", "0");
        text1.setFontSize(32);

        Text text2 = new Text("Это тестовое сообщение, которое демонстрирует возможности рендера !!??.../,,,");
        text2.setAnchor(Anchor.LEFT);
        text2.setOffset("50px", "64px");
        text2.setFontSize(24);
        text2.setFont("font/arial.ttf");

        Text text3 = new Text("Это тестовое сообщение, которое демонстрирует возможности рендера !!??.../,,,");
        text3.setColor(Color.DARK_GRAY);
        text3.setAnchor(Anchor.LEFT);
        text3.setOffset("50px", "128px");
        text3.setFontSize(24);
        text3.setFont("font/Lobster-Regular.ttf");

        Text text4 = new Text("Это тестовое сообщение, которое демонстрирует возможности рендера !!??.../,,,");
        text4.setAnchor(Anchor.LEFT);
        text4.setOffset("50px", "192px");
        text4.setFontSize(24);
        text4.setFont("font/Kablammo-Regular-VariableFont_MORF.ttf");

        Text text5 = new Text("Это тестовое сообщение, которое демонстрирует возможности рендера !!??.../,,,");
        text5.setColor(Color.DARK_GRAY);
        text5.setAnchor(Anchor.LEFT);
        text5.setOffset("50px", "256px");
        text5.setFontSize(24);
        text5.setFont("font/RubikDirt-Regular.ttf");

        scene.addChildren(horizontalLine, text1, text2, text3, text4, text5);

        setScene(scene);
    }
}
