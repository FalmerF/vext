package ru.vext.engine.resource.font;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import ru.vext.engine.resource.IResource;
import ru.vext.engine.resource.IResourceLoader;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.buffer.BufferType;
import ru.vext.engine.vulkan.buffer.ImageView;
import ru.vext.engine.vulkan.buffer.MemoryBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphBox;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8_UNORM;

@RequiredArgsConstructor
public class FontLoader implements IResourceLoader {

    public static final int FONT_SIZE = 64;
    public static final int MAP_SIZE = 2048;

    private final VkApplication vkApplication;

    @Override
    public boolean isMatchResource(String name) {
        return name.endsWith(".ttf");
    }

    @SneakyThrows
    @Override
    public IResource load(String key, InputStream inputStream) {
        BakedFont.BakedFontBuilder fontBuilder = BakedFont.builder()
                .key(key);

        ByteBuffer buffer = loadFont(inputStream);

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!stbtt_InitFont(fontInfo, buffer)) {
            throw new RuntimeException("Font initialization failed");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            putFontMetrics(fontBuilder, fontInfo, stack);
            bakeFont(fontBuilder, fontInfo, buffer, stack);
        }

        return fontBuilder.build();
    }

    private ByteBuffer loadFont(InputStream fontStream) throws IOException {
        byte[] bytes = IOUtils.toByteArray(fontStream);
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        return buffer.put(bytes).flip();
    }

    private void putFontMetrics(BakedFont.BakedFontBuilder fontBuilder, STBTTFontinfo fontInfo, MemoryStack stack) {
        IntBuffer ascent = stack.mallocInt(1);
        IntBuffer descent = stack.mallocInt(1);
        IntBuffer lineGap = stack.mallocInt(1);

        stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);

        fontBuilder.ascent(ascent.get(0))
                .descent(descent.get(0))
                .lineGap(lineGap.get(0));
    }

    private void bakeFont(BakedFont.BakedFontBuilder fontBuilder, STBTTFontinfo fontInfo, ByteBuffer fontData, MemoryStack stack) {
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
        Map<Character, Glyph> glyphs = new HashMap<>();

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

            float offsetY = y0.get(0) * fontScale;

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

        ImageView imageView = new ImageView(vkApplication, bitmap, width, height, VK_FORMAT_R8_UNORM);

        MemoryUtil.memFree(bitmap);

        MemoryBuffer dataBuffer = new MemoryBuffer(vkApplication, data, BufferType.USAGE_INDEX_TRANSFER_DSC | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, BufferType.MEMORY_TYPE_GPU_LOCAL);

        fontBuilder.glyphs(glyphs)
                .imageView(imageView)
                .dataBuffer(dataBuffer);
    }
}
