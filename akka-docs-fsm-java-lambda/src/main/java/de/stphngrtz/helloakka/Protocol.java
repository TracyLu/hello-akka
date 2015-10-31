package de.stphngrtz.helloakka;

import akka.actor.ActorRef;

import java.util.List;
import java.util.Objects;

public class Protocol {

    public static final class SetTarget {
        public final ActorRef target;

        public SetTarget(ActorRef target) {
            this.target = target;
        }

        @Override
        public String toString() {
            return "SetTarget: " + target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SetTarget setTarget = (SetTarget) o;
            return Objects.equals(target, setTarget.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target);
        }
    }

    public static final class Queue {
        public final Object object;

        public Queue(Object object) {
            this.object = object;
        }

        @Override
        public String toString() {
            return "Queue: " + object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Queue queue = (Queue) o;
            return Objects.equals(object, queue.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }
    }

    public static final class Batch {
        public final List<Object> list;

        public Batch(List<Object> list) {
            this.list = list;
        }

        @Override
        public String toString() {
            return "Batch: " + list;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Batch batch = (Batch) o;
            return Objects.equals(list, batch.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(list);
        }
    }

    public enum Flush {
        Flush
    }
}
