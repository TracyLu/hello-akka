package de.stphngrtz.helloakka;

import java.io.Serializable;

public interface Expression extends Serializable {

    Expression getLeft();

    Expression getRight();

    abstract class AbstractExpression implements Expression {
        private final Expression left;
        private final Expression right;
        private final String operator;

        public AbstractExpression(Expression left, String operator, Expression right) {
            this.left = left;
            this.right = right;
            this.operator = operator;
        }

        @Override
        public Expression getLeft() {
            return left;
        }

        @Override
        public Expression getRight() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AbstractExpression that = (AbstractExpression) o;

            if (!left.equals(that.left)) return false;
            if (!right.equals(that.right)) return false;
            return operator.equals(that.operator);

        }

        @Override
        public int hashCode() {
            int result = left.hashCode();
            result = 31 * result + right.hashCode();
            result = 31 * result + operator.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "(" + left + operator + right + ")";
        }
    }

    class Add extends AbstractExpression {
        public Add(Expression left, Expression right) {
            super(left, "+", right);
        }
    }

    class Multiply extends AbstractExpression {
        public Multiply(Expression left, Expression right) {
            super(left, "*", right);
        }
    }

    class Divide extends AbstractExpression {
        public Divide(Expression left, Expression right) {
            super(left, "/", right);
        }

    }

    class Const implements Expression {
        private final int value;

        public Const(int value) {
            this.value = value;
        }

        @Override
        public Expression getLeft() {
            return this;
        }

        @Override
        public Expression getRight() {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Const aConst = (Const) o;

            return value == aConst.value;

        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public int getValue() {
            return value;
        }
    }
}
