package ru.vext.engine.vulkan.fabric;

import ru.vext.engine.util.MemoryUtil;
import ru.vext.engine.util.VulkanUtil;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

@Slf4j
public class InstanceFabric {
    public VkInstance createInstance(boolean enableValidationLayers, Collection<String> validationLayers) {
        if (enableValidationLayers && !checkValidationLayerSupport(validationLayers))
            throw new RuntimeException("Validation requested but not supported");

        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe("Hello Triangle"));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe("No Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(getRequiredExtensions(stack, enableValidationLayers));

            if (enableValidationLayers) {
                createInfo.ppEnabledLayerNames(MemoryUtil.asPointerBuffer(stack, validationLayers));

                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                VulkanUtil.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePtr = stack.mallocPointer(1);

            if (vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            return new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    private boolean checkValidationLayerSupport(Collection<String> validationLayers) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer layerCount = stack.ints(0);
            vkEnumerateInstanceLayerProperties(layerCount, null);
            VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);
            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);
            Set<String> availableLayerNames = availableLayers.stream().map(VkLayerProperties::layerNameString).collect(Collectors.toSet());

            return availableLayerNames.containsAll(validationLayers);
        }
    }

    private PointerBuffer getRequiredExtensions(MemoryStack stack, boolean enableValidationLayers) {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if (enableValidationLayers) {
            assert glfwExtensions != null;
            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            return extensions.rewind();
        }

        return glfwExtensions;
    }
}
