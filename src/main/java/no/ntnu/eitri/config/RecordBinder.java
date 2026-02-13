package no.ntnu.eitri.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection-based record binder with strict key/type validation.
 */
public final class RecordBinder {

    private RecordBinder() {
    }

    public static <T extends Record> T bindFlatRecord(
            Map<String, Object> input,
            Class<T> recordType,
            T defaults,
            String path) throws ConfigException {
        if (input == null || input.isEmpty()) {
            return defaults;
        }

        RecordComponent[] components = recordType.getRecordComponents();
        Map<String, RecordComponent> byName = new HashMap<>();
        for (RecordComponent component : components) {
            byName.put(component.getName(), component);
        }

        for (String key : input.keySet()) {
            if (!byName.containsKey(key)) {
                throw new ConfigException("Unknown config key: " + path + "." + key);
            }
        }

        Object[] args = new Object[components.length];
        Class<?>[] argTypes = new Class<?>[components.length];
        try {
            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];
                argTypes[i] = component.getType();

                Object raw = input.containsKey(component.getName())
                        ? input.get(component.getName())
                        : component.getAccessor().invoke(defaults);
                args[i] = convert(raw, component.getType(), path + "." + component.getName());
            }

            Constructor<T> constructor = recordType.getDeclaredConstructor(argTypes);
            return constructor.newInstance(args);
        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Failed to bind record at " + path + ": " + e.getMessage(), e);
        }
    }

    private static Object convert(Object raw, Class<?> targetType, String path) throws ConfigException {
        if (targetType == String.class) {
            if (raw instanceof String s) {
                return s;
            }
            throw new ConfigException("Invalid type for " + path + ": expected string");
        }
        if (targetType == int.class || targetType == Integer.class) {
            if (raw instanceof Number n) {
                return n.intValue();
            }
            throw new ConfigException("Invalid type for " + path + ": expected integer");
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (raw instanceof Boolean b) {
                return b;
            }
            throw new ConfigException("Invalid type for " + path + ": expected boolean");
        }
        if (targetType == LayoutDirection.class) {
            if (raw instanceof LayoutDirection layoutDirection) {
                return layoutDirection;
            }
            if (!(raw instanceof String s)) {
                throw new ConfigException("Invalid type for " + path + ": expected string");
            }
            String normalized = s.toLowerCase().trim();
            return switch (normalized) {
                case "lr", "left-to-right", "lefttoright", "ltr" -> LayoutDirection.LEFT_TO_RIGHT;
                case "tb", "top-to-bottom", "toptobottom", "ttb" -> LayoutDirection.TOP_TO_BOTTOM;
                default -> throw new ConfigException(
                        "Invalid value for " + path + ": " + raw + " (expected tb/top-to-bottom or lr/left-to-right)");
            };
        }
        throw new ConfigException("Unsupported config type at " + path + ": " + targetType.getSimpleName());
    }
}
