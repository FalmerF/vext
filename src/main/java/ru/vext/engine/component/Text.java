package ru.vext.engine.component;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.vext.engine.VextApplication;
import ru.vext.engine.component.base.AbstractComponent;
import ru.vext.engine.resource.ResourceStorage;
import ru.vext.engine.resource.font.BakedFont;
import ru.vext.engine.vulkan.render.Drawer;

import javax.naming.OperationNotSupportedException;
import java.awt.*;

@Slf4j
@Getter
@Setter
public class Text extends AbstractComponent {

    private String text;
    private float fontSize;
    private String font;

    public Text(String text) {
        setColor(Color.BLACK);
        setText(text);
        setFontSize(24);
    }

    @Override
    protected void draw(Drawer drawer) {
        if (text != null && !text.isEmpty()) {
            drawer.drawText(text, font, fontSize, getColor());
        }
    }

    public void setText(String text) {
        this.text = text;
        markDirty();
        updateSize();
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        markDirty();
        updateSize();
    }

    public void setFont(String font) {
        this.font = font;
        markDirty();
        updateSize();
    }

    @Override
    public void setScene(Scene scene) {
        super.setScene(scene);
        updateSize();
    }

    public void updateSize() {
        super.setHeight(fontSize + "px");

        Scene scene = getScene();

        if (scene == null) {
            return;
        }

        VextApplication vextApplication = scene.getVextApplication();
        ResourceStorage resourceStorage = vextApplication.getResourceStorage();
        BakedFont bakedFont = resourceStorage.getFont(font);
        float width = bakedFont.getTextWidth(text, fontSize);
        super.setWidth(Math.floor(width) + "px");

        log.info("Font {}, width: {}, height: {}", font, getWidth(), getHeight());
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
