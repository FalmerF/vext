package ru.vext.engine.vulkan.render;

import lombok.extern.slf4j.Slf4j;
import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import ru.vext.engine.util.Unit;
import ru.vext.engine.util.VextUtil;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.buffer.BufferType;
import ru.vext.engine.vulkan.buffer.MemoryBuffer;
import ru.vext.engine.vulkan.render.font.FontDrawer;
import ru.vext.engine.vulkan.render.font.Glyph;
import ru.vext.engine.vulkan.swapchain.SwapChain;
import ru.vext.engine.vulkan.swapchain.pipeline.graphics.GraphicsPipeline;

import java.awt.*;
import java.lang.Math;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class Drawer {

    private final VkApplication vkApplication;
    private final VkDevice device;
    private final SwapChain swapChain;

    private final Matrix4f matrix;
    private final List<float[]> matrixStorage = new ArrayList<>();

    private Frame frame;
    private int frameIndex;
    private VkCommandBuffer commandBuffer;

    public Drawer(VkApplication vkApplication, SwapChain swapChain) {
        this.vkApplication = vkApplication;
        this.device = vkApplication.getDevice();
        this.swapChain = swapChain;

        matrix = new Matrix4f();
    }

    public void identity(Frame frame, int frameIndex, VkCommandBuffer commandBuffer) {
        this.frame = frame;
        this.frameIndex = frameIndex;
        this.commandBuffer = commandBuffer;

        matrix.identity();
        matrix.translate(-1, -1, 1);

        VkExtent2D extent = swapChain.getExtent();
        matrix.scale(2f / extent.width(), 2f / extent.height(), 1);

        matrixStorage.clear();
    }

    public void translate(String x, String y) {
        VkExtent2D extent = swapChain.getExtent();
        translate(Unit.getScreenValue(x, extent.width()), Unit.getScreenValue(y, extent.height()));
    }

    public void translate(float x, float y) {
        matrix.translate(x, y, 0);
    }

    public void scale(float x, float y) {
        matrix.scale(x, y, 1);
    }

    public void rotate(float x, float y, float z) {
        matrix.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }

    public void pushMatrix() {
        float[] matrix = this.matrix.get(new float[16]);
        matrixStorage.add(0, matrix);
    }

    public void popMatrix() {
        if (matrixStorage.isEmpty()) {
            throw new IllegalStateException("No matrix stored yet");
        }

        float[] matrix = matrixStorage.remove(0);
        this.matrix.set(matrix);
    }

    public void drawQuad(String width, String height, Color color) {
        VkExtent2D extent = swapChain.getExtent();
        drawQuad(Unit.getScreenValue(width, extent.width()), Unit.getScreenValue(height, extent.height()), color);
    }

    public void drawQuad(float width, float height, Color color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Vector3f point1 = matrix.transformPosition(new Vector3f(0, 0, 0));
            Vector3f point2 = matrix.transformPosition(new Vector3f(width, 0, 0));
            Vector3f point3 = matrix.transformPosition(new Vector3f(0, height, 0));
            Vector3f point4 = matrix.transformPosition(new Vector3f(width, height, 0));

            Float[] positions = new Float[]{
                    point1.x, point1.y,
                    point2.x, point2.y,
                    point3.x, point3.y,
                    point4.x, point4.y,
            };

            MemoryBuffer positionBuffer = frame.getBufferPool().createTemp(positions, BufferType.USAGE_VERTEX, BufferType.MEMORY_TYPE_CPU_VISIBLE);

            GraphicsPipeline pipeline = swapChain.getGraphicsPipelines()[0];
            pipeline.bind(commandBuffer, frameIndex);

            FloatBuffer pushConstantData = stack.mallocFloat(16);
            pushConstantData.put(color.getRGBComponents(null));
            pushConstantData.flip();

            vkCmdPushConstants(commandBuffer, pipeline.getPipelineLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantData);

            vkCmdBindVertexBuffers(commandBuffer, 0, new long[]{positionBuffer.getId()}, new long[]{0});
            vkCmdDraw(commandBuffer, 4, 1, 0, 0);
        }

    }

    public void drawText(CharSequence text, Color color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            float scale = 24f / FontDrawer.FONT_SIZE;
            scale(scale, scale);

            GraphicsPipeline pipeline = swapChain.getGraphicsPipelines()[1];
            pipeline.bind(commandBuffer, frameIndex);

            FloatBuffer pushConstantData = stack.mallocFloat(21);
            pushConstantData.put(matrix.get(new float[16]));
            pushConstantData.put(color.getRGBColorComponents(null));
            pushConstantData.put(FontDrawer.FONT_SIZE);
            pushConstantData.put(FontDrawer.MAP_SIZE);
            pushConstantData.flip();

            vkCmdPushConstants(commandBuffer, pipeline.getPipelineLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantData);

            FontDrawer fontDrawer = vkApplication.getDefaultFontDrawer();
            Glyph[] glyphs = fontDrawer.getGlyphs(text);

            Integer[] glyphData = new Integer[glyphs.length];
            Float[] offsetData = new Float[glyphs.length];

            float offsetX = 0;
            for (int i = 0; i < glyphs.length; i++) {
                Glyph glyph = glyphs[i];

                offsetX += glyph.getOffsetX();
                glyphData[i] = (int) glyph.getCharacter();
                offsetData[i] = (float) Math.floor(offsetX);
                offsetX += glyph.getAdvanceWidth();
            }

            MemoryBuffer glyphBuffer = frame.getBufferPool().createTemp(glyphData, BufferType.USAGE_VERTEX_TRANSFER_SRC, BufferType.MEMORY_TYPE_CPU_VISIBLE);
            MemoryBuffer offsetBuffer = frame.getBufferPool().createTemp(offsetData, BufferType.USAGE_VERTEX_TRANSFER_SRC, BufferType.MEMORY_TYPE_CPU_VISIBLE);

            vkCmdBindVertexBuffers(
                    commandBuffer, 0,
                    new long[]{glyphBuffer.getId(), offsetBuffer.getId()},
                    new long[]{0, 0}
            );
            vkCmdDraw(commandBuffer, 4, glyphs.length, 0, 0);
        }
    }
}
