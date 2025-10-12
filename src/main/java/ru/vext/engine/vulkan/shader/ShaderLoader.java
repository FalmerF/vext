package ru.vext.engine.vulkan.shader;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class ShaderLoader {

    public static ShaderInfo loadShaders(VkDevice device, MemoryStack stack, String... shaderPaths) {
        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(shaderPaths.length, stack);
        long[] shaderModules = new long[shaderPaths.length];

        for (int i = 0; i < shaderPaths.length; i++) {
            String path = shaderPaths[i];
            shaderModules[i] = createShaderModule(device, path);

            VkPipelineShaderStageCreateInfo shaderStageInfo = shaderStages.get(i);
            shaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderStageInfo.stage(getShaderType(path));
            shaderStageInfo.module(shaderModules[i]);
            shaderStageInfo.pName(stack.UTF8("main"));
        }

        return new ShaderInfo(device, shaderStages, shaderModules);
    }

    private static long createShaderModule(VkDevice device, String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer sourceBuffer = loadShaderSource(path, stack);

            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(stack);
            shaderModuleCreateInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            shaderModuleCreateInfo.pCode(sourceBuffer);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if (vkCreateShaderModule(device, shaderModuleCreateInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module: " + path);
            }

            return pShaderModule.get(0);
        }
    }

    private static ByteBuffer loadShaderSource(String path, MemoryStack stack) {
        if (!path.endsWith(".spv")) path += ".spv";

        try (InputStream inputStream = ShaderLoader.class.getResourceAsStream(path)) {
            assert inputStream != null;
            return stack.bytes(inputStream.readAllBytes());
        } catch (IOException e) {
            log.error("Failed to load shader source: {}", path, e);
            throw new RuntimeException(e);
        }
    }

    private static int getShaderType(String path) {
        if (path.endsWith(".frag"))
            return VK_SHADER_STAGE_FRAGMENT_BIT;
        else if (path.endsWith(".vert"))
            return VK_SHADER_STAGE_VERTEX_BIT;

        throw new RuntimeException("Unknown shader type: " + path);
    }
}
