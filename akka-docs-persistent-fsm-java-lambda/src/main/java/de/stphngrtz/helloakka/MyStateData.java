package de.stphngrtz.helloakka;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MyStateData {

    public static final class ShoppingCart {
        private final List<Item> items = new ArrayList<>();

        public List<Item> getItems() {
            return Collections.unmodifiableList(items);
        }

        public void addItem(Item item) {
            items.add(item);
        }

        public void clear() {
            items.clear();
        }

        @Override
        public String toString() {
            return "ShoppingCart: " + items;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShoppingCart that = (ShoppingCart) o;
            return Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {
            return Objects.hash(items);
        }
    }

    public static final class Item implements Serializable {
        public final String id;
        public final String name;
        public final float price;

        public Item(String id, String name, float price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return "Item: " + name + " " + price + "€ (id:"+id+")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return Objects.equals(price, item.price) &&
                    Objects.equals(id, item.id) &&
                    Objects.equals(name, item.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, price);
        }
    }
}
