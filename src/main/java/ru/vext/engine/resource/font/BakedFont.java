package ru.vext.engine.resource.font;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import ru.vext.engine.resource.IResource;
import ru.vext.engine.resource.ResourceType;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.buffer.BufferType;
import ru.vext.engine.vulkan.buffer.ImageView;
import ru.vext.engine.vulkan.buffer.MemoryBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8_UNORM;

@Slf4j
@Getter
@Builder
public class BakedFont implements IResource {

    @Getter
    private final String key;
    private final Map<Character, Glyph> glyphs;
    private final int ascent, descent, lineGap;
    private final ImageView imageView;
    private final MemoryBuffer dataBuffer;

    public Glyph[] getGlyphs(CharSequence text) {
        Glyph[] glyphsArray = new Glyph[text.length()];

        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyphs.get(text.charAt(i));
            if (glyph == null) {
                glyphsArray[i] = glyphs.get(' ');
            } else {
                glyphsArray[i] = glyph;
            }
        }

        return glyphsArray;
    }

    public float getTextWidth(CharSequence text, float fontSize) {
        Glyph[] glyphs = getGlyphs(text);

        float width = 0;

        for (Glyph glyph : glyphs) {
            width += glyph.getOffsetX();
            width += glyph.getAdvanceWidth();
        }

        return width * (fontSize / 24);
    }

    @Override
    public ResourceType getType() {
        return ResourceType.FONT;
    }

    @Override
    public void cleanup() {
        imageView.cleanup();
        dataBuffer.cleanup();
    }
}
