package ru.vext.engine.vulkan;

import lombok.Getter;
import lombok.Setter;
import ru.vext.engine.component.Scene;
import ru.vext.engine.util.MemoryUtil;
import ru.vext.engine.vulkan.buffer.DefaultBuffers;
import ru.vext.engine.vulkan.fabric.InstanceFabric;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import ru.vext.engine.vulkan.render.RenderPipeline;
import ru.vext.engine.vulkan.render.font.FontDrawer;
import ru.vext.engine.vulkan.swapchain.pipeline.graphics.DefaultGraphicsPipeline;
import ru.vext.engine.vulkan.swapchain.pipeline.graphics.GraphicsPipeline;
import ru.vext.engine.vulkan.swapchain.SwapChain;
import ru.vext.engine.vulkan.swapchain.SwapChainSupportDetails;
import ru.vext.engine.vulkan.swapchain.pipeline.graphics.FontGraphicsPipeline;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
@Getter
public class VkApplication {

    private static final boolean ENABLE_VALIDATION_LAYERS = false;
    public static final int MAX_FRAMES_IN_FLIGHT = 2;

    private static final Set<String> VALIDATION_LAYERS;

    static {
        if (ENABLE_VALIDATION_LAYERS) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
        } else
            VALIDATION_LAYERS = null;
    }

    private static final double TARGET_FRAME_TIME = 1.0 / 30.0;

    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(Collectors.toSet());


    private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {

        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

        System.err.println("Validation layer: " + callbackData.pMessageString());

        return VK_FALSE;
    }

    private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo, VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger) {

        if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger, VkAllocationCallbacks allocationCallbacks) {

        if (vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
        }

    }

    private long window;

    private int width = 800, height = 600;

    private VkInstance instance;
    private long debugMessenger;
    private long surface;

    private VkPhysicalDevice physicalDevice;
    private VkDevice device;

    private VkQueue graphicsQueue;
    private VkQueue presentQueue;

    private SwapChain swapChain;

    private long commandPool;

    private RenderPipeline renderPipeline;
    private DefaultBuffers defaultBuffers;

    @Setter
    private boolean framebufferResized;

    private FontDrawer defaultFontDrawer;

    @Setter
    private Scene scene;

    public VkApplication(String windowTitle) {
        log.info("Initializing Vulkan Application");
        initWindow(windowTitle);
        initVulkan(new InstanceFabric());
    }

    private void initWindow(String windowTitle) {
        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, windowTitle, NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Cannot create window");
        }

        glfwSetWindowAttrib(window, GLFW_RESIZABLE, GLFW_TRUE);

        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            if (width == 0 && height == 0) return;
            this.width = width;
            this.height = height;
            framebufferResized = true;
        });
    }

    private void initVulkan(InstanceFabric instanceFabric) {
        instance = instanceFabric.createInstance(ENABLE_VALIDATION_LAYERS, List.of("VK_LAYER_KHRONOS_validation"));
        setupDebugMessenger();
        createSurface();
        pickPhysicalDevice();
        createLogicalDevice();
        createCommandPool();

        defaultBuffers = new DefaultBuffers(this);
        defaultBuffers.create();

        defaultFontDrawer = new FontDrawer(this, Objects.requireNonNull(VkApplication.class.getResourceAsStream("/fonts/segoeui.ttf")));

        swapChain = new SwapChain(this, new GraphicsPipeline[]{
                new DefaultGraphicsPipeline(
                        this, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP,
                        "/shader/default-triangle-strip.vert", "/shader/default.frag"
                ),
                new FontGraphicsPipeline(
                        this, defaultFontDrawer,
                        "/shader/font.vert", "/shader/font.frag"
                )
        });
        swapChain.create();

        renderPipeline = new RenderPipeline(this, swapChain, MAX_FRAMES_IN_FLIGHT);
        renderPipeline.create();
    }

    public void recreateSwapChain() {
        vkDeviceWaitIdle(device);
        swapChain.cleanup();
        swapChain.create();
    }

    public void mainLoop() {
        long previousTime = System.nanoTime();

        while (!glfwWindowShouldClose(window)) {
            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - previousTime) / 1e9;

            if (deltaTime < TARGET_FRAME_TIME) {
                try {
                    Thread.sleep((long) ((TARGET_FRAME_TIME - deltaTime) * 1000));
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }

            previousTime = currentTime;

            glfwPollEvents();

            if (framebufferResized) {
                framebufferResized = false;
                recreateSwapChain();
            }

            renderPipeline.drawFrame();
        }

        vkDeviceWaitIdle(device);
    }

    public void cleanup() {
        swapChain.cleanup();
        defaultFontDrawer.cleanup();
        defaultBuffers.cleanup();
        renderPipeline.cleanup();

        vkDestroyCommandPool(device, commandPool, null);

        vkDestroyDevice(device, null);

        if (ENABLE_VALIDATION_LAYERS) {
            destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(VkApplication::debugCallback);
    }

    private void setupDebugMessenger() {
        if (!ENABLE_VALIDATION_LAYERS) return;

        try (MemoryStack stack = stackPush()) {

            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);

            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

            if (createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger) != VK_SUCCESS)
                throw new RuntimeException("Failed to set up debug messenger");

            debugMessenger = pDebugMessenger.get(0);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            if (glfwCreateWindowSurface(instance, window, null, pSurface) != VK_SUCCESS)
                throw new RuntimeException("Failed to create window surface");

            surface = pSurface.get(0);
        }
    }

    private void pickPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

                if (isDeviceSuitable(device)) {
                    physicalDevice = device;
                    return;
                }
            }

            throw new RuntimeException("Failed to find a suitable GPU");
        }
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            QueueFamilyIndices indices = findQueueFamilies(physicalDevice);
            int[] uniqueQueueFamilies = indices.unique();
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);

            createInfo.pEnabledFeatures(deviceFeatures);

            createInfo.ppEnabledExtensionNames(MemoryUtil.asPointerBuffer(stack, DEVICE_EXTENSIONS));

            if (ENABLE_VALIDATION_LAYERS) {
                createInfo.ppEnabledLayerNames(MemoryUtil.asPointerBuffer(stack, VALIDATION_LAYERS));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(device, indices.getGraphicsFamily(), 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, indices.getPresentFamily(), 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    private boolean isDeviceSuitable(VkPhysicalDevice device) {
        QueueFamilyIndices indices = findQueueFamilies(device);

        boolean extensionsSupported = checkDeviceExtensionSupport(device);
        boolean swapChainAdequate = false;

        if (extensionsSupported) {
            try (MemoryStack stack = stackPush()) {
                SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device, stack);
                swapChainAdequate = swapChainSupport.getFormats().hasRemaining() && swapChainSupport.getPresentModes().hasRemaining();
            }
        }

        return indices.isComplete() && extensionsSupported && swapChainAdequate;
    }

    private boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer extensionCount = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);
            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);

            return availableExtensions.stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toSet()).containsAll(DEVICE_EXTENSIONS);
        }
    }

    public SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.setCapabilities(VkSurfaceCapabilitiesKHR.malloc(stack));
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.getCapabilities());

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if (count.get(0) != 0) {
            details.setFormats(VkSurfaceFormatKHR.malloc(count.get(0), stack));
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.getFormats());
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null);

        if (count.get(0) != 0) {
            details.setPresentModes(stack.mallocInt(count.get(0)));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.getPresentModes());
        }

        return details;
    }

    public QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);
            IntBuffer presentSupport = stack.ints(VK_FALSE);

            for (int i = 0; i < queueFamilies.capacity() || !indices.isComplete(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.setGraphicsFamily(i);
                }

                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                if (presentSupport.get(0) == VK_TRUE) {
                    indices.setPresentFamily(i);
                }
            }

            return indices;
        }
    }

    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice);

            VkCommandPoolCreateInfo commandPoolInfo = VkCommandPoolCreateInfo.calloc(stack);
            commandPoolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            commandPoolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            commandPoolInfo.queueFamilyIndex(queueFamilyIndices.getGraphicsFamily());

            LongBuffer pCommandPool = stack.mallocLong(1);
            if (vkCreateCommandPool(device, commandPoolInfo, null, pCommandPool) != VK_SUCCESS)
                throw new RuntimeException("Failed to create command pool");

            commandPool = pCommandPool.get(0);
        }
    }

    public VkCommandBuffer beginSingleTimeCommands() {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandPool(commandPool)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);
            return commandBuffer;
        }
    }

    public void endSingleTimeCommands(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkEndCommandBuffer(commandBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.mallocPointer(1).put(0, commandBuffer));

            vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(graphicsQueue);

            vkFreeCommandBuffers(device, commandPool, commandBuffer);
        }
    }
}