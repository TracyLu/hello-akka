package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class StatsWorker extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(StatsWorker.class);
    }

    private final Map<String, Integer> cache = new HashMap<>();

    public StatsWorker() {
        receive(ReceiveBuilder
                        .match(String.class, word -> {
                            Integer length = cache.get(word);
                            if (length == null) {
                                length = word.length();
                                cache.put(word, length);
                            }
                            sender().tell(length, self());
                        })
                        .build()
        );
    }
}
