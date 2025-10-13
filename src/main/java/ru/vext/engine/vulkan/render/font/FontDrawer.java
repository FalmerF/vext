package ru.vext.engine.vulkan.render.font;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
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
public class FontDrawer {

    public static final int FONT_SIZE = 64;
    public static final int MAP_SIZE = 2048;

    private final Map<Character, Glyph> glyphs = new HashMap<>();

    private final VkApplication vkApplication;
    private final STBTTFontinfo fontInfo;

    private int ascent, descent, lineGap;

    private ImageView imageView;
    private MemoryBuffer dataBuffer;

    @SneakyThrows
    public FontDrawer(VkApplication vkApplication, InputStream fontStream) {
        this.vkApplication = vkApplication;

        ByteBuffer buffer = loadFont(fontStream);

        fontInfo = STBTTFontinfo.create();
        if (!stbtt_InitFont(fontInfo, buffer)) {
            throw new RuntimeException("Font initialization failed");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            getFontMetrics(stack);
            bakeFont(buffer, stack);
        }
    }

    private void bakeFont(ByteBuffer fontData, MemoryStack stack) {
        int width = MAP_SIZE;
        int height = MAP_SIZE;
        ByteBuffer bitmap = MemoryUtil.memAlloc(width * height);

        STBTTBakedChar.Buffer backedChars = new STBTTBakedChar.Buffer(stack.malloc(0x04FF * STBTTBakedChar.SIZEOF));

        STBTruetype.stbtt_BakeFontBitmap(
                fontData,
                FONT_SIZE,
                bitmap,
                width,
                height,
                0,
                backedChars
        );

        float fontScale = stbtt_ScaleForPixelHeight(fontInfo, FONT_SIZE);

        Float[] data = new Float[Glyph.SIZE * 0x04FF];

        for (int i = 0; i < 0x04FF; i++) {
            STBTTBakedChar bakedChar = backedChars.get(i);
            char c = (char) i;
            int glyphIndex = stbtt_FindGlyphIndex(fontInfo, c);

            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer leftSideBearing = stack.mallocInt(1);
            stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidth, leftSideBearing);

            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            stbtt_GetGlyphBox(fontInfo, glyphIndex, x0, y0, x1, y1);

            float baseline = ascent * fontScale;
            float offsetY = baseline + (y0.get(0) * fontScale);

            Float[] minMaxUV = new Float[]{
                    (float) bakedChar.x0() / width,
                    (float) bakedChar.y0() / height,
                    (float) bakedChar.x1() / width,
                    (float) bakedChar.y1() / height
            };

            Glyph glyph = new Glyph(c,
                    (x1.get(0) - x0.get(0)) * fontScale,
                    (y1.get(0) - y0.get(0)) * fontScale,
                    advanceWidth.get(0) * fontScale,
                    leftSideBearing.get(0) * fontScale,
                    offsetY,
                    minMaxUV
            );
            glyphs.put(c, glyph);

            glyph.write(data, i);
        }

        imageView = new ImageView(vkApplication, bitmap, width, height, VK_FORMAT_R8_UNORM);

        MemoryUtil.memFree(bitmap);

        dataBuffer = new MemoryBuffer(vkApplication, data, BufferType.USAGE_INDEX_TRANSFER_DSC | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, BufferType.MEMORY_TYPE_GPU_LOCAL);
    }

    private ByteBuffer loadFont(InputStream fontStream) throws IOException {
        byte[] bytes = IOUtils.toByteArray(fontStream);
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        return buffer.put(bytes).flip();
    }

    private void getFontMetrics(MemoryStack stack) {
        IntBuffer ascent = stack.mallocInt(1);
        IntBuffer descent = stack.mallocInt(1);
        IntBuffer lineGap = stack.mallocInt(1);

        stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);

        this.ascent = ascent.get(0);
        this.descent = descent.get(0);
        this.lineGap = lineGap.get(0);
    }

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

    public void cleanup() {
        imageView.cleanup();
    }
}
