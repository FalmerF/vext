package ru.vext.engine;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.vext.engine.component.Scene;
import ru.vext.engine.resource.ResourceLoader;
import ru.vext.engine.resource.ResourceStorage;
import ru.vext.engine.vulkan.VkApplication;
import ru.vext.engine.vulkan.fabric.InstanceFabric;

import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
@Getter
public abstract class VextApplication {

    private final VkApplication vkApplication;
    private final ResourceStorage resourceStorage;
    private final ResourceLoader resourceLoader;

    @Getter
    private Scene scene;

    public VextApplication(String windowTitle) {
        log.info("Initializing Vext Application");

        resourceStorage = new ResourceStorage();

        vkApplication = new VkApplication(resourceStorage);
        vkApplication.initWindow(windowTitle);
        vkApplication.initVulkan(new InstanceFabric());

        resourceLoader = new ResourceLoader(vkApplication, resourceStorage);

        try {
            resourceLoader.loadAllResourcesToStorage();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        vkApplication.initRenderPipeline();

        setScene(new Scene());

        initialize();

        vkApplication.mainLoop();
        vkApplication.cleanup();
    }

    public void setScene(Scene scene) {
        scene.setVkApplication(vkApplication);
        this.scene = scene;
        vkApplication.setScene(scene);
    }

    public abstract void initialize();

}
