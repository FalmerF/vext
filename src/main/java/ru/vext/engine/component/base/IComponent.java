package ru.vext.engine.component.base;

import ru.vext.engine.component.Scene;
import ru.vext.engine.util.Anchor;

import java.awt.*;

public interface IComponent extends IDrawable {

    void setParent(IParent parent);

    IParent getParent();

    void setScene(Scene scene);

    Scene getScene();

    void setWidth(String width);

    String getWidth();

    void setHeight(String height);

    String getHeight();

    default void setSize(String size) {
        setWidth(size);
        setHeight(size);
    }

    default void setSize(String width, String height) {
        setWidth(width);
        setHeight(height);
    }

    void setOffsetX(String offsetX);

    String getOffsetX();

    void setOffsetY(String offsetY);

    String getOffsetY();

    default void setOffset(String offset) {
        setOffsetX(offset);
        setOffsetY(offset);
    }

    default void setOffset(String offsetX, String offsetY) {
        setOffsetX(offsetX);
        setOffsetY(offsetY);
    }

    void setScaleX(float scaleX);

    float getScaleX();

    void setScaleY(float scaleY);

    float getScaleY();

    default void setScale(float scale) {
        setScaleX(scale);
        setScaleY(scale);
    }

    default void setScale(float scaleX, float scaleY) {
        setScaleX(scaleX);
        setScaleY(scaleY);
    }

    void setColor(Color color);

    Color getColor();

    void setRotationX(float rotationX);

    float getRotationX();

    void setRotationY(float rotationY);

    float getRotationY();

    void setRotationZ(float rotationZ);

    float getRotationZ();

    default void setRotation(float x, float y, float z) {
        setRotationX(x);
        setRotationY(y);
        setRotationZ(z);
    }

    void setMarginLeft(String marginLeft);

    String getMarginLeft();

    void setMarginTop(String marginTop);

    String getMarginTop();

    void setMarginRight(String marginRight);

    String getMarginRight();

    void setMarginBottom(String marginBottom);

    String getMarginBottom();

    default void setMargin(String left, String top, String right, String bottom) {
        setMarginLeft(left);
        setMarginTop(top);
        setMarginRight(right);
        setMarginBottom(bottom);
    }

    default void setMargin(String horizontal, String vertical) {
        setMargin(horizontal, vertical, horizontal, vertical);
    }

    default void setMargin(String margin) {
        setMargin(margin, margin, margin, margin);
    }

    void setPaddingLeft(String paddingLeft);

    String getPaddingLeft();

    void setPaddingTop(String paddingTop);

    String getPaddingTop();

    void setPaddingRight(String paddingRight);

    String getPaddingRight();

    void setPaddingBottom(String paddingBottom);

    String getPaddingBottom();

    default void setPadding(String left, String top, String right, String bottom) {
        setPaddingLeft(left);
        setPaddingTop(top);
        setPaddingRight(right);
        setPaddingBottom(bottom);
    }

    default void setPadding(String horizontal, String vertical) {
        setPadding(horizontal, vertical, horizontal, vertical);
    }

    default void setPadding(String padding) {
        setPadding(padding, padding, padding, padding);
    }

    void setAnchor(Anchor anchor);

    Anchor getAnchor();
}
