package de.stphngrtz.helloakka;

import scala.concurrent.duration.Deadline;

public abstract class WorkerStatus {

    public abstract boolean isBusy();

    public abstract boolean isDeadlineOverdue();

    public abstract String getWorkId();

    public boolean isIdle() {
        return !isBusy();
    }

    public static final class Idle extends WorkerStatus {

        @Override
        public boolean isBusy() {
            return false;
        }

        @Override
        public boolean isDeadlineOverdue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getWorkId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "Idle";
        }
    }

    public static final class Busy extends WorkerStatus {

        private final String workId;
        private final Deadline deadline;

        public Busy(String workId, Deadline deadline) {
            this.workId = workId;
            this.deadline = deadline;
        }

        @Override
        public boolean isBusy() {
            return true;
        }

        @Override
        public boolean isDeadlineOverdue() {
            return deadline.isOverdue();
        }

        @Override
        public String getWorkId() {
            return workId;
        }
    }
}
