package de.stphngrtz.helloakka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class WorkResultConsumerActor extends AbstractActor {

    public static Props props(ActorRef frontend) {
        return Props.create(WorkResultConsumerActor.class, frontend);
    }

    public WorkResultConsumerActor(ActorRef frontend) {
        receive(ReceiveBuilder
                        .matchAny(message -> System.out.println(this.getClass().getSimpleName()))
                        .build()
        );
    }
}
