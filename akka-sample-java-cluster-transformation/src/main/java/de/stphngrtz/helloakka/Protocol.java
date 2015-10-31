package de.stphngrtz.helloakka;

import java.io.Serializable;

public class Protocol {

    public static final class TransformationJob implements Serializable {
        public final String text;

        public TransformationJob(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "TransformationJob{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }

    public static final class TransformationResult implements Serializable {
        public final String text;

        public TransformationResult(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return "TransformationResult{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }

    public static final class JobFailed implements Serializable {
        public final String reason;
        public final TransformationJob job;

        public JobFailed(String reason, TransformationJob job) {
            this.reason = reason;
            this.job = job;
        }

        @Override
        public String toString() {
            return "JobFailed{" +
                    "reason='" + reason + '\'' +
                    '}';
        }
    }

    public static final String BackendRegistration = "BackendRegistration";
}
