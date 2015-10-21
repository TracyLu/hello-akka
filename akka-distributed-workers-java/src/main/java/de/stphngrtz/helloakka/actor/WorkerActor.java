package de.stphngrtz.helloakka.actor;

import akka.actor.*;
import akka.cluster.client.ClusterClient;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import de.stphngrtz.helloakka.MessageProtocol;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WorkerActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(context().system(), this);

    public static Props props(ActorRef clusterClient, Props workExecutorActorProps) {
        return Props.create(WorkerActor.class, clusterClient, workExecutorActorProps, Duration.create(10, TimeUnit.SECONDS));
    }

    private final UUID workerId = UUID.randomUUID();
    private final Cancellable registerTask;
    private final ActorRef workExecutor;
    private final ActorRef master;
    private String currentWorkId;
    private Object lastResult;

    private PartialFunction<Object, BoxedUnit> idle;
    private PartialFunction<Object, BoxedUnit> working;
    private PartialFunction<Object, BoxedUnit> waiting;

    public WorkerActor(ActorRef clusterClient, Props workExecutorActorProps, FiniteDuration registerInterval) {
        master = clusterClient;
        workExecutor = context().watch(context().actorOf(workExecutorActorProps, "executor"));

        registerTask = context().system().scheduler().schedule(
                Duration.Zero(),
                registerInterval,
                clusterClient,
                new ClusterClient.SendToAll("/user/master/singleton", new MessageProtocol.RegisterWorker(workerId)),
                context().dispatcher(),
                self()
        );

        idle = ReceiveBuilder
                .match(MessageProtocol.WorkIsReady.class, message -> {
                    sendToMaster(new MessageProtocol.WorkerRequestsWork(workerId));
                })
                .match(MessageProtocol.Work.class, message -> {
                    log.info("Got work: {}", message);
                    currentWorkId = message.workId;
                    workExecutor.tell(message.job, self());
                    context().become(working);
                })
                .match(Terminated.class, message -> {
                    if (message.actor().equals(workExecutor))
                        context().stop(self());
                })
                .build();

        working = ReceiveBuilder
                .match(MessageProtocol.WorkCompleted.class, message -> {
                    lastResult = message.result;
                    log.info("Work completed. Result: {}", lastResult);
                    sendToMaster(new MessageProtocol.WorkIsDone(currentWorkId, workerId, lastResult));
                    context().setReceiveTimeout(Duration.create(5, TimeUnit.SECONDS));
                    context().become(waiting);
                })
                .match(MessageProtocol.Work.class, message -> {
                    log.info("Yikes! Master told me to work, while I'm working.");
                })
                .match(Terminated.class, message -> {
                    if (message.actor().equals(workExecutor))
                        context().stop(self());
                })
                .build();

        waiting = ReceiveBuilder
                .match(MessageProtocol.Ack.class, message -> {
                    if (Objects.equals(message.workId, currentWorkId)) {
                        sendToMaster(new MessageProtocol.WorkerRequestsWork(workerId));
                        context().setReceiveTimeout(Duration.Undefined());
                        context().become(idle);
                    }
                })
                .match(ReceiveTimeout.class, message -> {
                    log.info("No ack from master, retrying {} -> {}", workerId, currentWorkId);
                    sendToMaster(new MessageProtocol.WorkIsDone(currentWorkId, workerId, lastResult));
                })
                .build();

        context().become(idle);

        /*
        receive(ReceiveBuilder
                        .matchAny(message -> System.out.println(this.getClass().getSimpleName()))
                        .build()
        );
        */
    }

    @Override
    public void postStop() throws Exception {
        registerTask.cancel();
        super.postStop();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(-1, Duration.Inf(), DeciderBuilder
                .match(ActorInitializationException.class, e -> SupervisorStrategy.stop())
                .match(DeathPactException.class, e -> SupervisorStrategy.stop())
                .match(Exception.class, e -> {
                    if (currentWorkId != null) {
                        sendToMaster(new MessageProtocol.WorkFailed(currentWorkId, workerId));
                    }
                    getContext().become(idle);
                    return SupervisorStrategy.restart();
                })
                .matchAny(e -> SupervisorStrategy.escalate())
                .build()
        );
    }

    private void sendToMaster(Object message) {
        master.tell(new ClusterClient.SendToAll("/user/master/singleton", message), self());
    }
}
