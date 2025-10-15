package ru.vext.engine.resource;

import javax.annotation.Nullable;
import java.util.*;

public class ResourceStorage {

    private final Map<ResourceType, Map<String, IResource>> resourceMap = new EnumMap<>(ResourceType.class);

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

    public void cleanup() {
        for (Map<String, IResource> rMap : resourceMap.values()) {
            for (IResource resource : rMap.values()) {
                resource.cleanup();
            }
        }
    }
}
