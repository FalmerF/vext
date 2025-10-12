package ru.vext.engine.util;

import org.lwjgl.PointerBuffer;

import java.nio.Buffer;
import java.util.function.BiConsumer;

public class BufferUtil {

    public static void fillBuffer(Object[] numbers, PointerBuffer pb) {
        if (numbers instanceof Integer[]) {
            fillBuffer(numbers, pb.getIntBuffer(0, Integer.BYTES * numbers.length), (b, n) -> b.put(n.intValue()));
        } else if (numbers instanceof Long[]) {
            fillBuffer(numbers, pb.getLongBuffer(0, Long.BYTES * numbers.length), (b, n) -> b.put(n.longValue()));
        } else if (numbers instanceof Float[]) {
            fillBuffer(numbers, pb.getFloatBuffer(0, Float.BYTES * numbers.length), (b, n) -> b.put(n.floatValue()));
        } else if (numbers instanceof Double[]) {
            fillBuffer(numbers, pb.getDoubleBuffer(0, Double.BYTES * numbers.length), (b, n) -> b.put(n.doubleValue()));
        } else if (numbers instanceof Short[]) {
            fillBuffer(numbers, pb.getShortBuffer(0, Short.BYTES * numbers.length), (b, n) -> b.put(n.shortValue()));
        } else if (numbers instanceof Byte[]) {
            fillBuffer(numbers, pb.getByteBuffer(0, numbers.length), (b, n) -> b.put(n.byteValue()));
        } else {
            throw new IllegalArgumentException("Unsupported number type: " + numbers.getClass().getName());
        }
    }

    public static <T extends Buffer> void fillBuffer(Object[] numbers, T buffer, BiConsumer<T, Number> consumer) {
        for (Object number : numbers) {
            consumer.accept(buffer, (Number) number);
        }
    }

    public static int getDataSize(Object[] numbers) {
        if (numbers instanceof Integer[]) {
            return Integer.BYTES * numbers.length;
        } else if (numbers instanceof Long[]) {
            return Long.BYTES * numbers.length;
        } else if (numbers instanceof Float[]) {
            return Float.BYTES * numbers.length;
        } else if (numbers instanceof Double[]) {
            return Double.BYTES * numbers.length;
        } else if (numbers instanceof Short[]) {
            return Short.BYTES * numbers.length;
        } else if (numbers instanceof Byte[]) {
            return numbers.length;
        } else {
            throw new IllegalArgumentException("Unsupported number type: " + numbers.getClass().getName());
        }
    }

}
