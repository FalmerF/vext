package ru.vext.engine;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.vext.engine.component.Scene;
import ru.vext.engine.vulkan.VkApplication;

@Slf4j
public abstract class VextApplication {

    private final VkApplication vkApplication;

    @Getter
    private Scene scene;

    public VextApplication(String windowTitle) {
        log.info("Initializing Vext Application");

        vkApplication = new VkApplication(windowTitle);
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
