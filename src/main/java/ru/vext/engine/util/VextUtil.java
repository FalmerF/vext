package ru.vext.engine.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;

public class VextUtil {

    public static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    public static float[] collectionToPrimitiveFloatArray(Collection<?> collection) {
        float[] floats = new float[collection.size()];
        int i = 0;

        for (Object o : collection) {
            floats[i++] = Float.parseFloat(String.valueOf(o));
        }

        return floats;
    }

    public static int[] collectionToPrimitiveIntArray(Collection<?> collection) {
        int[] ints = new int[collection.size()];
        int i = 0;

        for (Object o : collection) {
            ints[i++] = Integer.parseInt(String.valueOf(o));
        }

        return ints;
    }

    public static double[] convertFloatArrayToDouble(float[] array) {
        double[] doubles = new double[array.length];

        for (int i = 0; i < array.length; i++) {
            doubles[i] = array[i];
        }

        return doubles;
    }

    public static short[] collectionToPrimitiveShortArray(Collection<?> collection) {
        short[] shorts = new short[collection.size()];
        int i = 0;

        for (Object o : collection) {
            shorts[i++] = Short.parseShort(String.valueOf(o));
        }

        return shorts;
    }

    public static void joinVerticesAndIndices(List<Float> dstVertices, List<Short> dstIndices, float[] srcVertices, short[] srcIndices) {
        int startIndex = dstVertices.size() / 2;

        for (short index : srcIndices) {
            dstIndices.add((short) (index + startIndex));
        }

        for (float point : srcVertices) {
            dstVertices.add(point);
        }
    }

    public static Integer[] primitiveToObjectArray(int[] array) {
        Integer[] objects = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            objects[i] = array[i];
        }
        return objects;
    }

    public static Float[] primitiveToObjectArray(float[] array) {
        Float[] objects = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            objects[i] = array[i];
        }
        return objects;
    }

    public static Double[] primitiveToObjectArray(double[] array) {
        Double[] objects = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            objects[i] = array[i];
        }
        return objects;
    }

    public static Short[] primitiveToObjectArray(short[] array) {
        Short[] objects = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            objects[i] = array[i];
        }
        return objects;
    }

    public static Byte[] primitiveToObjectArray(byte[] array) {
        Byte[] objects = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            objects[i] = array[i];
        }
        return objects;
    }

    public static Float[] getColorArray(Color color) {
        return VextUtil.primitiveToObjectArray(color.getRGBColorComponents(null));
    }
}
