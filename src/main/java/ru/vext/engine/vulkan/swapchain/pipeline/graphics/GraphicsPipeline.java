package ru.vext.engine.vulkan.swapchain.pipeline.graphics;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.shader.ShaderInfo;
import ru.vext.engine.vulkan.shader.ShaderLoader;
import ru.vext.engine.vulkan.swapchain.SwapChain;
import ru.vext.engine.vulkan.swapchain.pipeline.descriptor.DescriptorPool;
import ru.vext.engine.vulkan.swapchain.pipeline.descriptor.layout.DescriptorSetLayout;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
@Getter
public abstract class GraphicsPipeline {

    protected final VkApplication vkApplication;
    protected final VkDevice device;

    protected final int topology;
    protected final String[] shaderPaths;

    @Setter
    protected SwapChain swapChain;

    protected long pipelineLayout;
    protected long id;

    @Getter
    protected DescriptorPool descriptorPool;

    public GraphicsPipeline(VkApplication vkApplication, int topology, String... shaderPaths) {
        this.vkApplication = vkApplication;
        this.device = vkApplication.getDevice();
        this.topology = topology;
        this.shaderPaths = shaderPaths;
    }

    protected abstract VkVertexInputBindingDescription.Buffer createInputBindings(MemoryStack stack);

    protected abstract VkVertexInputAttributeDescription.Buffer createInputAttribute(MemoryStack stack);

    protected abstract DescriptorSetLayout createSetLayout();

    public void create() {
        DescriptorSetLayout descriptorSetLayout = createSetLayout();
        if (descriptorSetLayout != null) {
            descriptorPool = new DescriptorPool(vkApplication, descriptorSetLayout, VkApplication.MAX_FRAMES_IN_FLIGHT);
        }

        try (MemoryStack stack = stackPush()) {
            ShaderInfo shaderInfo = ShaderLoader.loadShaders(device, stack, shaderPaths);

            VkVertexInputBindingDescription.Buffer inputBindings = createInputBindings(stack);

            VkVertexInputAttributeDescription.Buffer inputAttributeDescriptions = createInputAttribute(stack);

            VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInput.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInput.pVertexBindingDescriptions(inputBindings);
            vertexInput.pVertexAttributeDescriptions(inputAttributeDescriptions);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(topology);
            inputAssembly.primitiveRestartEnable(false);

            VkViewport viewport = VkViewport.malloc(stack);
            viewport.x(0);
            viewport.y(0);
            viewport.width(swapChain.getExtent().width());
            viewport.height(swapChain.getExtent().height());
            viewport.minDepth(0);
            viewport.maxDepth(1);

            VkRect2D scissor = VkRect2D.malloc(stack);
            scissor.offset(VkOffset2D.malloc(stack).set(0, 0));
            scissor.extent(swapChain.getExtent());

            IntBuffer dynamicStates = stack.ints(new int[]{VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR});

            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            dynamicState.pDynamicStates(dynamicStates);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            VkPipelineRasterizationStateCreateInfo rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizationState.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizationState.depthClampEnable(false);
            rasterizationState.rasterizerDiscardEnable(false);

            rasterizationState.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizationState.lineWidth(1);

            rasterizationState.cullMode(VK_CULL_MODE_BACK_BIT);
            rasterizationState.frontFace(VK_FRONT_FACE_CLOCKWISE);

            rasterizationState.depthBiasEnable(false);

            VkPipelineMultisampleStateCreateInfo multisampleState = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampleState.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampleState.sampleShadingEnable(false);
            multisampleState.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachmentState = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachmentState.blendEnable(true);
            colorBlendAttachmentState.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
            colorBlendAttachmentState.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
            colorBlendAttachmentState.colorBlendOp(VK_BLEND_OP_ADD);
            colorBlendAttachmentState.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
            colorBlendAttachmentState.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            colorBlendAttachmentState.alphaBlendOp(VK_BLEND_OP_ADD);
            colorBlendAttachmentState.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);

            VkPipelineColorBlendStateCreateInfo colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlendState.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlendState.logicOpEnable(false);
            colorBlendState.attachmentCount(1);
            colorBlendState.pAttachments(colorBlendAttachmentState);

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(true)
                    .depthWriteEnable(false)
                    .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL);

            LongBuffer pPipelineLayout = stack.mallocLong(1);

            VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(0)
                    .size(128);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
            if (descriptorPool != null) {
                pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorPool.getDescriptorSetLayout()));
            }

            if (vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderInfo.getShaderStages());

            pipelineInfo.pVertexInputState(vertexInput);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizationState);
            pipelineInfo.pMultisampleState(multisampleState);
            pipelineInfo.pColorBlendState(colorBlendState);
            pipelineInfo.pDynamicState(dynamicState);
            pipelineInfo.pDepthStencilState(depthStencil);

            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(swapChain.getRenderPass());
            pipelineInfo.subpass(0);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            id = pGraphicsPipeline.get(0);

            shaderInfo.destroy();
        }
    }

    protected void configureInputAttribute(VkVertexInputAttributeDescription inputAttribute, int binding, int location, int format, int offset) {
        inputAttribute.binding(binding);
        inputAttribute.location(location);
        inputAttribute.format(format);
        inputAttribute.offset(offset);
    }

    protected void configureInputBinding(VkVertexInputBindingDescription inputBinding, int binding, int stride, int inputRate) {
        inputBinding.binding(binding);
        inputBinding.stride(stride);
        inputBinding.inputRate(inputRate);
    }

    public void bind(VkCommandBuffer commandBuffer, int frameIndex) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, id);
        if (descriptorPool != null) {
            long[] descriptorSets = new long[]{descriptorPool.getDescriptorSets()[frameIndex]};

            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, descriptorSets, null);
        }
    }

    public void cleanup() {
        if (descriptorPool != null) {
            descriptorPool.cleanup();
        }
        vkDestroyPipeline(device, id, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
    }
}
