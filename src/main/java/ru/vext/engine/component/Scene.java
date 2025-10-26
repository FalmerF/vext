package ru.vext.engine.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.vext.engine.VextApplication;
import ru.vext.engine.component.base.AbstractParent;
import ru.vext.engine.component.base.IComponent;
import ru.vext.engine.component.base.IDrawable;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.render.Drawer;

import java.awt.*;

@Slf4j
@Getter
public class Scene extends AbstractParent implements IDrawable {

    private final VkApplication vkApplication;
    private final VextApplication vextApplication;

    @Setter
    private Color backgroundColor = Color.WHITE;

    @Getter
    private boolean isDirty = true;

    public Scene(VextApplication vextApplication) {
        this.vextApplication = vextApplication;
        this.vkApplication = vextApplication.getVkApplication();
    }

    @Override
    public void drawPipeline(Drawer drawer) {
        for (IComponent c : getChildren()) {
            c.drawPipeline(drawer);
        }
    }

    @Override
    public void markDirty() {
        isDirty = true;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    @Override
    public float getExternalWidth() {
        return vkApplication.getWidth();
    }

    @Override
    public float getExternalHeight() {
        return vkApplication.getHeight();
    }

    @Override
    public float getInternalWidth() {
        return vkApplication.getWidth();
    }

    @Override
    public float getInternalHeight() {
        return vkApplication.getHeight();
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
        return vkApplication.getWidth();
    }

    @Override
    public float calculateHeight() {
        return vkApplication.getHeight();
    }
}
