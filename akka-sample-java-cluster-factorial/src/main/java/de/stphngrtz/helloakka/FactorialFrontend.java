package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.FromConfig;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class FactorialFrontend extends AbstractLoggingActor {

    public static Props props(int upToN, boolean repeat) {
        return Props.create(FactorialFrontend.class, upToN, repeat);
    }

    private ActorRef factorialBackendRouter = context().actorOf(FromConfig.getInstance().props(), "factorialBackendRouter");
    private final int upToN;

    @Override
    public void preStart() throws Exception {
        sendJobs();
        context().setReceiveTimeout(Duration.create(10, TimeUnit.SECONDS));
    }

    public FactorialFrontend(int upToN, boolean repeat) {
        this.upToN = upToN;

        receive(ReceiveBuilder
                        .match(Protocol.FactorialResult.class, m -> m.n == upToN, m -> {
                            log().info("{}! = {}", m.n, m.factorial);
                            if (repeat)
                                sendJobs();
                            else
                                context().stop(self());
                        })
                        .match(ReceiveTimeout.class, m -> {
                            log().info("Timeout!");
                            sendJobs();
                        })
                        .build()
        );
    }

    private void sendJobs() {
        log().info("starting batch of factorials up to [{}]", upToN);
        for (int i = 0; i <= upToN ; i++) {
            factorialBackendRouter.tell(i, self());
        }
    }
}
