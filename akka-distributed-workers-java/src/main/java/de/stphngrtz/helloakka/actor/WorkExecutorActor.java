package de.stphngrtz.helloakka.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import de.stphngrtz.helloakka.MessageProtocol;

public class WorkExecutorActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props() {
        return Props.create(WorkExecutorActor.class);
    }

    public WorkExecutorActor() {
        receive(ReceiveBuilder
                        .match(Integer.class, message -> {
                            String result = message + " * " + message + " = " + (message * message);
                            log.info("Produced result: " + result);
                            sender().tell(new MessageProtocol.WorkCompleted(result), self());
                        })
                        .build()
        );
    }
}
