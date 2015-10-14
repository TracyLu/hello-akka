package de.stphngrtz.helloakka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class WorkerActor extends AbstractActor {

    public static Props props(ActorRef clusterClient, Props workExecutorActorProps) {
        return Props.create(WorkerActor.class, clusterClient, workExecutorActorProps, Duration.create(10, TimeUnit.SECONDS));
    }

    public WorkerActor(ActorRef clusterClient, Props workExecutorActorProps, FiniteDuration registerInterval) {
        receive(ReceiveBuilder
                        .matchAny(message -> System.out.println(this.getClass().getSimpleName()))
                        .build()
        );
    }
}
