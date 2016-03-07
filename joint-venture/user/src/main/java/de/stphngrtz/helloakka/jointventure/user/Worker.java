package de.stphngrtz.helloakka.jointventure.user;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.mongodb.client.MongoDatabase;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Worker extends AbstractFSM<Worker.State, Worker.Data<? extends Event>> {

    public enum State {
        IDLE,
        WORKING,
        WAITING
    }

    public static class Data<T extends de.stphngrtz.helloakka.jointventure.user.Event> {
        public final Optional<T> event;

        public Data() {
            this.event = Optional.empty();
        }

        public Data(T event) {
            this.event = Optional.of(event);
        }
    }

    /**
     * Commands, Events or Messages to which the Worker reacts
     */
    public static class Protocol {

        public static class CallForWork {
        }

        public static class Work<T extends de.stphngrtz.helloakka.jointventure.user.Event> {
            public final T event;

            public Work(T event) {
                this.event = event;
            }
        }

        public static class WorkIsDone<T extends de.stphngrtz.helloakka.jointventure.user.Event> {
            public final T event;

            public WorkIsDone(T event) {
                this.event = event;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                WorkIsDone<?> that = (WorkIsDone<?>) o;
                return Objects.equals(event, that.event);
            }

            @Override
            public int hashCode() {
                return Objects.hash(event);
            }
        }

        public static class Acknowledged<T extends de.stphngrtz.helloakka.jointventure.user.Event> {
            public final T event;

            public Acknowledged(T event) {
                this.event = event;
            }
        }
    }

    public static Props props(ActorRef processor, MongoDatabase db, FiniteDuration intervalForRegistration, FiniteDuration timeoutForAcknowledgement) {
        return Props.create(Worker.class, processor, db, intervalForRegistration, timeoutForAcknowledgement);
    }

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private final Cancellable registrationTask;

    @SuppressWarnings("unchecked")
    public Worker(ActorRef processor, MongoDatabase db, FiniteDuration intervalForRegistration, FiniteDuration timeoutForAcknowledgement) {
        UUID workerId = UUID.randomUUID();
        ActorRef workExecutor = context().watch(context().actorOf(WorkExecutor.props(db)));

        registrationTask = context().system().scheduler().schedule(
                Duration.Zero(),
                intervalForRegistration,
                processor,
                new Processor.Protocol.RegisterWorker(workerId),
                context().dispatcher(),
                self()
        );

        startWith(State.IDLE, new Data<>());

        when(State.IDLE, matchEvent(Void.class, (command, data) -> stay())
                .event(Protocol.CallForWork.class, (command, data) -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    processor.tell(new Processor.Protocol.CallingForWork(workerId), self());
                    return stay();
                })
                .event(Protocol.Work.class, (command, data) -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    workExecutor.tell(new WorkExecutor.Protocol.Work<>(command.event), self());
                    return goTo(State.WORKING).using(new Data<>(command.event));
                })
                .event(Terminated.class, (terminated, data) -> Objects.equals(terminated.actor(), workExecutor), (terminated, data) -> {
                    context().stop(self());
                    return stay();
                })
        );

        when(State.WORKING, matchEvent(Void.class, (command, data) -> stay())
                .event(Protocol.WorkIsDone.class, (command, data) -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    processor.tell(new Processor.Protocol.WorkIsDone<>(workerId, command.event), self());
                    return goTo(State.WAITING).using(new Data<>(command.event)).forMax(timeoutForAcknowledgement);
                })
                .event(Terminated.class, (terminated, data) -> Objects.equals(terminated.actor(), workExecutor), (terminated, data) -> {
                    log.debug("WorkExecutor terminated, terminating Worker!");
                    context().stop(self());
                    return stay();
                })
        );

        when(State.WAITING, matchEvent(Void.class, (command, data) -> stay())
                .event(Protocol.Acknowledged.class, (command, data) -> Objects.equals(command.event, data.event.orElse(null)), (command, data) -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    processor.tell(new Processor.Protocol.CallingForWork(workerId), self());
                    return goTo(State.IDLE);
                })
                .eventEquals(StateTimeout(), (timeout, data) -> {
                    log.debug("Processor timed out, retrying...");
                    processor.tell(new Processor.Protocol.WorkIsDone<>(workerId, data.event.get()), self());
                    return goTo(State.WAITING).forMax(timeoutForAcknowledgement);
                })
        );

        initialize();
        log.debug("Worker initialized (id:{})", workerId);
    }

    // TODO work timeout einbauen. wenn der workexecutor seine arbeit nicht innerhalb von X bearbeitet hat, dann timeout (supervisorstrategy?)

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(-1, Duration.Inf(), DeciderBuilder
                .match(ActorInitializationException.class, e -> SupervisorStrategy.stop())
                .match(DeathPactException.class, e -> SupervisorStrategy.stop())
                .match(Exception.class, e -> {
                    // TODO wenn es aktuell arbeit gibt, dann sag dem processor bescheid, dass diese fehlgeschlagen ist!
                    return SupervisorStrategy.restart();
                })
                .matchAny(e -> SupervisorStrategy.escalate())
                .build()
        );
    }

    @Override
    public void postStop() {
        registrationTask.cancel();
        super.postStop();
    }
}
