package ru.vext.engine.component.base;

import lombok.Getter;
import lombok.Setter;
import ru.vext.engine.component.Scene;
import ru.vext.engine.util.Anchor;
import ru.vext.engine.vulkan.render.Drawer;

import java.awt.*;

@Setter
@Getter
public abstract class AbstractComponent extends AbstractParent implements IComponent {

    private IParent parent;

    private Scene scene;

    private boolean isDirty = true;

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
    public void markDirty() {
        if (!isDirty) {
            isDirty = true;

            for (IComponent children : getChildren()) {
                children.markDirty();
            }
        }
    }

    @Override
    public void setParent(IParent parent) {
        if (this.parent == parent) {
            return;
        }

        this.parent = parent;
        markDirty();

        if (parent instanceof IComponent p) {
            setScene(p.getScene());
        } else if(parent instanceof Scene s) {
            setScene(s);
        }
    }

    @Override
    public void setScene(Scene scene) {
        if (scene == null || this.scene == scene) {
            return;
        }

        markDirty();

        this.scene = scene;
        for (IComponent children : getChildren()) {
            children.setScene(scene);
        }
    }

    @Override
    public void drawPipeline(Drawer drawer) {
        preDraw(drawer);
        draw(drawer);
        postDraw(drawer);
    }

    @Override
    public void setWidth(String width) {
        this.width = width;
        markDirty();
    }

    @Override
    public void setHeight(String height) {
        this.height = height;
        markDirty();
    }

    @Override
    public void setOffsetX(String offsetX) {
        this.offsetX = offsetX;
        markDirty();
    }

    @Override
    public void setOffsetY(String offsetY) {
        this.offsetY = offsetY;
        markDirty();
    }

    @Override
    public void setMarginLeft(String marginLeft) {
        this.marginLeft = marginLeft;
        markDirty();
    }

    @Override
    public void setMarginTop(String marginTop) {
        this.marginTop = marginTop;
        markDirty();
    }

    @Override
    public void setMarginRight(String marginRight) {
        this.marginRight = marginRight;
        markDirty();
    }

    @Override
    public void setMarginBottom(String marginBottom) {
        this.marginBottom = marginBottom;
        markDirty();
    }

    @Override
    public void setPaddingLeft(String paddingLeft) {
        this.paddingLeft = paddingLeft;
        markDirty();
    }

    @Override
    public void setPaddingTop(String paddingTop) {
        this.paddingTop = paddingTop;
        markDirty();
    }

    @Override
    public void setPaddingRight(String paddingRight) {
        this.paddingRight = paddingRight;
        markDirty();
    }

    @Override
    public void setPaddingBottom(String paddingBottom) {
        this.paddingBottom = paddingBottom;
        markDirty();
    }

    @Override
    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
        markDirty();
    }

    @Override
    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
        markDirty();
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
        markDirty();
    }

    @Override
    public void setRotationX(float rotationX) {
        this.rotationX = rotationX;
        markDirty();
    }

    @Override
    public void setRotationY(float rotationY) {
        this.rotationY = rotationY;
        markDirty();
    }

    @Override
    public void setRotationZ(float rotationZ) {
        this.rotationZ = rotationZ;
        markDirty();
    }

    @Override
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
        markDirty();
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
