package ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.buffer.ImageView;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

@Getter
public class ImageBinding extends AbstractDescriptorBinding {

    private final ImageView imageView;

    public ImageBinding(ImageView imageView, int flags, int binding) {
        super(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, flags, binding);
        this.imageView = imageView;
    }

    public long createTextureSampler(VkApplication vkApplication) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_LINEAR)
                    .minFilter(VK_FILTER_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .anisotropyEnable(false)
                    .maxAnisotropy(16)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .mipLodBias(0.0f)
                    .minLod(0.0f)
                    .maxLod(0.0f);

            LongBuffer pSampler = stack.mallocLong(1);

            if (vkCreateSampler(vkApplication.getDevice(), samplerInfo, null, pSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create sampler");
            }

            return pSampler.get(0);
        }
    }

    @Override
    public void fillBindingInfo(VkApplication vkApplication, VkWriteDescriptorSet descriptorWrite, MemoryStack stack) {
        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
        imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        imageInfo.imageView(imageView.getImageView());
        imageInfo.sampler(createTextureSampler(vkApplication));
        descriptorWrite.pImageInfo(imageInfo);
    }
}
