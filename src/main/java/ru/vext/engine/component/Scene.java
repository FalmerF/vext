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

@Slf4j
@Getter
public class Scene extends AbstractParent implements IDrawable {

    private final VkApplication vkApplication;
    private final VextApplication vextApplication;

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
    public String getExternalWidth() {
        return vkApplication.getWidth() + "px";
    }

    @Override
    public String getExternalHeight() {
        return vkApplication.getHeight() + "px";
    }

    @Override
    public String getInternalWidth() {
        return vkApplication.getWidth() + "px";
    }

    @Override
    public String getInternalHeight() {
        return vkApplication.getHeight() + "px";
    }
}
