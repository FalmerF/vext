package ru.vext.engine.component;

import lombok.extern.slf4j.Slf4j;
import ru.vext.engine.component.base.AbstractComponent;
import ru.vext.engine.vulkan.render.Drawer;

@Slf4j
public class Panel extends AbstractComponent {

    @Override
    public void draw(Drawer drawer) {
        drawer.drawQuad(getWidth(), getHeight(), getColor());
    }
}
