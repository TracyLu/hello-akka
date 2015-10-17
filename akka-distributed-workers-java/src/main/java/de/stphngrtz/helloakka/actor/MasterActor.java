package de.stphngrtz.helloakka.actor;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.client.ClusterClientReceptionist;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import de.stphngrtz.helloakka.MessageProtocol;
import de.stphngrtz.helloakka.WorkState;
import de.stphngrtz.helloakka.WorkerState;
import de.stphngrtz.helloakka.WorkerStatus;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MasterActor extends AbstractActor {

    public static Props props() {
        return Props.create(MasterActor.class);
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final Cancellable cleanupTask;

    private final Map<UUID, WorkerState> workers = new HashMap<>();
    private WorkState workState = new WorkState();

    public MasterActor() {
        FiniteDuration workTimeout = Duration.create(10, TimeUnit.SECONDS);

        ClusterClientReceptionist.get(context().system()).registerService(self());
        cleanupTask = context().system().scheduler().schedule(
                Duration.create(5, TimeUnit.SECONDS),
                Duration.create(5, TimeUnit.SECONDS),
                self(),
                new MessageProtocol.CleanupTick(),
                context().dispatcher(),
                self()
        );

        receive(ReceiveBuilder
                        .match(MessageProtocol.CleanupTick.class, message -> {
                            for (Map.Entry<UUID, WorkerState> entry : workers.entrySet()) {
                                UUID worker = entry.getKey();
                                WorkerState workerState = entry.getValue();
                                if (workerState.isBusy() && workerState.isDeadlineOverdue()) {
                                    log.info("Work timed out: {}", workerState.getWorkId());
                                    workers.remove(worker);
                                    // persist(new WorkState.WorkerTimedOut( ...
                                    workState = workState.updated(new WorkState.WorkerTimedOut(workerState.getWorkId()));
                                    notifyWorkers();
                                }
                            }
                        })
                        .match(MessageProtocol.RegisterWorker.class, message -> {
                            if (workers.containsKey(message.worker)) {
                                workers.put(message.worker, workers.get(message.worker).copyWithRef(sender()));
                            } else {
                                log.info("Worker registered: {}", message.worker);
                                workers.put(message.worker, new WorkerState(sender(), new WorkerStatus.Idle()));
                                if (workState.hasWork())
                                    sender().tell(new MessageProtocol.WorkIsReady(), self());
                            }
                        })
                        .match(MessageProtocol.WorkerRequestsWork.class, message -> {
                            if (workState.hasWork()) {
                                WorkerState workerState = workers.get(message.worker);
                                if (workerState != null && workerState.isIdle()) {
                                    MessageProtocol.Work work = workState.nextWork();
                                    // persist(new WorkState.WorkStarted( ...
                                    workState = workState.updated(new WorkState.WorkStarted(work.workId));
                                    log.info("Giving worker {} some work {}", message.worker, work.workId);
                                    workers.put(message.worker, workerState.copyWithStatus(new WorkerStatus.Busy(work.workId, workTimeout.fromNow())));
                                    sender().tell(work, self());
                                }
                            }
                        })
                        .match(MessageProtocol.WorkIsDone.class, message -> {
                            if (workState.isDone(message.workId)) {
                                log.info("Work {} already done, reported as done by worker {}", message.workId, message.worker);
                                sender().tell(new MessageProtocol.Ack(message.workId), self());
                            }
                            else if (workState.isInProgress(message.workId)) {
                                log.info("Work {} done by worker {}", message.workId, message.worker);
                                if (workers.get(message.worker).isBusy()) {
                                    workers.put(message.worker, workers.get(message.worker).copyWithStatus(new WorkerStatus.Idle()));
                                }
                                workState = workState.updated(new WorkState.WorkCompleted(message.workId));
                                // mediator.tell( ...
                                sender().tell(new MessageProtocol.Ack(message.workId), self());
                            }
                            else {
                                log.info("Work {} not in progress, reported as done by worker {}", message.workId, message.worker);
                            }
                        })
                        .match(MessageProtocol.WorkFailed.class, message -> {
                            if (workState.isInProgress(message.workId)) {
                                log.info("Work {} failed by worker {}", message.workId, message.worker);
                                if (workers.get(message.worker).isBusy()) {
                                    workers.put(message.worker, workers.get(message.worker).copyWithStatus(new WorkerStatus.Idle()));
                                }
                                workState = workState.updated(new WorkState.WorkerFailed(message.workId));
                                notifyWorkers();
                            }
                        })
                        .match(MessageProtocol.Work.class, message -> {
                            if (workState.isAccepted(message.workId)) {
                                sender().tell(new MessageProtocol.Ack(message.workId), self());
                            }
                            else {
                                log.info("Accepted work: {}", message.workId);
                                sender().tell(new MessageProtocol.Ack(message.workId), self());
                                workState = workState.updated(new WorkState.WorkAccepted(message));
                                notifyWorkers();
                            }
                        })
                        .build()
        );
    }

    @Override
    public void postStop() throws Exception {
        cleanupTask.cancel();
        super.postStop();
    }

    private void notifyWorkers() {
        if (workState.hasWork()) {
            workers.values().stream()
                    .filter(WorkerState::isIdle)
                    .forEach(workerState -> workerState.worker.tell(new MessageProtocol.WorkIsReady(), self()));
        }
    }
}
