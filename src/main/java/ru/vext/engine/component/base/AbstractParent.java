package ru.vext.engine.component.base;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractParent implements IParent {

    private final List<IComponent> children = new CopyOnWriteArrayList<>();

    public void addChildren(Collection<? extends IComponent> children) {
        this.children.addAll(children);
        for (IComponent child : children) {
            child.setParent(this);
        }
    }

    public void setChildren(Collection<? extends IComponent> children) {
        this.children.clear();
        this.children.addAll(children);
        for (IComponent child : children) {
            child.setParent(this);
        }
    }

    public Collection<IComponent> getChildren() {
        return children;
    }
}
