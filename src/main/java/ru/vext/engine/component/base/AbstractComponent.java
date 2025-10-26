package ru.vext.engine.component.base;

import lombok.Getter;
import lombok.Setter;
import ru.vext.engine.component.Scene;
import ru.vext.engine.util.Anchor;
import ru.vext.engine.util.Unit;
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
    public float getExternalWidth() {
        String expression = String.format("(%s+%s+%s)", calculateWidth(), marginLeft, marginRight);
        return Unit.getScreenValue(expression, getParentWidth());
    }

    @Override
    public float getExternalHeight() {
        String expression = String.format("(%s+%s+%s)", calculateHeight(), marginTop, marginBottom);
        return Unit.getScreenValue(expression, getParentHeight());
    }

    @Override
    public float getInternalWidth() {
        String expression = String.format("(%s-%s-%s)", calculateWidth(), paddingLeft, paddingRight);
        return Unit.getScreenValue(expression, getParentWidth());
    }

    @Override
    public float getInternalHeight() {
        String expression = String.format("(%s-%s-%s)", calculateHeight(), paddingTop, paddingBottom);
        return Unit.getScreenValue(expression, getParentHeight());
    }

    @Override
    public float getMaxInternalWidth() {
        return getInternalWidth();
    }

    @Override
    public float getMaxInternalHeight() {
        return getInternalHeight();
    }

    @Override
    public float calculateWidth() {
        float parentWidth = getParentWidth();
        return Unit.getScreenValue(getWidth(), (int) parentWidth);
    }

    @Override
    public float calculateHeight() {
        float parentHeight = getParentHeight();
        return Unit.getScreenValue(getHeight(), (int) parentHeight);
    }

    protected float getParentWidth() {
        return parent == null ? 0 : parent.getMaxInternalWidth();
    }

    protected float getParentHeight() {
        return parent == null ? 0 : parent.getMaxInternalHeight();
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
                ((parent.getInternalWidth() * anchor.x()) + (getExternalWidth() * -anchor.x())) * parent.getAnchorWidthMultiplier(),
                ((parent.getInternalHeight() * anchor.y()) + (getExternalHeight() * -anchor.y())) * parent.getAnchorHeightMultiplier()
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
