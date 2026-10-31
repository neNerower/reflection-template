package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Generator {
    private static final Random rnd = new Random();

    public <T> T generateValueOfType(Class<T> clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<T>[] constructors = (Constructor<T>[]) clazz.getDeclaredConstructors();

        int randomConstructorIndex = rnd.nextInt(constructors.length);
        Constructor<T> randomConstructor = constructors[randomConstructorIndex];

        List<Object> list = new ArrayList<>();
        for (Type type : randomConstructor.getGenericParameterTypes()) {
            Object generateValue = generateValue(type);
            list.add(generateValue);
        }
        Object[] arguments = list.toArray();

        return randomConstructor.newInstance(arguments);
    }

    @SuppressWarnings("unchecked")
    private <T> T generateValue(Type type)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {
        return switch (type) {
            case ParameterizedType parameterizedType -> {
                Type rawType = parameterizedType.getRawType();

                if (rawType == List.class) {
                    yield generateList(parameterizedType);
                } else if (rawType == Map.class) {
                    yield generateMap(parameterizedType);
                }

                throw new IllegalArgumentException("Unsupported parameterized type: " + type);
            }
            case Class<?> clazz -> (T) generateValue(clazz);
            case null, default ->
                throw new IllegalArgumentException("Unsupported generic parameter type: " + type);
        };
    }

    private <T> T generateMap(ParameterizedType parameterizedType)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        if (typeArguments.length != 2) {
            throw new IllegalArgumentException("Map must have exactly two type argument: " + parameterizedType);
        }

        Type keyType = typeArguments[0];
        Type valueType = typeArguments[1];
        int size = rnd.nextInt(0, 5);

        Map<Object, Object> result = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            result.put(generateValue(keyType), generateValue(valueType));
        }

        return (T) result;
    }

    private <T> T generateList(ParameterizedType parameterizedType)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        if (typeArguments.length != 1) {
            throw new IllegalArgumentException("List must have exactly one type argument: " + parameterizedType);
        }

        Type elementType = typeArguments[0];
        int size = rnd.nextInt(0, 5);

        List<Object> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(generateValue(elementType));
        }

        return (T) result;
    }


    @SuppressWarnings("unchecked")
    private <T> T generateValue(Class<T> clazz)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Object value;

        if (clazz == int.class || clazz == Integer.class) {
            value = rnd.nextInt();
        } else if (clazz == long.class || clazz == Long.class) {
            value = rnd.nextLong();
        } else if (clazz == double.class || clazz == Double.class) {
            value = rnd.nextDouble();
        } else if (clazz == float.class || clazz == Float.class) {
            value = rnd.nextFloat();
        } else if (clazz == boolean.class || clazz == Boolean.class) {
            value = rnd.nextBoolean();
        } else if (clazz == byte.class || clazz == Byte.class) {
            value = (byte) rnd.nextInt(Byte.MAX_VALUE + 1);
        } else if (clazz == short.class || clazz == Short.class) {
            value = (short) rnd.nextInt(Short.MAX_VALUE + 1);
        } else if (clazz == char.class || clazz == Character.class) {
            value = (char) ('a' + rnd.nextInt(26));
        } else if (clazz == String.class) {
            value = "value-" + rnd.nextInt();
        } else if (clazz.isArray()) {
            int length = rnd.nextInt(0, 5);
            Class<?> componentType = clazz.getComponentType();
            Object array = Array.newInstance(componentType, length);

            for (int i = 0; i < length; i++) {
                Object element = generateValue(componentType);
                Array.set(array, i, element);
            }
            value = array;
        } else {
            value = generateValueOfType(clazz);
        }

        return (T) value;
    }

}
