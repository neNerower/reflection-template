package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Generator {

    private static final Random rnd = new Random();

    private static class GenerationContext {
        private final Map<Class<?>, Integer> depth = new HashMap<>();

        private static final int MAX_DEPTH = 3;

        boolean canGenerate(Class<?> clazz) {
            return depth.getOrDefault(clazz, 0) < MAX_DEPTH;
        }

        void enter(Class<?> clazz) {
            depth.merge(clazz, 1, Integer::sum);
        }

        void exit(Class<?> clazz) {
            int currentDepth = depth.get(clazz);

            if (currentDepth == 1) {
                depth.remove(clazz);
            } else {
                depth.put(clazz, currentDepth - 1);
            }
        }
    }

    public <T> T generateValueOfType(Class<T> clazz)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {

        return generateValueOfType(clazz, new GenerationContext());
    }

    private <T> T generateValueOfType(Class<T> clazz, GenerationContext context)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {

        if (isPrimitiveOrSpecialType(clazz)) {
            return generateValue(clazz, context);
        }

        if (!context.canGenerate(clazz)) {
            return null;
        }

        context.enter(clazz);

        try {
            if (clazz.isInterface()) {
                Generatable annotation = clazz.getAnnotation(Generatable.class);

                if (annotation != null) {
                    return generateImplementation(clazz, annotation, context);
                }

                return generateInterface(clazz, context);
            }

            if (Modifier.isAbstract(clazz.getModifiers())) {
                Generatable annotation = clazz.getAnnotation(Generatable.class);

                if (annotation != null) {
                    return generateImplementation(clazz, annotation, context);
                }

                throw new IllegalArgumentException(
                    "Abstract class must have @Generatable annotation: %s".formatted(clazz.getName()));
            }

            return generateConcreteClass(clazz, context);
        } finally {
            context.exit(clazz);
        }
    }

    private boolean isPrimitiveOrSpecialType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class || clazz.isEnum() || clazz.isArray() ||
            Number.class.isAssignableFrom(clazz) || clazz == Boolean.class || clazz == Character.class;
    }

    @SuppressWarnings("unchecked")
    private <T> T generateInterface(Class<T> clazz, GenerationContext context) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();

            // Методы без возвращаемого значения
            if (returnType == void.class) {
                return null;
            }

            // Генерируем значение на основании возвращаемого типа
            return generateValue(method.getGenericReturnType(), context);
        });
    }

    private <T> T generateImplementation(Class<T> interfaceClass, Generatable annotation, GenerationContext context)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {

        Class<?>[] implementations = annotation.implementations();

        if (implementations.length == 0) {
            throw new IllegalArgumentException("No implementations specified for " + interfaceClass.getName());
        }

        // Проверяем, что все классы действительно реализуют интерфейс
        for (Class<?> implementation : implementations) {
            if (!interfaceClass.isAssignableFrom(implementation)) {
                throw new IllegalArgumentException(
                    "%s does not implement %s".formatted(implementation.getName(), interfaceClass.getName()));
            }

            if (implementation.isInterface() || Modifier.isAbstract(implementation.getModifiers())) {
                throw new IllegalArgumentException(implementation.getName() + " is not a concrete class");
            }
        }

        // Случайный выбор реализации
        Class<?> implementation = implementations[rnd.nextInt(implementations.length)];

        // Создание объекта выбранной реализации
        return generateConcreteClass(implementation.asSubclass(interfaceClass), context);
    }

    @SuppressWarnings("unchecked")
    private <T> T generateConcreteClass(Class<T> clazz, GenerationContext context)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {

        Constructor<T>[] publicConstructors = Arrays.stream((Constructor<T>[]) clazz.getDeclaredConstructors())
            .filter(c -> Modifier.isPublic(c.getModifiers())).toArray(Constructor[]::new);

        if (publicConstructors.length == 0) {
            throw new IllegalArgumentException("%s does not have any public constructor".formatted(clazz.getName()));
        }

        int randomConstructorIndex = rnd.nextInt(publicConstructors.length);
        Constructor<T> randomConstructor = publicConstructors[randomConstructorIndex];

        List<Object> arguments = new ArrayList<>();
        for (Type type : randomConstructor.getGenericParameterTypes()) {
            arguments.add(generateValue(type, context));
        }

        return randomConstructor.newInstance(arguments.toArray());
    }

    @SuppressWarnings("unchecked")
    private <T> T generateValue(Type type, GenerationContext context)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {

        return switch (type) {
            case ParameterizedType parameterizedType -> {
                Type rawType = parameterizedType.getRawType();

                if (rawType == List.class) {
                    yield generateList(parameterizedType, context);
                } else if (rawType == Map.class) {
                    yield generateMap(parameterizedType, context);
                }

                throw new IllegalArgumentException("Unsupported parameterized type: " + type);
            }

            case Class<?> clazz -> (T) generateValue(clazz, context);

            case null, default -> throw new IllegalArgumentException("Unsupported generic parameter type: " + type);
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T generateMap(ParameterizedType parameterizedType, GenerationContext context)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {

        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        if (typeArguments.length != 2) {
            throw new IllegalArgumentException("Map must have exactly two type arguments: " + parameterizedType);
        }

        Type keyType = typeArguments[0];
        Type valueType = typeArguments[1];

        int size = rnd.nextInt(0, 5);

        Map<Object, Object> result = new HashMap<>(size);

        for (int i = 0; i < size; i++) {
            result.put(generateValue(keyType, context), generateValue(valueType, context));
        }

        return (T) result;
    }

    @SuppressWarnings("unchecked")
    private <T> T generateList(ParameterizedType parameterizedType, GenerationContext context)
        throws InvocationTargetException, InstantiationException, IllegalAccessException {

        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        if (typeArguments.length != 1) {
            throw new IllegalArgumentException("List must have exactly one type argument: " + parameterizedType);
        }

        Type elementType = typeArguments[0];
        int size = rnd.nextInt(0, 5);

        List<Object> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(generateValue(elementType, context));
        }

        return (T) result;
    }

    @SuppressWarnings("unchecked")
    private <T> T generateValue(Class<T> clazz, GenerationContext context)
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
        } else if (clazz.isEnum()) {
            T[] constants = clazz.getEnumConstants();
            int id = rnd.nextInt(constants.length);
            value = constants[id];
        } else if (clazz.isArray()) {
            int length = rnd.nextInt(0, 5);
            Class<?> componentType = clazz.getComponentType();

            Object array = Array.newInstance(componentType, length);

            for (int i = 0; i < length; i++) {
                Object element = generateValue(componentType, context);
                Array.set(array, i, element);
            }

            value = array;
        } else {
            value = generateValueOfType(clazz, context);
        }

        return (T) value;
    }
}
