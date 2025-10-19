package ru.vext.engine.component.base;

import java.util.Arrays;
import java.util.Collection;

public interface IParent {

    void addChildren(Collection<? extends IComponent> children);

    default void addChildren(IComponent... child) {
        addChildren(Arrays.asList(child));
    }

    void setChildren(Collection<? extends IComponent> children);

    default void setChildren(IComponent... child) {
        setChildren(Arrays.asList(child));
    }

    Collection<IComponent> getChildren();

    float getExternalWidth();

    float getExternalHeight();

    float getInternalWidth();

    float getInternalHeight();

    float calculateWidth();

    float calculateHeight();
}
