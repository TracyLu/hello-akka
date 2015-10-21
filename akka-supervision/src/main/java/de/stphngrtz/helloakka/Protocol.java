package de.stphngrtz.helloakka;

import java.io.Serializable;

public abstract class Protocol {

    public static final class Calculate implements Serializable {
        public final Expression expression;

        public Calculate(Expression expression) {
            this.expression = expression;
        }

        @Override
        public String toString() {
            return "Calculate{expression=" + expression + "}";
        }
    }

    public static final class Result implements Serializable {
        public final int result;
        public final ExpressionsCalculator.Position position;

        public Result(int result, ExpressionsCalculator.Position position) {
            this.result = result;
            this.position = position;
        }

        @Override
        public String toString() {
            return "Result{result=" + result + ", position=" + position + "}";
        }
    }

    public static final class Failure implements Serializable {}
}
