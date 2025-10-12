package ru.vext.engine.vulkan.render;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ru.vext.engine.component.Scene;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.swapchain.SwapChain;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class RenderPipeline {

    private final VkApplication vkApplication;
    private final VkDevice device;
    private final SwapChain swapChain;
    private final int maxFramesInFlight;

    private List<Frame> inFlightFrames;
    private Map<Integer, Frame> imagesInFlight;
    private int currentFrame;

    private Drawer drawer;

    public RenderPipeline(VkApplication vkApplication, SwapChain swapChain, int maxFramesInFlight) {
        this.vkApplication = vkApplication;
        this.device = vkApplication.getDevice();
        this.swapChain = swapChain;
        this.maxFramesInFlight = maxFramesInFlight;
    }

    public void create() {
        createSyncObjects();
        drawer = new Drawer(vkApplication, swapChain);
    }

    public void cleanup() {
        for (Frame frame : inFlightFrames) {
            vkDestroySemaphore(device, frame.getRenderFinishedSemaphore(), null);
            vkDestroySemaphore(device, frame.getImageAvailableSemaphore(), null);
            vkDestroyFence(device, frame.getFence(), null);
            frame.getBufferPool().cleanup();
        }
        imagesInFlight.clear();
    }

    private void createSyncObjects() {
        inFlightFrames = new ArrayList<>(maxFramesInFlight);
        imagesInFlight = new HashMap<>(swapChain.getImages().size());

        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < maxFramesInFlight; i++) {

                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                inFlightFrames.add(new Frame(vkApplication, pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0)));
            }
        }
    }

    public void drawFrame() {
        Scene scene = vkApplication.getScene();

        if (scene == null || !scene.isDirty()) return;

        try (MemoryStack stack = stackPush()) {
            Frame thisFrame = inFlightFrames.get(currentFrame);

            vkWaitForFences(device, thisFrame.pFence(), true, 0xFFFFFFFFFFFFFFFFL);

            IntBuffer pImageIndex = stack.mallocInt(1);

            long result = vkAcquireNextImageKHR(device, swapChain.getId(), 0xFFFFFFFFFFFFFFFFL, thisFrame.getImageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);

            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                vkApplication.recreateSwapChain();
                return;
            } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("Failed to acquire swap chain image!");
            }

            final int imageIndex = pImageIndex.get(0);

            thisFrame.getBufferPool().cleanup();

            if (imagesInFlight.containsKey(imageIndex))
                vkWaitForFences(device, imagesInFlight.get(imageIndex).getFence(), true, 0xFFFFFFFFFFFFFFFFL);

            imagesInFlight.put(imageIndex, thisFrame);

            VkCommandBuffer commandBuffer = swapChain.getCommandBuffers().get(imageIndex);
            vkResetCommandBuffer(commandBuffer, 0);

            drawer.identity(thisFrame, currentFrame, commandBuffer);

            recordCommandBuffer(commandBuffer, thisFrame, imageIndex);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());

            submitInfo.pCommandBuffers(stack.pointers(swapChain.getCommandBuffers().get(imageIndex)));

            vkResetFences(device, thisFrame.pFence());

            if (vkQueueSubmit(vkApplication.getGraphicsQueue(), submitInfo, thisFrame.getFence()) != VK_SUCCESS)
                throw new RuntimeException("Failed to submit draw command buffer");

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore());

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapChain.getId()));

            presentInfo.pImageIndices(pImageIndex);

            result = vkQueuePresentKHR(vkApplication.getPresentQueue(), presentInfo);

            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                vkApplication.recreateSwapChain();
            } else if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image!");
            }

            currentFrame = (currentFrame + 1) % maxFramesInFlight;

            scene.setDirty(false);
        }
    }

    private void recordCommandBuffer(VkCommandBuffer commandBuffer, Frame frame, int imageIndex) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            renderPassInfo.renderPass(swapChain.getRenderPass());

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(swapChain.getExtent());
            renderPassInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color().float32(stack.floats(1.0f, 1.0f, 1.0f, 1.0f));
            clearValues.depthStencil().depth(1);
            renderPassInfo.pClearValues(clearValues);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS)
                throw new RuntimeException("Failed to begin recording command buffer");

            renderPassInfo.framebuffer(swapChain.getFrameBuffers().get(imageIndex));

            VkViewport.Buffer viewport = VkViewport.malloc(1, stack);
            viewport.x(0);
            viewport.y(0);
            viewport.width(swapChain.getExtent().width());
            viewport.height(swapChain.getExtent().height());
            viewport.maxDepth(0);
            viewport.maxDepth(1);
            vkCmdSetViewport(commandBuffer, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
            scissor.offset(VkOffset2D.malloc(stack).set(0, 0));
            scissor.extent(swapChain.getExtent());
            vkCmdSetScissor(commandBuffer, 0, scissor);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            {
                Scene scene = vkApplication.getScene();
                if (scene != null) {
                    scene.drawPipeline(drawer);
                }
            }
            vkCmdEndRenderPass(commandBuffer);

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS)
                throw new RuntimeException("Failed to record command buffer");
        }
    }
}
