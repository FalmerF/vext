package ru.vext.engine.vulkan.render;

import lombok.Getter;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.buffer.BufferPool;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackGet;

@Getter
public final class Frame {

    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long fence;

    private final BufferPool bufferPool;

    public Frame(VkApplication vkApplication, long imageAvailableSemaphore, long renderFinishedSemaphore, long fence) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;

        bufferPool = new BufferPool(vkApplication);
    }

    public LongBuffer pImageAvailableSemaphore() {
        return stackGet().longs(imageAvailableSemaphore);
    }

    public LongBuffer pRenderFinishedSemaphore() {
        return stackGet().longs(renderFinishedSemaphore);
    }

    public LongBuffer pFence() {
        return stackGet().longs(fence);
    }
}
