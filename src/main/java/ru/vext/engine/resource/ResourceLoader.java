package ru.vext.engine.resource;

import lombok.extern.slf4j.Slf4j;
import ru.vext.engine.resource.font.FontLoader;
import ru.vext.engine.vulkan.VkApplication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class ResourceLoader {

    private static final String RESOURCES_PATH = "assets/";

    private final Set<IResourceLoader> loaders = new HashSet<>();

    private final ResourceStorage resourceStorage;

    public ResourceLoader(VkApplication vkApplication, ResourceStorage resourceStorage) {
        this.resourceStorage = resourceStorage;

        loaders.add(new FontLoader(vkApplication));
    }

    public void loadAllResourcesToStorage() throws URISyntaxException, IOException {
        Set<String> entries = getResourcesFromJarFile();
        Set<IResource> resources = loadResources(entries);
        resources.forEach(resourceStorage::putResource);
    }

    private Set<String> getResourcesFromJarFile() throws URISyntaxException, IOException {
        URL jarUrl = ResourceLoader.class.getProtectionDomain().getCodeSource().getLocation();
        File jarFile = new File(jarUrl.toURI());

        log.info("Jar file path: {}", jarFile.getAbsolutePath());

        Set<String> entriesSet = new HashSet<>();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(RESOURCES_PATH)) {
                    entriesSet.add(entryName);
                }
            }
        }

        return entriesSet;
    }

    private Set<IResource> loadResources(Set<String> entries) {
        Set<IResource> resources = new HashSet<>();

        for (String entry : entries) {
            String key = entry.substring(RESOURCES_PATH.length());

            Optional<IResourceLoader> loaderOptional = findLoaderForEntry(key);

            if (loaderOptional.isEmpty()) {
                continue;
            }

            IResourceLoader loader = loaderOptional.get();

            try (InputStream is = getEntryInputStream(entry)) {
                IResource resource = loader.load(key, is);
                resources.add(resource);
            } catch (Exception e) {
                log.error("Error on load resource '{}'", entry, e);
            }
        }

        return resources;
    }

    private Optional<IResourceLoader> findLoaderForEntry(String entry) {
        return loaders.stream()
                .filter(l -> l.isMatchResource(entry))
                .findFirst();
    }

    private InputStream getEntryInputStream(String entry) {
        return ResourceLoader.class.getResourceAsStream("/" + entry);
    }
}
