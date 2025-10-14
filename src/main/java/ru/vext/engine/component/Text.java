package ru.vext.engine.component;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import ru.vext.engine.component.base.AbstractComponent;
import ru.vext.engine.vulkan.render.Drawer;

import javax.naming.OperationNotSupportedException;

@Getter
@Setter
public class Text extends AbstractComponent {

    private String text;
    private float fontSize;

    public Text(String text) {
        setText(text);
        setFontSize(24);
    }

    @Override
    protected void draw(Drawer drawer) {
        if (text != null && !text.isEmpty()) {
            drawer.drawText(text, fontSize, getColor());
        }
    }

    public void setText(String text) {
        this.text = text;
        super.setWidth("300px"); // TODO: Calculate normal width
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        super.setHeight(fontSize + "px");
    }

    @SneakyThrows
    @Override
    public void setWidth(String width) {
        throw new OperationNotSupportedException("It is not possible to manually change the width of the text");
    }

    @SneakyThrows
    @Override
    public void setHeight(String height) {
        throw new OperationNotSupportedException("It is not possible to manually change the height of the text");
    }

}
