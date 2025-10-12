package ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import ru.vext.engine.vulkan.VkApplication;

@Data
@AllArgsConstructor
public abstract class AbstractDescriptorBinding {

    private int type;
    private int flags;
    private int binding;

    public abstract void fillBindingInfo(VkApplication vkApplication, VkWriteDescriptorSet descriptorWrite, MemoryStack stack);
}
