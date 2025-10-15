package ru.vext.engine.resource;

public interface IResource {

    String getKey();

    ResourceType getType();

    void cleanup();

}
