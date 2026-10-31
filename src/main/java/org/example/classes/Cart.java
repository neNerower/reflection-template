package org.example.classes;

import java.util.List;
import java.util.Map;

public class Cart {
    private List<Product> items;
    private Map<String, Product> map;

    public Cart(List<Product> items) {
        this.items = items;
    }

    public Cart(List<Product> items, Map<String, Product> map) {
        this.items = items;
        this.map = map;
    }

    public List<Product> getItems() {
        return items;
    }

    public void setItems(List<Product> items) {
        this.items = items;
    }

    // Конструктор, методы добавления и удаления товаров, геттеры и другие методы


    @Override
    public String toString() {
        return "Cart{" +
            "items=" + items +
            ", map=" + map +
            '}';
    }
}