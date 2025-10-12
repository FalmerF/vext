package ru.vext.engine.vulkan.buffer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ru.vext.engine.util.BufferUtil;
import ru.vext.engine.util.VulkanUtil;
import ru.vext.engine.vulkan.VkApplication;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

@Slf4j
@Getter
public class MemoryBuffer {

    private final VkApplication vkApplication;
    private final VkDevice device;

    private final int usage;
    private final int memoryType;

    private final long id;
    private final long bufferMemory;

    private long size;

    public MemoryBuffer(VkApplication vkApplication, Object[] data, int usage, int memoryType) {
        this.vkApplication = vkApplication;
        this.device = vkApplication.getDevice();
        this.usage = usage;
        this.memoryType = memoryType;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(BufferUtil.getDataSize(data));
            bufferInfo.usage(usage);
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
            memoryAllocate.memoryTypeIndex(VulkanUtil.findMemoryType(stack, vkApplication.getPhysicalDevice(), memRequirements.memoryTypeBits(), memoryType));

            LongBuffer pVertexBufferMemory = stack.mallocLong(1);

            if (vkAllocateMemory(device, memoryAllocate, null, pVertexBufferMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate memory");
            }

            bufferMemory = pVertexBufferMemory.get(0);

            vkBindBufferMemory(device, id, bufferMemory, 0);

            writeData(data, bufferInfo, stack);
        }
    }

    private void writeData(Object[] data, VkBufferCreateInfo bufferInfo, MemoryStack stack) {
        size = BufferUtil.getDataSize(data);

        if ((memoryType & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
            writeDataWithMapMemory(data, bufferInfo, stack);
        } else {
            writeDataWithStagingBuffer(data, stack);
        }
    }

    private void writeDataWithMapMemory(Object[] data, VkBufferCreateInfo bufferInfo, MemoryStack stack) {
        PointerBuffer pointerBuffer = stack.mallocPointer(1);
        vkMapMemory(device, bufferMemory, 0, bufferInfo.size(), 0, pointerBuffer);
        BufferUtil.fillBuffer(data, pointerBuffer);
        vkUnmapMemory(device, bufferMemory);
    }

    private void writeDataWithStagingBuffer(Object[] data, MemoryStack stack) {
        MemoryBuffer stagingBuffer = new MemoryBuffer(vkApplication, data, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, BufferType.MEMORY_TYPE_CPU_VISIBLE);

        VkCommandBuffer commandBuffer = vkApplication.beginSingleTimeCommands();

        VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack);
        copyRegion.srcOffset(0);
        copyRegion.dstOffset(0);
        copyRegion.size(BufferUtil.getDataSize(data));

        vkCmdCopyBuffer(commandBuffer, stagingBuffer.getId(), id, copyRegion);

        vkApplication.endSingleTimeCommands(commandBuffer);

        stagingBuffer.cleanup();
    }

    public void cleanup() {
        vkDestroyBuffer(device, id, null);
        vkFreeMemory(device, bufferMemory, null);
    }
}
