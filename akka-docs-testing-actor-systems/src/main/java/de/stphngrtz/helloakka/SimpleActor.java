package de.stphngrtz.helloakka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class SimpleActor extends AbstractActor {

    public static Props props() {
        return Props.create(SimpleActor.class);
    }

    private ActorRef target;

    public SimpleActor() {
        receive(ReceiveBuilder
                        .matchEquals("hello", m -> {
                            sender().tell("world", self());
                            if (target != null)
                                target.forward(m, getContext());
                        })
                        .match(ActorRef.class, t -> {
                            target = t;
                            sender().tell("done", self());
                        })
                        .build()
        );
    }
}
