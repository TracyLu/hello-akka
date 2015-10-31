package de.stphngrtz.helloakka;

import java.util.Objects;

public class MyEvent {

    interface DomainEvent {}

    public static final class ItemAdded implements DomainEvent {
        public final MyStateData.Item item;

        public ItemAdded(MyStateData.Item item) {
            this.item = item;
        }

        @Override
        public String toString() {
            return "ItemAdded: " + item;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemAdded itemAdded = (ItemAdded) o;
            return Objects.equals(item, itemAdded.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }

    public enum OrderExecuted implements DomainEvent {INSTANCE}
    public enum OrderDiscarded implements DomainEvent {INSTANCE}
}
