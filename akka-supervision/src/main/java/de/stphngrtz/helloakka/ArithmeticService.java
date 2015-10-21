package de.stphngrtz.helloakka;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

import static de.stphngrtz.helloakka.ExpressionsCalculator.Position.Left;

public class ArithmeticService extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(ArithmeticService.class);
    }

    private final Map<ActorRef, ActorRef> pendingWorkers = new HashMap<>();

    public ArithmeticService() {
        receive(
                ReceiveBuilder
                        .match(Protocol.Calculate.class, this::calculate)
                        .match(Protocol.Result.class, this::notifyConsumerOnSuccess)
                        .build()
        );
    }

    private void calculate(Protocol.Calculate message) {
        ActorRef calculator = context().actorOf(ExpressionsCalculator.props(message.expression, Left));
        pendingWorkers.put(calculator, sender());
    }

    private void notifyConsumerOnSuccess(Protocol.Result message) {
        ActorRef actorRef = pendingWorkers.remove(sender());
        if (actorRef != null)
            actorRef.tell(message, self());
    }

    private void notifyConsumerOnFailure(Throwable e) {
        ActorRef actorRef = pendingWorkers.remove(sender());
        if (actorRef != null)
            actorRef.tell(new Protocol.Failure(), self());
    }

    private final SupervisorStrategy supervisorStrategy = new OneForOneStrategy(false, DeciderBuilder
            .match(ExpressionsCalculator.FlakinessException.class, e -> {
                log().warning("evaluation of a top level expression failed, restarting.");
                return SupervisorStrategy.restart();
            })
            .match(ArithmeticException.class, e -> {
                log().error("evaluation failed because of {}", e.getMessage());
                notifyConsumerOnFailure(e);
                return SupervisorStrategy.stop();
            })
            .match(Throwable.class, e -> {
                log().error("unexpected failure: {}", e.getMessage());
                notifyConsumerOnFailure(e);
                return SupervisorStrategy.stop();
            })
            .build()
    );

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }
}
