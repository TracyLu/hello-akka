package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.ConsistentHashingRouter;
import akka.routing.FromConfig;

public class StatsService extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(StatsService.class);
    }

    // This router is used both with lookup and deploy of routees. If you
    // have a router with only lookup of routees you can use Props.empty()
    // instead of Props.create(StatsWorker.class).
    private ActorRef workerRouter = context().actorOf(FromConfig.getInstance().props(StatsWorker.props()), "workerRouter");

    public StatsService() {
        receive(ReceiveBuilder
                        .match(Protocol.StatsJob.class, m -> m.text.length() > 0, m -> {
                            String[] words = m.text.split(" ");

                            // create actor that collects replies from workers
                            ActorRef aggregator = context().actorOf(StatsAggregator.props(words.length, sender()));

                            // send each word to a worker
                            for (String word : words) {
                                workerRouter.tell(new ConsistentHashingRouter.ConsistentHashableEnvelope(word, word), aggregator);
                            }
                        })
                        .build()
        );
    }
}
