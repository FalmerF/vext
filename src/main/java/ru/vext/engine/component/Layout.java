package ru.vext.engine.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.vext.engine.component.base.IComponent;
import ru.vext.engine.util.Unit;
import ru.vext.engine.vulkan.render.Drawer;

import java.util.function.Function;

@Getter
public class Layout extends Panel {

    private Orientation orientation = Orientation.HORIZONTAL;
    private float spacing = 10;

    public Layout(Orientation orientation) {
        this();
        this.orientation = orientation;
    }

    public Layout() {
        super.setWidth("auto");
        super.setHeight("auto");
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        markDirty();
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
        markDirty();
    }

    @Override
    public float calculateWidth() {
        String width = getWidth();

        if ("auto".equals(width)) {
            if (orientation == Orientation.HORIZONTAL) {
                return getPrimaryLength(Orientation.HORIZONTAL);
            } else {
                return getSecondaryLength(Orientation.HORIZONTAL);
            }
        }

        return super.calculateWidth();
    }

    @Override
    public float calculateHeight() {
        String height = getHeight();

        if ("auto".equals(height)) {
            if (orientation == Orientation.VERTICAL) {
                return getPrimaryLength(Orientation.VERTICAL);
            } else {
                return getSecondaryLength(Orientation.VERTICAL);
            }
        }

        return super.calculateHeight();
    }

    private float getPrimaryLength(Orientation orientation) {
        float totalLength = 0;
        int count = 0;

        for (IComponent child : getChildren()) {
            totalLength += orientation.getLength(child);
            if (count >= 1) {
                totalLength += spacing;
            }
            count++;
        }

        return totalLength;
    }

    private float getSecondaryLength(Orientation orientation) {
        return (float) getChildren().stream()
                .mapToDouble(orientation::getLength)
                .max().orElse(0);
    }

    @Override
    public float getInternalWidth() {
        String widthExpression = super.getWidth();

        if ("auto".equals(widthExpression)) {
            float widthFloat = getParentWidth();
            String expression = String.format("(%s-%s-%s)", widthFloat, getPaddingLeft(), getPaddingRight());
            return Unit.getScreenValue(expression, widthFloat);
        }

        return super.getInternalWidth();
    }

    @Override
    public float getInternalHeight() {
        String heightExpression = super.getHeight();

        if ("auto".equals(heightExpression)) {
            float heightFloat = getParentHeight();
            String expression = String.format("(%s-%s-%s)", heightFloat, getPaddingLeft(), getPaddingRight());
            return Unit.getScreenValue(expression, heightFloat);
        }

        return super.getInternalHeight();
    }

    @Override
    public void postDraw(Drawer drawer) {
        drawer.translate(getPaddingLeft(), getPaddingTop());

        if (orientation == Orientation.HORIZONTAL) {
            for (IComponent child : getChildren()) {
                child.drawPipeline(drawer);
                float width = child.calculateWidth();
                drawer.translate(width + spacing, 0);
            }
        } else {
            for (IComponent child : getChildren()) {
                child.drawPipeline(drawer);
                float height = child.calculateHeight();
                drawer.translate(0, height + spacing);
            }
        }
        drawer.popMatrix();
    }

    @RequiredArgsConstructor
    public enum Orientation {
        HORIZONTAL(IComponent::calculateWidth),
        VERTICAL(IComponent::calculateHeight);

        private final Function<IComponent, Float> getLengthFunction;

        public float getLength(IComponent component) {
            return getLengthFunction.apply(component);
        }
    }
}
