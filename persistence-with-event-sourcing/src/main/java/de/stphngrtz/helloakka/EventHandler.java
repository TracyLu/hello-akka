package de.stphngrtz.helloakka;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

public class EventHandler extends AbstractActor {

    public static Props props() {
        return Props.create(EventHandler.class);
    }

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    public EventHandler() {
        receive(ReceiveBuilder
                .matchAny(event -> log.info("Handled Event:" + event))
                .build()
        );
    }
}
