package ru.vext.engine.vulkan.buffer;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ru.vext.engine.util.VextUtil;
import ru.vext.engine.util.VulkanUtil;
import ru.vext.engine.vulkan.VkApplication;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class ImageView {

    private final VkApplication vkApplication;
    @Getter
    private final int width, height;

    @Getter
    private final long image, imageMemory, imageView;

    public ImageView(VkApplication vkApplication, ByteBuffer buffer, int width, int height, int format) {
        this.vkApplication = vkApplication;
        this.width = width;
        this.height = height;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.format(format);
            imageInfo.extent(VkExtent3D.calloc(stack).width(width).height(height).depth(1));
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
            imageInfo.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

            LongBuffer pImage = stack.mallocLong(1);
            if (vkCreateImage(vkApplication.getDevice(), imageInfo, null, pImage) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }
            image = pImage.get(0);
            imageMemory = allocateImageMemory(stack);
            transitionImageLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            writeData(buffer, stack);
            imageView = createImageView(format);
            transitionImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
    }

    private long allocateImageMemory(MemoryStack stack) {
        VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(vkApplication.getDevice(), image, memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(VulkanUtil.findMemoryType(stack, vkApplication.getPhysicalDevice(), memRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

        LongBuffer pImageMemory = stack.mallocLong(1);
        if (vkAllocateMemory(vkApplication.getDevice(), allocInfo, null, pImageMemory) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate memory");
        }
        long imageMemory = pImageMemory.get(0);

        vkBindImageMemory(vkApplication.getDevice(), image, imageMemory, 0);

        return imageMemory;
    }

    private void writeData(ByteBuffer buffer, MemoryStack stack) {
        Byte[] data = new Byte[buffer.capacity()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = buffer.get(i);
        }

        MemoryBuffer stagingBuffer = new MemoryBuffer(vkApplication, data, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, BufferType.MEMORY_TYPE_CPU_VISIBLE);

        VkCommandBuffer commandBuffer = vkApplication.beginSingleTimeCommands();

        VkBufferImageCopy.Buffer copyRegion = VkBufferImageCopy.calloc(1, stack);
        copyRegion.bufferOffset(0);
        copyRegion.bufferRowLength(0);
        copyRegion.bufferImageHeight(0);
        copyRegion.imageSubresource(VkImageSubresourceLayers.calloc(stack)
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1));
        copyRegion.imageOffset(VkOffset3D.calloc(stack).x(0).y(0).z(0));
        copyRegion.imageExtent(VkExtent3D.calloc(stack).width(width).height(height).depth(1));

        vkCmdCopyBufferToImage(commandBuffer, stagingBuffer.getId(), image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion);

        vkApplication.endSingleTimeCommands(commandBuffer);

        stagingBuffer.cleanup();
    }

    private long createImageView(int format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange(VkImageSubresourceRange.calloc(stack)
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1));

            LongBuffer pImageView = stack.mallocLong(1);
            if (vkCreateImageView(vkApplication.getDevice(), viewInfo, null, pImageView) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image view");
            }
            return pImageView.get(0);
        }
    }

    private void transitionImageLayout(int oldLayout, int newLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBuffer commandBuffer = vkApplication.beginSingleTimeCommands();

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(VkImageSubresourceRange.calloc(stack)
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            int sourceStage, destinationStage;
            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else {
                throw new RuntimeException("Неподдерживаемый переход layout");
            }

            vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, null, null, barrier);

            vkApplication.endSingleTimeCommands(commandBuffer);
        }
    }

    public void cleanup() {
        vkDestroyImage(vkApplication.getDevice(), image, null);
        vkFreeMemory(vkApplication.getDevice(), imageMemory, null);
    }
}
