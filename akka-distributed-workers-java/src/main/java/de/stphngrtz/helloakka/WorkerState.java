package de.stphngrtz.helloakka;

import akka.actor.ActorRef;

public class WorkerState {
    public final ActorRef worker;
    public final WorkerStatus status;

    public WorkerState(ActorRef worker, WorkerStatus status) {
        this.worker = worker;
        this.status = status;
    }

    public WorkerState copyWithRef(ActorRef worker) {
        return new WorkerState(worker, this.status);
    }

    public WorkerState copyWithStatus(WorkerStatus status) {
        return new WorkerState(this.worker, status);
    }

    public boolean isBusy() {
        return status.isBusy();
    }

    public boolean isIdle() {
        return status.isIdle();
    }

    public boolean isDeadlineOverdue() {
        return status.isDeadlineOverdue();
    }

    public String getWorkId() {
        return status.getWorkId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkerState that = (WorkerState) o;

        if (!worker.equals(that.worker)) return false;
        return status.equals(that.status);

    }

    @Override
    public int hashCode() {
        int result = worker.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "WorkerState{worker=" + worker + ", status=" + status + "}";
    }
}
