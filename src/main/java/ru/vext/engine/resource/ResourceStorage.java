package ru.vext.engine.resource;

import lombok.RequiredArgsConstructor;
import ru.vext.engine.StyleProperties;
import ru.vext.engine.resource.font.BakedFont;

import javax.annotation.Nullable;
import java.util.*;

@RequiredArgsConstructor
public class ResourceStorage {

    private final Map<ResourceType, Map<String, IResource>> resourceMap = new EnumMap<>(ResourceType.class);

    private final StyleProperties styleProperties;

    public void putResource(IResource resource) {
        ResourceType type = resource.getType();
        Map<String, IResource> rMap = resourceMap.computeIfAbsent(type, key -> new HashMap<>());
        rMap.put(resource.getKey(), resource);
    }

    @Nullable
    public IResource findResource(String key, ResourceType type) {
        Map<String, IResource> rMap = resourceMap.get(type);

        if (rMap == null) {
            return null;
        }

        return rMap.get(key);
    }

    public Collection<IResource> findResourcesByType(ResourceType type) {
        Map<String, IResource> rMap = resourceMap.get(type);
        return rMap == null ? Collections.emptySet() : rMap.values();
    }

    public BakedFont getFont(String key) {
        if (key == null || key.isEmpty()) {
            return getDefaultFont();
        }

        return (BakedFont) findResource(key, ResourceType.FONT);
    }

    public BakedFont getDefaultFont() {
        return (BakedFont) findResource(StyleProperties.DEFAULT_FONT, ResourceType.FONT);
    }

    public void cleanup() {
        for (Map<String, IResource> rMap : resourceMap.values()) {
            for (IResource resource : rMap.values()) {
                resource.cleanup();
            }
        }
    }
}
