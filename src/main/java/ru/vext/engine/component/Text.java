package ru.vext.engine.component;

import lombok.Setter;
import ru.vext.engine.component.base.AbstractComponent;
import ru.vext.engine.vulkan.render.Drawer;

@Setter
public class Text extends AbstractComponent {

    private String text;

    public Text(String text) {
        this.text = text;
    }

    @Override
    protected void draw(Drawer drawer) {
        if (text != null && !text.isEmpty()) {
            drawer.drawText(text, getColor());
        }
    }

}
