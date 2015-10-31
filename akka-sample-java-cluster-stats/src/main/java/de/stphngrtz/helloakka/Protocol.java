package de.stphngrtz.helloakka;

import java.io.Serializable;

public class Protocol {

    public static class StatsJob implements Serializable {
        public final String text;

        public StatsJob(String text) {
            this.text = text;
        }
    }

    public static class StatsResult implements Serializable {
        public final double meanWordLength;

        public StatsResult(double meanWordLength) {
            this.meanWordLength = meanWordLength;
        }
    }

    public static class JobFailed implements Serializable {
        public final String reason;

        public JobFailed(String reason) {
            this.reason = reason;
        }
    }
}
