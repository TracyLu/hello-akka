package de.stphngrtz.helloakka.jointventure.user;

import akka.actor.ActorRef;
import scala.concurrent.duration.Deadline;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;

public class WorkerState {

    private final ActorRef worker;
    private final Optional<Event> event;
    private final Optional<Deadline> deadline;

    public WorkerState(ActorRef worker, Optional<Event> event, Optional<Deadline> deadline) {
        this.worker = worker;
        this.event = event;
        this.deadline = deadline;
    }

    public boolean isIdle() {
        return !isBusy();
    }

    public boolean isBusy() {
        return event.isPresent();
    }

    public boolean isDeadlineOverdue() {
        return deadline.isPresent() && deadline.get().isOverdue();
    }

    public ActorRef getWorker() {
        return worker;
    }

    public Optional<Event> getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return isIdle() ? "IDLE" : String.format("BUSY with %s, %s left", event.get(), deadline.get().timeLeft());
    }

    public static WorkerState busy(ActorRef worker, Event event, FiniteDuration timeout) {
        return new WorkerState(worker, Optional.of(event), Optional.of(Deadline.apply(timeout)));
    }

    public static WorkerState idle(ActorRef worker) {
        return new WorkerState(worker, Optional.empty(), Optional.empty());
    }
}
