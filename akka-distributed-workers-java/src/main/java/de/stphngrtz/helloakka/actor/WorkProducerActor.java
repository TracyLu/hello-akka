package de.stphngrtz.helloakka.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class WorkProducerActor extends AbstractActor {

    public static Props props(ActorRef frontend) {
        return Props.create(WorkProducerActor.class, frontend);
    }

    public WorkProducerActor(ActorRef frontend) {
        receive(ReceiveBuilder
                        .matchAny(message -> System.out.println(this.getClass().getSimpleName()))
                        .build()
        );
    }
}
