package de.stphngrtz.helloakka;

import java.io.Serializable;
import java.math.BigInteger;

public class Protocol {

    public static class FactorialResult implements Serializable {
        public final Integer n;
        public final BigInteger factorial;

        public FactorialResult(Integer n, BigInteger factorial) {
            this.n = n;
            this.factorial = factorial;
        }
    }
}
