package ru.vext.engine.resource;

import java.io.InputStream;

public interface IResourceLoader {

    IResource load(String key, InputStream inputStream);

    boolean isMatchResource(String name);

}
