package org.example;


import org.example.classes.Product;
import org.example.generator.Generator;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();
        try {
            Object generated = gen.generateValueOfType(Product.class);
            System.out.println(generated);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}