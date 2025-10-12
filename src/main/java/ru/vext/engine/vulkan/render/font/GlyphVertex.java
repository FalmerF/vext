package ru.vext.engine.vulkan.render.font;

import org.lwjgl.stb.STBTruetype;

public record GlyphVertex(float x, float y, float cx, float cy, byte type) {

    public GlyphVertex controlPoint() {
        return new GlyphVertex(cx, cy, 0, 0, STBTruetype.STBTT_vline);
    }

}
