package ru.vext.engine.component;

import ru.vext.engine.component.base.AbstractComponent;
import ru.vext.engine.vulkan.render.Drawer;

public class Text extends AbstractComponent {

    private String text;

    public Text() {
        text = "Hello, world!";
    }

    @Override
    protected void draw(Drawer drawer) {
        drawer.drawText(text, getColor());
    }

}
