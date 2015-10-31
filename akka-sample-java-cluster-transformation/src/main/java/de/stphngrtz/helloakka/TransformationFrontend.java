package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;
import java.util.List;

public class TransformationFrontend extends AbstractLoggingActor {

    private List<ActorRef> backends = new ArrayList<>();
    private int jobCounter = 0;

    public TransformationFrontend() {
        receive(ReceiveBuilder
                        .match(Protocol.TransformationJob.class, m -> backends.isEmpty(), m -> sender().tell(new Protocol.JobFailed("no backends available", m), self()))
                        .match(Protocol.TransformationJob.class, m -> backends.get(jobCounter % backends.size()).forward(m, context()))
                        .matchEquals(Protocol.BackendRegistration, m -> {
                            context().watch(sender());
                            backends.add(sender());
                        })
                        .match(Terminated.class, m -> backends.remove(m.actor()))
                        .build()
        );
    }
}
