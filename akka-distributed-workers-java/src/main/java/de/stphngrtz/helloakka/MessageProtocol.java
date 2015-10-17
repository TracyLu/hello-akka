package de.stphngrtz.helloakka;

import java.io.Serializable;
import java.util.UUID;

public abstract class MessageProtocol {

    public static final class CleanupTick implements Serializable {
        @Override
        public String toString() {
            return "CleanupTick";
        }
    }

    public static final class RegisterWorker implements Serializable {
        public final UUID worker;

        public RegisterWorker(UUID worker) {
            this.worker = worker;
        }

        @Override
        public String toString() {
            return "RegisterWorker{worker='" + worker + "'}";
        }
    }

    public static final class WorkerRequestsWork implements Serializable {
        public final UUID worker;

        public WorkerRequestsWork(UUID worker) {
            this.worker = worker;
        }

        @Override
        public String toString() {
            return "WorkerRequestsWork{worker='" + worker + "'}";
        }
    }

    public static final class WorkIsDone implements Serializable {
        public final String workId;
        public final UUID worker;

        public WorkIsDone(String workId, UUID worker) {
            this.workId = workId;
            this.worker = worker;
        }

        @Override
        public String toString() {
            return "WorkIsDone{workId='" + workId + "', worker=" + worker + "}";
        }
    }

    public static final class WorkFailed implements Serializable {
        public final String workId;
        public final UUID worker;

        public WorkFailed(String workId, UUID worker) {
            this.workId = workId;
            this.worker = worker;
        }

        @Override
        public String toString() {
            return "WorkFailed{workId='" + workId + "', worker=" + worker + "}";
        }
    }

    public static final class Work implements Serializable {
        public final String workId;
        public final Object job;

        public Work(String workId, Object job) {
            this.workId = workId;
            this.job = job;
        }

        @Override
        public String toString() {
            return "Work{workId='" + workId + "', job=" + job + "}";
        }
    }

    public static final class WorkIsReady implements Serializable {}

    public static final class Ack implements Serializable {
        public final String workId;

        public Ack(String workId) {
            this.workId = workId;
        }

        @Override
        public String toString() {
            return "Ack{workId='" + workId + "'}";
        }
    }
}
