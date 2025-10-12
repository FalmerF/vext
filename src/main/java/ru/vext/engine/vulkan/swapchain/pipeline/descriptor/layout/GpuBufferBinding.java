package ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.buffer.MemoryBuffer;

import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;

@Getter
public class GpuBufferBinding extends AbstractDescriptorBinding {

    private final MemoryBuffer buffer;

    public GpuBufferBinding(MemoryBuffer buffer, int flags, int binding) {
        super(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, flags, binding);
        this.buffer = buffer;
    }

    @Override
    public void fillBindingInfo(VkApplication vkApplication, VkWriteDescriptorSet descriptorWrite, MemoryStack stack) {
        VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
        bufferInfo.buffer(buffer.getId());
        bufferInfo.offset(0);
        bufferInfo.range(buffer.getSize());
        descriptorWrite.pBufferInfo(bufferInfo);
    }
}
