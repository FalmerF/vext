package ru.vext.engine.vulkan.shader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

@Getter
@AllArgsConstructor
public class ShaderInfo {

    private final VkDevice device;
    private final VkPipelineShaderStageCreateInfo.Buffer shaderStages;
    private final long[] shaderModules;

    public void destroy() {
        for (long shaderModule : shaderModules)
            vkDestroyShaderModule(device, shaderModule, null);
    }
}
