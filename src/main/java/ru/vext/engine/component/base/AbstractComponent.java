package ru.vext.engine.component.base;

import lombok.Getter;
import lombok.Setter;
import ru.vext.engine.util.Anchor;
import ru.vext.engine.vulkan.render.Drawer;

import java.awt.*;

@Setter
@Getter
public abstract class AbstractComponent extends AbstractParent implements IComponent {

    private IParent parent;

    private String width = "0", height = "0";

    private String offsetX = "0", offsetY = "0";

    private String marginLeft = "0", marginTop = "0", marginRight = "0", marginBottom = "0";

    private String paddingLeft = "0", paddingTop = "0", paddingRight = "0", paddingBottom = "0";

    private float scaleX = 1, scaleY = 1;

    private Color color = new Color(1f, 1f, 1f, 1f);

    private float rotationX, rotationY, rotationZ;

    private Anchor anchor = Anchor.LEFT_TOP;

    @Override
    public String getExternalWidth() {
        return String.format("(%s+%s+%s)", width, marginLeft, marginRight);
    }

    @Override
    public String getExternalHeight() {
        return String.format("(%s+%s+%s)", height, marginTop, marginBottom);
    }

    @Override
    public String getInternalWidth() {
        return String.format("(%s-%s-%s)", width, paddingLeft, paddingRight);
    }

    @Override
    public String getInternalHeight() {
        return String.format("(%s-%s-%s)", height, paddingTop, paddingBottom);
    }

    @Override
    public void drawPipeline(Drawer drawer) {
        preDraw(drawer);
        draw(drawer);
        postDraw(drawer);
    }

    public void preDraw(Drawer drawer) {
        drawer.pushMatrix();

        drawer.translate(
                String.format("%s*%s", parent.getInternalWidth(), anchor.x()),
                String.format("%s*%s", parent.getInternalHeight(), anchor.y())
        );

        drawer.translate(
                String.format("%s*%s", getExternalWidth(), -anchor.x()),
                String.format("%s*%s", getExternalHeight(), -anchor.y())
        );

        drawer.translate(offsetX, offsetY);
        drawer.translate(marginLeft, marginTop);
        drawer.rotate(rotationX, rotationY, rotationZ);
        drawer.scale(scaleX, scaleY);
    }

    public void postDraw(Drawer drawer) {
        drawer.translate(paddingLeft, paddingTop);

        for (IComponent child : getChildren()) {
            child.drawPipeline(drawer);
        }
        drawer.popMatrix();
    }

    protected abstract void draw(Drawer drawer);
}
