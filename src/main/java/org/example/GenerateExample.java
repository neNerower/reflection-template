package org.example;


import org.example.classes.Shape;
import org.example.generator.Generator;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();
        try {
            Shape generated = gen.generateValueOfType(Shape.class);

            System.out.println(generated);
            System.out.println(generated.getArea());
            System.out.println(generated.getPerimeter());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}