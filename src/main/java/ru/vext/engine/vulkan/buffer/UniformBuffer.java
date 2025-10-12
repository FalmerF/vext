package ru.vext.engine.vulkan.buffer;

import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import ru.vext.engine.util.BufferUtil;
import ru.vext.engine.util.VulkanUtil;
import ru.vext.engine.vulkan.VkApplication;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

public class UniformBuffer {

    private final VkDevice device;
    @Getter
    private final int size;

    @Getter
    private final long id, bufferMemory;
    @Getter
    private final PointerBuffer mappedBuffer;

    public UniformBuffer(VkApplication vkApplication, int size) {
        this.device = vkApplication.getDevice();
        this.size = size;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pVertexBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pVertexBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create vertex buffer");
            }

            id = pVertexBuffer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, id, memRequirements);

            VkMemoryAllocateInfo memoryAllocate = VkMemoryAllocateInfo.calloc(stack);
            memoryAllocate.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            memoryAllocate.allocationSize(memRequirements.size());
            memoryAllocate.memoryTypeIndex(VulkanUtil.findMemoryType(stack, vkApplication.getPhysicalDevice(), memRequirements.memoryTypeBits(), BufferType.MEMORY_TYPE_CPU_VISIBLE));

            LongBuffer pVertexBufferMemory = stack.mallocLong(1);

            if (vkAllocateMemory(device, memoryAllocate, null, pVertexBufferMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate memory");
            }

            bufferMemory = pVertexBufferMemory.get(0);

            vkBindBufferMemory(device, id, bufferMemory, 0);

            mappedBuffer = MemoryUtil.memAllocPointer(size);
            vkMapMemory(device, bufferMemory, 0, bufferInfo.size(), 0, mappedBuffer);
        }
    }

    public void writeData(Object[] data) {
        BufferUtil.fillBuffer(data, mappedBuffer);
    }

    public void cleanup() {
        vkDestroyBuffer(device, id, null);
        vkFreeMemory(device, bufferMemory, null);
    }
}
