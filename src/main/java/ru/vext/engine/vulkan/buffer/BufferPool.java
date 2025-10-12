package ru.vext.engine.vulkan.buffer;

import ru.vext.engine.vulkan.VkApplication;

import java.util.HashSet;
import java.util.Set;

public class BufferPool {

    private final VkApplication vkApplication;

    private final Set<MemoryBuffer> tempBuffers = new HashSet<>();

    public BufferPool(VkApplication vkApplication) {
        this.vkApplication = vkApplication;
    }

    public MemoryBuffer createTemp(Object[] data, int usage, int memoryType) {
        MemoryBuffer buffer = new MemoryBuffer(vkApplication, data, usage, memoryType);
        tempBuffers.add(buffer);
        return buffer;
    }

    public void cleanup() {
        for (MemoryBuffer buffer : tempBuffers) {
            buffer.cleanup();
        }
        tempBuffers.clear();
    }
}
