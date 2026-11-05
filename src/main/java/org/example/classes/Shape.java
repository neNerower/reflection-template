package org.example.classes;

import org.example.generator.Generatable;

@Generatable(implementations = {
    Triangle.class,
    Rectangle.class
})
public interface Shape {
    double getArea();
    double getPerimeter();
}