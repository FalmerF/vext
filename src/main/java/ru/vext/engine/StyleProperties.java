package ru.vext.engine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StyleProperties {

    public static final String DEFAULT_FONT = "font/segoeui.ttf";

    private final String font;

}
