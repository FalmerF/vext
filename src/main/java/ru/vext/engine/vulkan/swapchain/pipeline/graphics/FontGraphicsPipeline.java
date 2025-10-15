package ru.vext.engine.vulkan.swapchain.pipeline.graphics;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.resource.font.BakedFont;
import ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout.DescriptorSetLayout;

import static org.lwjgl.vulkan.VK10.*;

@Getter
public class FontGraphicsPipeline extends GraphicsPipeline {

    private final BakedFont bakedFont;

    public FontGraphicsPipeline(VkApplication vkApplication, BakedFont font, String... shaderPaths) {
        super(vkApplication, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP, shaderPaths);
        this.bakedFont = font;
    }

    @Override
    protected VkVertexInputBindingDescription.Buffer createInputBindings(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer inputBindings = VkVertexInputBindingDescription.calloc(2, stack);

        configureInputBinding(inputBindings.get(0), 0, Float.BYTES, VK_VERTEX_INPUT_RATE_INSTANCE);
        configureInputBinding(inputBindings.get(1), 1, Float.BYTES, VK_VERTEX_INPUT_RATE_INSTANCE);

        return inputBindings;
    }

    @Override
    protected VkVertexInputAttributeDescription.Buffer createInputAttribute(MemoryStack stack) {
        VkVertexInputAttributeDescription.Buffer inputAttributeDescriptions = VkVertexInputAttributeDescription.calloc(2, stack);

        configureInputAttribute(inputAttributeDescriptions.get(0), 0, 0, VK_FORMAT_R32_SINT, 0);
        configureInputAttribute(inputAttributeDescriptions.get(1), 1, 1, VK_FORMAT_R32_SFLOAT, 0);

        return inputAttributeDescriptions;
    }

    @Override
    protected DescriptorSetLayout createSetLayout() {
        return DescriptorSetLayout.builder()
                .addImageBinding(bakedFont.getImageView(), VK_SHADER_STAGE_FRAGMENT_BIT)
                .addGpuBufferBinding(bakedFont.getDataBuffer(), VK_SHADER_STAGE_FRAGMENT_BIT | VK_SHADER_STAGE_VERTEX_BIT)
                .build();
    }
}
