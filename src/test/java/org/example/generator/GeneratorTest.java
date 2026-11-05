package org.example.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class GeneratorTest {

    private final Generator generator = new Generator();

    enum TestEnum {
        FIRST, SECOND, THIRD
    }

    interface SimpleInterface {
        String getName();
        int getAge();
        void doSomething();
    }

    static final class SimpleClass {
        private final String name;

        private final int age;

        public SimpleClass(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String name() {
            return name;
        }

        public int age() {
            return age;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (SimpleClass) obj;
            return Objects.equals(this.name, that.name) &&
                this.age == that.age;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "SimpleClass[" +
                "name=" + name + ", " +
                "age=" + age + ']';
        }
    }

    static class DefaultConstructorClass {
        public DefaultConstructorClass() {
        }
    }

    static class NoPublicConstructor {
        private NoPublicConstructor() {
        }
    }

    abstract static class AbstractClass {
    }

    @Generatable(implementations = {FirstImplementation.class, SecondImplementation.class})
    interface TestInterface {
    }

    static class FirstImplementation implements TestInterface {
        public FirstImplementation() {
        }
    }

    static class SecondImplementation implements TestInterface {
        public SecondImplementation() {
        }
    }

    @Generatable(implementations = {})
    interface EmptyImplementationInterface {
    }

    @Generatable(implementations = {String.class})
    interface InvalidImplementationInterface {
    }

    @Generatable(implementations = {AbstractImplementation.class})
    interface AbstractImplementationInterface {
    }

    abstract static class AbstractImplementation implements AbstractImplementationInterface {
    }

    @Generatable(implementations = {ConcreteImplementation.class})
    abstract static class AnnotatedAbstractClass {
    }

    static class ConcreteImplementation extends AnnotatedAbstractClass {
        public ConcreteImplementation() {
        }
    }

    static final class RecursiveClass {
        private final RecursiveClass child;

        public RecursiveClass(RecursiveClass child) {
            this.child = child;
        }

        public RecursiveClass child() {
            return child;
        }

    }

    static final class ListHolder {
        private final List<String> values;

        public ListHolder(List<String> values) {
            this.values = values;
        }

        public List<String> values() {
            return values;
        }

    }

    static final class MapHolder {
        private final Map<String, Integer> values;

        public MapHolder(Map<String, Integer> values) {
            this.values = values;
        }

        public Map<String, Integer> values() {
            return values;
        }
    }

    static final class GenericHolder {
        private final List<Integer> integers;

        private final Map<String, Long> map;

        public GenericHolder(List<Integer> integers, Map<String, Long> map) {
            this.integers = integers;
            this.map = map;
        }

        public List<Integer> integers() {
            return integers;
        }

        public Map<String, Long> map() {
            return map;
        }
    }

    @Test
    void shouldGeneratePrimitiveValues() throws Exception {
        assertThat(generator.generateValueOfType(int.class)).isNotNull();
        assertThat(generator.generateValueOfType(long.class)).isNotNull();
        assertThat(generator.generateValueOfType(double.class)).isNotNull();
        assertThat(generator.generateValueOfType(float.class)).isNotNull();
        assertThat(generator.generateValueOfType(boolean.class)).isNotNull();
        assertThat(generator.generateValueOfType(byte.class)).isBetween((byte) 0, Byte.MAX_VALUE);
        assertThat(generator.generateValueOfType(short.class)).isBetween((short) 0, Short.MAX_VALUE);
        assertThat(generator.generateValueOfType(char.class)).isBetween('a', 'z');
    }

    @Test
    void shouldGenerateWrapperValues() throws Exception {
        assertThat(generator.generateValueOfType(Integer.class)).isNotNull();
        assertThat(generator.generateValueOfType(Long.class)).isNotNull();
        assertThat(generator.generateValueOfType(Double.class)).isNotNull();
        assertThat(generator.generateValueOfType(Float.class)).isNotNull();
        assertThat(generator.generateValueOfType(Boolean.class)).isNotNull();
        assertThat(generator.generateValueOfType(Byte.class)).isBetween((byte) 0, Byte.MAX_VALUE);
        assertThat(generator.generateValueOfType(Short.class)).isBetween((short) 0, Short.MAX_VALUE);
        assertThat(generator.generateValueOfType(Character.class)).isBetween('a', 'z');
    }

    @Test
    void shouldGenerateString() throws Exception {
        String result = generator.generateValueOfType(String.class);

        assertThat(result).isNotNull()
            .startsWith("value-");
    }

    @Test
    void shouldGenerateEnum() throws Exception {
        TestEnum result = generator.generateValueOfType(TestEnum.class);

        assertThat(result).isIn((Object[]) TestEnum.values());
    }

    @Test
    void shouldGeneratePrimitiveArray() throws Exception {
        int[] result = generator.generateValueOfType(int[].class);

        assertThat(result).isNotNull()
            .hasSizeBetween(0, 4);
    }

    @Test
    void shouldGenerateObjectArray() throws Exception {
        String[] result = generator.generateValueOfType(String[].class);

        assertThat(result).isNotNull()
            .hasSizeBetween(0, 4);

        assertThat(result).allMatch(value ->
            value != null && value.startsWith("value-"));
    }

    @Test
    void shouldGenerateMultidimensionalArray() throws Exception {
        int[][] result = generator.generateValueOfType(int[][].class);

        assertThat(result).isNotNull();
        assertThat(result.length).isBetween(0, 4);
    }

    @Test
    void shouldGenerateConcreteClass() throws Exception {
        SimpleClass result = generator.generateValueOfType(SimpleClass.class);

        assertThat(result).isNotNull();
        assertThat(result.name()).startsWith("value-");
    }

    @Test
    void shouldGenerateClassWithDefaultConstructor() throws Exception {
        DefaultConstructorClass result =
            generator.generateValueOfType(DefaultConstructorClass.class);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldFailWhenClassHasNoPublicConstructor() {
        assertThatThrownBy(() ->
            generator.generateValueOfType(NoPublicConstructor.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not have any public constructor");
    }

    @Test
    void shouldGenerateInterfaceProxy() throws Exception {
        SimpleInterface result =
            generator.generateValueOfType(SimpleInterface.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).startsWith("value-");
        assertThat(result.getAge()).isNotNull();

        assertThatCode(result::doSomething).doesNotThrowAnyException();
    }

    @Test
    void shouldGenerateInterfaceImplementation() throws Exception {
        TestInterface result =
            generator.generateValueOfType(TestInterface.class);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOfAny(
            FirstImplementation.class,
            SecondImplementation.class
        );
    }

    @Test
    void shouldFailWhenNoImplementationsSpecified() {
        assertThatThrownBy(() ->
            generator.generateValueOfType(EmptyImplementationInterface.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No implementations specified");
    }

    @Test
    void shouldFailWhenImplementationDoesNotImplementInterface() {
        assertThatThrownBy(() ->
            generator.generateValueOfType(InvalidImplementationInterface.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not implement");
    }

    @Test
    void shouldFailWhenImplementationIsAbstract() {
        assertThatThrownBy(() ->
            generator.generateValueOfType(AbstractImplementationInterface.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not a concrete class");
    }

    @Test
    void shouldGenerateAnnotatedAbstractClass() throws Exception {
        AnnotatedAbstractClass result =
            generator.generateValueOfType(AnnotatedAbstractClass.class);

        assertThat(result).isInstanceOf(ConcreteImplementation.class);
    }

    @Test
    void shouldFailWhenAbstractClassHasNoAnnotation() {
        assertThatThrownBy(() ->
            generator.generateValueOfType(AbstractClass.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Abstract class must have @Generatable annotation");
    }

    @Test
    void shouldGenerateList() throws Exception {
        ListHolder result = generator.generateValueOfType(ListHolder.class);

        assertThat(result.values()).isNotNull()
            .hasSizeBetween(0, 4);

        assertThat(result.values()).allMatch(value ->
            value != null && value.startsWith("value-"));
    }

    @Test
    void shouldGenerateMap() throws Exception {
        MapHolder result = generator.generateValueOfType(MapHolder.class);

        assertThat(result.values()).isNotNull()
            .hasSizeBetween(0, 4);

        assertThat(result.values().keySet()).allMatch(key ->
            key.startsWith("value-"));

        assertThat(result.values().values()).allMatch(value ->
            value != null);
    }

    @Test
    void shouldGenerateMultipleGenericFields() throws Exception {
        GenericHolder result = generator.generateValueOfType(GenericHolder.class);

        assertThat(result.integers()).isNotNull()
            .hasSizeBetween(0, 4);

        assertThat(result.map()).isNotNull()
            .hasSizeBetween(0, 4);
    }

    @Test
    void shouldRespectMaximumRecursionDepth() throws Exception {
        RecursiveClass result =
            generator.generateValueOfType(RecursiveClass.class);

        assertThat(result).isNotNull();

        assertThat(result.child()).isNotNull();
        assertThat(result.child().child()).isNotNull();
        assertThat(result.child().child().child()).isNull();
    }
}
