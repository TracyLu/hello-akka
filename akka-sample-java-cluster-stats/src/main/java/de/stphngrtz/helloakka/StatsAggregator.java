package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StatsAggregator extends AbstractLoggingActor {

    public static Props props(int expectedResults, ActorRef replyTo) {
        return Props.create(StatsAggregator.class, expectedResults, replyTo);
    }

    private List<Integer> results = new ArrayList<>();

    @Override
    public void preStart() throws Exception {
        context().setReceiveTimeout(Duration.create(3, TimeUnit.SECONDS));
    }

    public StatsAggregator(int expectedResults, ActorRef replyTo) {
        receive(ReceiveBuilder
                        .match(Integer.class, wordCount -> {
                            results.add(wordCount);
                            if (results.size() == expectedResults) {
                                int sum=0;
                                for (Integer result : results) {
                                    sum += result;
                                }
                                double meanWordLength = ((double) sum) / results.size();
                                replyTo.tell(new Protocol.StatsResult(meanWordLength), self());
                                context().stop(self());
                            }
                        })
                        .match(ReceiveTimeout.class, m -> {
                            replyTo.tell(new Protocol.JobFailed("received timeout"), self());
                            context().stop(self());
                        })
                        .build()
        );
    }
}
