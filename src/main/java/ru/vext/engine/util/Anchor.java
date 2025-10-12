package ru.vext.engine.util;

public record Anchor(float x, float y) {

    public static final Anchor LEFT_TOP = new Anchor(0, 0);
    public static final Anchor TOP = new Anchor(0.5f, 0);
    public static final Anchor RIGHT_TOP = new Anchor(1, 0);
    public static final Anchor RIGHT = new Anchor(1, 0.5f);
    public static final Anchor RIGHT_BOTTOM = new Anchor(1, 1);
    public static final Anchor BOTTOM = new Anchor(0.5f, 1);
    public static final Anchor LEFT_BOTTOM = new Anchor(0, 1);
    public static final Anchor LEFT = new Anchor(0, 0.5f);
    public static final Anchor CENTER = new Anchor(0.5f, 0.5f);

}
