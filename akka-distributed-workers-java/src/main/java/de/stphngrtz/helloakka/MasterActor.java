package de.stphngrtz.helloakka;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class MasterActor extends AbstractActor {

    public static Props props() {
        return Props.create(MasterActor.class);
    }

    public MasterActor() {
        receive(ReceiveBuilder
                        .matchAny(message -> System.out.println(this.getClass().getSimpleName()))
                        .build()
        );
    }
}
