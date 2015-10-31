package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.sun.javaws.exceptions.InvalidArgumentException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    /**
     * http://doc.akka.io/docs/akka/2.4.0/java/cluster-usage.html
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-cluster-java
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            startupBackend("2551");
            startupBackend("2552");
            startupBackend("2553");
            startupFrontend("2554");
        }
        else {
            if (args.length == 2) {
                if (args[0] == "frontend")
                    startupFrontend(args[1]);
                else if (args[0] == "backend")
                    startupBackend(args[1]);
                else {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    private static void startupBackend(String port) {
        Config config = ConfigFactory
                .parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory
                                .parseString("akka.cluster.roles = [backend]")
                                .withFallback(ConfigFactory.load())
                );

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        system.actorOf(TransformationBackend.props(), "backend");
    }

    private static void startupFrontend(String port) {
        Config config = ConfigFactory
                .parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory
                                .parseString("akka.cluster.roles = [frontend]")
                                .withFallback(ConfigFactory.load())
                );

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        ActorRef frontend = system.actorOf(TransformationBackend.props(), "frontend");

        AtomicInteger counter = new AtomicInteger(0);

        system.scheduler().schedule(Duration.create(2, TimeUnit.SECONDS), Duration.create(2, TimeUnit.SECONDS), () -> {
            Patterns.ask(frontend, new Protocol.TransformationJob("hello-" + counter.incrementAndGet()), new Timeout(5, TimeUnit.SECONDS))
                    .onSuccess(new OnSuccess<Object>() {
                        @Override
                        public void onSuccess(Object result) throws Throwable {
                            System.out.println(result);
                        }
                    }, system.dispatcher());
        }, system.dispatcher());
    }
}
