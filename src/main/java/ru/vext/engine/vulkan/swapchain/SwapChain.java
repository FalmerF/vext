package ru.vext.engine.vulkan.swapchain;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ru.vext.engine.component.Scene;
import ru.vext.engine.util.VextUtil;
import ru.vext.engine.util.MemoryUtil;
import ru.vext.engine.vulkan.QueueFamilyIndices;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.swapchain.pipeline.graphics.GraphicsPipeline;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
@Getter
public class SwapChain {

    private final VkApplication vkApplication;
    private final VkDevice device;

    private final Map<String, GraphicsPipeline> graphicsPipelines;

    private long id;
    private List<Long> images;
    private List<Long> imageViews;
    private int imageFormat;
    private VkExtent2D extent;
    private List<Long> frameBuffers;
    private List<VkCommandBuffer> commandBuffers;

    private long renderPass;

    @Builder
    public SwapChain(VkApplication vkApplication, Map<String, GraphicsPipeline> graphicsPipelines) {
        this.vkApplication = vkApplication;
        this.device = vkApplication.getDevice();
        this.graphicsPipelines = graphicsPipelines;

        for (GraphicsPipeline pipeline : graphicsPipelines.values()) {
            pipeline.setSwapChain(this);
        }
    }

    public void create() {
        createSwapChain();
        createImageViews();
        createRenderPass();
        createGraphicsPipelines();
        createFrameBuffers();
        createCommandBuffers();

        Optional.ofNullable(vkApplication.getScene()).ifPresent(Scene::markDirty);
    }

    public void cleanup() {
        frameBuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));

        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(device, vkApplication.getCommandPool(), MemoryUtil.asPointerBuffer(stack, commandBuffers));
        }

        for (GraphicsPipeline pipeline : graphicsPipelines.values()) {
            pipeline.cleanup();
        }
        vkDestroyRenderPass(device, renderPass, null);

        imageViews.forEach(imageView -> vkDestroyImageView(device, imageView, null));

        vkDestroySwapchainKHR(device, id, null);
    }

    private void createSwapChain() {
        try (MemoryStack stack = stackPush()) {
            SwapChainSupportDetails swapChainSupport = vkApplication.querySwapChainSupport(vkApplication.getPhysicalDevice(), stack);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.getFormats());
            int presentMode = chooseSwapPresentMode(swapChainSupport.getPresentModes());
            VkExtent2D extent = chooseSwapExtent(stack, swapChainSupport.getCapabilities());

            VkSurfaceCapabilitiesKHR capabilities = swapChainSupport.getCapabilities();
            IntBuffer imageCount = stack.ints(capabilities.minImageCount() + 1);

            if (capabilities.maxImageCount() > 0 && imageCount.get(0) > capabilities.maxImageCount()) {
                imageCount.put(0, capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(vkApplication.getSurface());

            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            QueueFamilyIndices indices = vkApplication.findQueueFamilies(vkApplication.getPhysicalDevice());

            if (!indices.getGraphicsFamily().equals(indices.getPresentFamily())) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.getGraphicsFamily(), indices.getPresentFamily()));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.getCapabilities().currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if (vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            id = pSwapChain.get(0);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(device, id, imageCount, pSwapchainImages);

            images = new ArrayList<>(imageCount.get(0));

            for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                images.add(pSwapchainImages.get(i));
            }

            imageFormat = surfaceFormat.format();
            this.extent = VkExtent2D.create().set(extent);
        }
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream().filter(availableFormat -> availableFormat.format() == VK_FORMAT_R8G8B8A8_UNORM).filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR).findAny().orElse(availableFormats.get(0));
    }

    private int chooseSwapPresentMode(IntBuffer availablePresentModes) {

        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if (availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }

        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private VkExtent2D chooseSwapExtent(MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != 0xFFFFFFFF) {
            return capabilities.currentExtent();
        }

        VkExtent2D actualExtent = VkExtent2D.malloc(stack).set(vkApplication.getWidth(), vkApplication.getHeight());

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(VextUtil.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(VextUtil.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    private void createImageViews() {
        imageViews = new ArrayList<>(images.size());

        try (MemoryStack stack = stackPush()) {

            LongBuffer pImageView = stack.mallocLong(1);

            for (long swapChainImage : images) {

                VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack);

                createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                createInfo.image(swapChainImage);
                createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                createInfo.format(imageFormat);

                createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

                createInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                createInfo.subresourceRange().baseMipLevel(0);
                createInfo.subresourceRange().levelCount(1);
                createInfo.subresourceRange().baseArrayLayer(0);
                createInfo.subresourceRange().layerCount(1);

                if (vkCreateImageView(device, createInfo, null, pImageView) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create image views");
                }

                imageViews.add(pImageView.get(0));
            }

        }
    }

    private void createRenderPass() {
        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachmentDescription = VkAttachmentDescription.calloc(1, stack);
            attachmentDescription.format(imageFormat);
            attachmentDescription.samples(VK_SAMPLE_COUNT_1_BIT);
            attachmentDescription.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            attachmentDescription.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            attachmentDescription.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            attachmentDescription.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            attachmentDescription.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            attachmentDescription.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer attachmentReference = VkAttachmentReference.calloc(1, stack);
            attachmentReference.attachment(0);
            attachmentReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpassDescription = VkSubpassDescription.calloc(1, stack);
            subpassDescription.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpassDescription.colorAttachmentCount(1);
            subpassDescription.pColorAttachments(attachmentReference);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachmentDescription);
            renderPassInfo.pSubpasses(subpassDescription);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if (vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS)
                throw new RuntimeException("Failed to create render pass");

            renderPass = pRenderPass.get(0);
        }
    }

    private void createGraphicsPipelines() {
        for (GraphicsPipeline pipeline : graphicsPipelines.values()) {
            pipeline.create();
        }
    }

    private void createFrameBuffers() {
        frameBuffers = new ArrayList<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (long attachments : imageViews) {
                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
                framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(renderPass);
                framebufferInfo.attachmentCount(1);
                framebufferInfo.pAttachments(stack.longs(attachments));
                framebufferInfo.width(extent.width());
                framebufferInfo.height(extent.height());
                framebufferInfo.layers(1);

                LongBuffer pFrameBuffer = stack.mallocLong(1);
                if (vkCreateFramebuffer(device, framebufferInfo, null, pFrameBuffer) != VK_SUCCESS)
                    throw new RuntimeException("Failed to create framebuffer");

                frameBuffers.add(pFrameBuffer.get(0));
            }
        }
    }

    private void createCommandBuffers() {
        final int commandBuffersCount = frameBuffers.size();
        commandBuffers = new ArrayList<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo commandBufferAllocate = VkCommandBufferAllocateInfo.calloc(stack);
            commandBufferAllocate.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            commandBufferAllocate.commandPool(vkApplication.getCommandPool());
            commandBufferAllocate.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            commandBufferAllocate.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

            if (vkAllocateCommandBuffers(device, commandBufferAllocate, pCommandBuffers) != VK_SUCCESS)
                throw new RuntimeException("Failed to allocate command buffers");

            for (int i = 0; i < commandBuffersCount; i++)
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
        }
    }

    public GraphicsPipeline getGraphicsPipeline(String key) {
        return graphicsPipelines.get(key);
    }
}
