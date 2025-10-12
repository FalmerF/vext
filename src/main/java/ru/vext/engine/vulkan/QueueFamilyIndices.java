package ru.vext.engine.vulkan;

import lombok.Data;

import java.util.stream.IntStream;

@Data
public class QueueFamilyIndices {

    private Integer graphicsFamily;
    private Integer presentFamily;

    public boolean isComplete() {
        return graphicsFamily != null && presentFamily != null;
    }

    public int[] unique() {
        return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
    }

    public int[] array() {
        return new int[]{graphicsFamily, presentFamily};
    }
}
