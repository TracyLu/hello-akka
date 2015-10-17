package de.stphngrtz.helloakka.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.client.ClusterClient;
import akka.japi.pf.ReceiveBuilder;
import de.stphngrtz.helloakka.MessageProtocol;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WorkerActor extends AbstractActor {

    public static Props props(ActorRef clusterClient, Props workExecutorActorProps) {
        return Props.create(WorkerActor.class, clusterClient, workExecutorActorProps, Duration.create(10, TimeUnit.SECONDS));
    }

    private final UUID workerId = UUID.randomUUID();
    private final Cancellable registerTask;

    public WorkerActor(ActorRef clusterClient, Props workExecutorActorProps, FiniteDuration registerInterval) {
        registerTask = context().system().scheduler().schedule(
                Duration.Zero(),
                registerInterval,
                clusterClient,
                new ClusterClient.SendToAll("/user/master/singleton", new MessageProtocol.RegisterWorker(workerId)),
                context().dispatcher(),
                self()
        );

        receive(ReceiveBuilder
                        .matchAny(message -> System.out.println(this.getClass().getSimpleName()))
                        .build()
        );
    }

    @Override
    public void postStop() throws Exception {
        registerTask.cancel();
        super.postStop();
    }
}
