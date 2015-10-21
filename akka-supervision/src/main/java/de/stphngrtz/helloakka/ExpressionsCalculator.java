package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpressionsCalculator extends AbstractLoggingActor {

    public static Props props(Expression expression, Position position) {
        return Props.create(ExpressionsCalculator.class, expression, position);
    }

    public enum Position {
        Left,
        Right
    }

    public static class FlakinessException extends RuntimeException {
        public FlakinessException() {
            super("FlakinessException");
        }
    }

    private final Expression expression;
    private final Position position;

    private final Set<Position> expected = Stream.of(Position.Left, Position.Right).collect(Collectors.toSet());
    private final Map<Position, Integer> results = new HashMap<>();

    public ExpressionsCalculator(Expression expression, Position position) {
        this.expression = expression;
        this.position = position;

        receive(ReceiveBuilder
                        .match(Protocol.Result.class, m -> expected.contains(m.position), m -> {
                            expected.remove(m.position);
                            results.put(m.position, m.result);
                            if (results.size() == 2) {
                                if (ThreadLocalRandom.current().nextDouble() < 0.2)
                                    throw new FlakinessException();

                                Integer result = evaluate(expression, results.get(Position.Left), results.get(Position.Right));
                                log().info("evaluated expression {} to value {}", expression, result);
                                context().parent().tell(new Protocol.Result(result, position), self());
                                context().stop(self());
                            }
                        })
                        .match(Protocol.Result.class, m -> {
                            throw new IllegalStateException("expected results for positions " + expected.stream().map(Object::toString).collect(Collectors.joining(",")) + " but got position " + m.position);
                        })
                        .build()
        );
    }

    private Integer evaluate(Expression expression, Integer left, Integer right) {
        if (expression instanceof Expression.Add)
            return left + right;
        else if (expression instanceof Expression.Multiply)
            return left * right;
        else if (expression instanceof Expression.Divide)
            return left / right;
        else
            throw new IllegalStateException("unknown expression:" + expression);
    }

    @Override
    public void preStart() throws Exception {
        if (expression instanceof Expression.Const) {
            int value = ((Expression.Const) expression).getValue();
            context().parent().tell(new Protocol.Result(value, position), self());
            context().stop(self());
        } else {
            context().actorOf(props(expression.getLeft(), Position.Left));
            context().actorOf(props(expression.getRight(), Position.Right));
        }
    }

    private Expression getExpression() {
        return expression;
    }

    private final SupervisorStrategy supervisorStrategy = new OneForOneStrategy(false, DeciderBuilder
            .match(FlakinessException.class, e -> {
                log().warning("evaluation of {} failed, restarting", getExpression());
                return SupervisorStrategy.restart();
            })
            .matchAny(e -> SupervisorStrategy.escalate())
            .build()
    );

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }
}
