package ru.vext.engine.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class Unit {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("([0-9%px.]+)");
    private static final Pattern EXPRESSION_FIND_PATTERN = Pattern.compile("([0-9%px.\\(\\)]+)([+\\-*/]+)");

    public static float getScreenValue(String value, int size) {
        if (value == null || value.isEmpty() || value.equals("0")) {
            return 0;
        }

        value = value.replaceAll(" ", "");

        if (EXPRESSION_FIND_PATTERN.matcher(value).find()) {
            return parseExpression(value, size);
        } else {
            return parseValue(value, size);
        }
    }

    public static float parseExpression(String expression, int size) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);

        expression = matcher.replaceAll(result -> {
            String value = result.group();
            return String.valueOf(parseValue(value, size));
        });

        Expression e = new ExpressionBuilder(expression).build();
        return (float) e.evaluate();
    }

    public static float parseValue(String value, int size) {
        Format format = parseFormat(value);
        Matcher matcher = format.getPattern().matcher(value);
        if (matcher.find()) {
            float floatValue = Float.parseFloat(matcher.group(1));
            if (format == Format.PERCENT) {
                return (floatValue / 100f) * size;
            } else if (format == Format.PIXEL) {
                return floatValue;
            } else if (format == Format.NUMBER) {
                return floatValue;
            }
        }
        throw new IllegalArgumentException("Invalid value format: " + format);
    }

    public static Format parseFormat(String value) {
        for (Format format : Format.values) {
            if (format.getPattern().matcher(value).find()) {
                return format;
            }
        }

        throw new IllegalArgumentException("Invalid value format: " + value);
    }

    @Getter
    public enum Format {
        PERCENT("^([0-9.-]+)%$"),
        PIXEL("^([0-9.-]+)px$"),
        NUMBER("^([0-9.-]+)$");

        private static final Format[] values = Format.values();

        private final Pattern pattern;

        Format(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ExpressionPart {

        private final String value;
        private final ExpressionOperation operation;

    }

    @Getter
    @RequiredArgsConstructor
    public enum ExpressionOperation {
        ADD(0, Float::sum),
        SUBTRACT(0, (v1, v2) -> v1 - v2),
        MULTIPLY(1, (v1, v2) -> v1 * v2),
        DIVIDE(1, (v1, v2) -> v1 / v2),
        NO_OPERATION(-1, null);

        private final int priority;
        private final BiFunction<Float, Float, Float> function;

        public static ExpressionOperation getOperation(String operation) {
            if ("+".equals(operation)) return ExpressionOperation.ADD;
            else if ("-".equals(operation)) return ExpressionOperation.SUBTRACT;
            else if ("*".equals(operation)) return ExpressionOperation.MULTIPLY;
            else if ("/".equals(operation)) return ExpressionOperation.DIVIDE;

            return NO_OPERATION;
        }
    }
}
