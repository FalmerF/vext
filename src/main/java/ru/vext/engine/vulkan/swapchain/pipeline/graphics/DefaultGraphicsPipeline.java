package ru.vext.engine.vulkan.swapchain.pipeline.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout.DescriptorSetLayout;

import static org.lwjgl.vulkan.VK10.*;

public class DefaultGraphicsPipeline extends GraphicsPipeline {

    public DefaultGraphicsPipeline(VkApplication vkApplication, int topology, String... shaderPaths) {
        super(vkApplication, topology, shaderPaths);
    }

    @Override
    protected VkVertexInputBindingDescription.Buffer createInputBindings(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer inputBindings = VkVertexInputBindingDescription.calloc(1, stack);

        configureInputBinding(inputBindings.get(0), 0, Float.BYTES * 2, VK_VERTEX_INPUT_RATE_VERTEX);

        return inputBindings;
    }

    @Override
    protected VkVertexInputAttributeDescription.Buffer createInputAttribute(MemoryStack stack) {
        VkVertexInputAttributeDescription.Buffer inputAttributeDescriptions = VkVertexInputAttributeDescription.calloc(1, stack);

        configureInputAttribute(inputAttributeDescriptions.get(0), 0, 0, VK_FORMAT_R32G32_SFLOAT, 0);

        return inputAttributeDescriptions;
    }

    @Override
    protected DescriptorSetLayout createSetLayout() {
        return null;
    }
}
