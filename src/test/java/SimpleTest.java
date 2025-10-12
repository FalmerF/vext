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
        Scene scene = new Scene();

        Panel panel1 = new Panel();
        panel1.setSize("70%");
        panel1.setOffset("15%");
        panel1.setColor(new Color(0.6f, 0.6f, 0.6f, 1));

        Panel panel2 = new Panel();
        panel2.setSize("150px");
        panel2.setColor(new Color(0f, 0f, 1f, 0.5f));
        panel2.setMargin("25px", "25px+25px");
        panel2.setAnchor(Anchor.CENTER);

        Panel panel3 = new Panel();
        panel3.setSize("25%");
        panel3.setColor(new Color(1f, 0f, 0f));
        panel3.setAnchor(Anchor.RIGHT_BOTTOM);
        panel3.setMargin("25px");

        panel1.addChildren(panel2);
        scene.addChildren(panel1);

        Text text1 = new Text();
        text1.setColor(new Color(0f, 0f, 0f));
        text1.setAnchor(Anchor.CENTER);
        text1.setScale(8);
        scene.addChildren(text1);

        setScene(scene);
    }
}
