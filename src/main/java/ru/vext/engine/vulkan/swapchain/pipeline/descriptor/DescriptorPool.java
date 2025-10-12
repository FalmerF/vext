package ru.vext.engine.vulkan.swapchain.pipeline.descriptor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout.AbstractDescriptorBinding;
import ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout.DescriptorSetLayout;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

@Slf4j
@Getter
public class DescriptorPool {

    private final VkApplication vkApplication;

    private final long descriptorPool;
    private final long descriptorSetLayout;
    private final long[] descriptorSets;

    public DescriptorPool(VkApplication vkApplication, DescriptorSetLayout descriptorSetLayout, int frames) {
        this.vkApplication = vkApplication;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            descriptorPool = createDescriptorPool(descriptorSetLayout, frames, stack);
            this.descriptorSetLayout = createDescriptorSetLayout(descriptorSetLayout, frames, stack);
            descriptorSets = allocateDescriptorSets(frames, stack);
            setupDescriptorSets(descriptorSetLayout, stack);
        }
    }

    private long createDescriptorPool(DescriptorSetLayout descriptorSetLayout, int frames, MemoryStack stack) {
        Map<Integer, Integer> descriptorsCounts = descriptorSetLayout.getDescriptorsCounts();

        VkDescriptorPoolSize.Buffer poolSizeBuffer = VkDescriptorPoolSize.calloc(descriptorsCounts.size(), stack);

        int i = 0;
        for (Map.Entry<Integer, Integer> entry : descriptorsCounts.entrySet()) {
            VkDescriptorPoolSize poolSize = poolSizeBuffer.get(i++);
            poolSize.type(entry.getKey());
            poolSize.descriptorCount(entry.getValue() * frames);
        }

        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
        poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
        poolInfo.pPoolSizes(poolSizeBuffer);
        poolInfo.maxSets(frames);

        LongBuffer pDescriptorPool = stack.mallocLong(1);

        if (vkCreateDescriptorPool(vkApplication.getDevice(), poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor pool!");
        }
        return pDescriptorPool.get(0);
    }

    private long createDescriptorSetLayout(DescriptorSetLayout descriptorSetLayout, int frames, MemoryStack stack) {
        AbstractDescriptorBinding[] bindings = descriptorSetLayout.getBindings();
        VkDescriptorSetLayoutBinding.Buffer bindingsBuffer = VkDescriptorSetLayoutBinding.calloc(bindings.length, stack);

        for (int i = 0; i < bindings.length; i++) {
            VkDescriptorSetLayoutBinding bindingLayout = bindingsBuffer.get(i);
            AbstractDescriptorBinding binding = bindings[i];
            bindingLayout.binding(binding.getBinding());
            bindingLayout.descriptorType(binding.getType());
            bindingLayout.descriptorCount(1);
            bindingLayout.stageFlags(binding.getFlags());
        }

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
        layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
        layoutInfo.pBindings(bindingsBuffer);

        LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

        if (vkCreateDescriptorSetLayout(vkApplication.getDevice(), layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor set layout!");
        }

        return pDescriptorSetLayout.get(0);
    }

    private long[] allocateDescriptorSets(int frames, MemoryStack stack) {
        long[] descriptorSetsLayouts = new long[frames];
        Arrays.fill(descriptorSetsLayouts, descriptorSetLayout);
        LongBuffer descriptorLayoutsBuffer = stack.longs(descriptorSetsLayouts);

        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
        allocInfo.descriptorPool(descriptorPool);
        allocInfo.pSetLayouts(descriptorLayoutsBuffer);

        LongBuffer descriptorsSets = stack.mallocLong(frames);

        if (vkAllocateDescriptorSets(vkApplication.getDevice(), allocInfo, descriptorsSets) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets!");
        }

        long[] descriptorLayouts = new long[frames];
        for (int i = 0; i < descriptorLayouts.length; ++i) {
            descriptorLayouts[i] = descriptorsSets.get(i);
        }

        return descriptorLayouts;
    }

    private void setupDescriptorSets(DescriptorSetLayout descriptorSetLayout, MemoryStack stack) {
        AbstractDescriptorBinding[] bindings = descriptorSetLayout.getBindings();

        for(long descriptorSet : descriptorSets) {
            VkWriteDescriptorSet.Buffer descriptorWriteBuffer = VkWriteDescriptorSet.calloc(bindings.length, stack);

            for (int i = 0; i < bindings.length; i++) {
                VkWriteDescriptorSet descriptorWrite = descriptorWriteBuffer.get(i);
                AbstractDescriptorBinding binding = bindings[i];

                descriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorWrite.dstSet(descriptorSet);
                descriptorWrite.dstBinding(binding.getBinding());
                descriptorWrite.dstArrayElement(0);
                descriptorWrite.descriptorCount(1);
                descriptorWrite.descriptorType(binding.getType());

                binding.fillBindingInfo(vkApplication, descriptorWrite, stack);
            }

            vkUpdateDescriptorSets(vkApplication.getDevice(), descriptorWriteBuffer, null);
        }
    }

    public void cleanup() {
        vkDestroyDescriptorPool(vkApplication.getDevice(), descriptorPool, null);
        vkDestroyDescriptorSetLayout(vkApplication.getDevice(), descriptorSetLayout, null);
    }
}
