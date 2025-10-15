package ru.vext.engine.resource.font;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Glyph {

    public static final int SIZE = 7;

    private final char character;
    private final float width, height;
    private final float offsetX, offsetY;
    private final float advanceWidth;
    private final Float[] minMaxUV;

    public Glyph(char character, float width, float height, float advanceWidth, float offsetX, float offsetY, Float[] minMaxUV) {
        this.character = character;
        this.width = width;
        this.height = height;
        this.advanceWidth = advanceWidth;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.minMaxUV = minMaxUV;
    }

    public void write(Float[] data, int index) {
        index *= SIZE;
        data[index] = width;
        data[index + 1] = height;
        data[index + 2] = offsetY;
        data[index + 3] = minMaxUV[0];
        data[index + 4] = minMaxUV[1];
        data[index + 5] = minMaxUV[2];
        data[index + 6] = minMaxUV[3];
    }
}
