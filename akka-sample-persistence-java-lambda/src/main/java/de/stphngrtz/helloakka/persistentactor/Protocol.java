package de.stphngrtz.helloakka.persistentactor;

import java.io.Serializable;

public abstract class Protocol {

    public static final class Cmd implements Serializable {
        private final String data;

        public Cmd(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    public static final class Evt implements Serializable {
        private final String data;

        public Evt(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }
}
