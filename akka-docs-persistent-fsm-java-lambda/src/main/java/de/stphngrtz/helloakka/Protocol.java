package de.stphngrtz.helloakka;

import java.util.List;
import java.util.Objects;

public class Protocol {

    public interface Command {}

    public static final class AddItem implements Command {
        public final MyStateData.Item item;

        public AddItem(MyStateData.Item item) {
            this.item = item;
        }

        @Override
        public String toString() {
            return "AddItem: " + item;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AddItem addItem = (AddItem) o;
            return Objects.equals(item, addItem.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }

    public enum Buy implements Command {INSTANCE}
    public enum Leave implements Command {INSTANCE}
    public enum GetCurrentCart implements Command {INSTANCE}

    public interface Report {}

    public static final class PurchaseWasMade implements Report {
        public final List<MyStateData.Item> items;

        public PurchaseWasMade(List<MyStateData.Item> items) {
            this.items = items;
        }

        @Override
        public String toString() {
            return "PurchaseWasMade: " + items;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PurchaseWasMade that = (PurchaseWasMade) o;
            return Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {
            return Objects.hash(items);
        }
    }

    public enum ShoppingCartDiscarded implements Report {INSTANCE}
}
