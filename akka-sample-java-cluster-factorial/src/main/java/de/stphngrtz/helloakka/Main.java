package de.stphngrtz.helloakka;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Main {

    /**
     * http://doc.akka.io/docs/akka/2.4.0/java/cluster-usage.html
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-cluster-java
     */
    public static void main(String[] args) {
        startupBackend("2551");
        startupBackend("2552");
        startupBackend("2553");
        startupFrontend("2554");
    }

    private static void startupBackend(String port) {
        Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]")
                        .withFallback(ConfigFactory.load("factorial")));

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        system.actorOf(FactorialBackend.props(), "factorialBackend");
        system.actorOf(MetricsListener.props(), "metricsListener");
    }

    private static void startupFrontend(String port) {
        Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")
                        .withFallback(ConfigFactory.load("factorial")));

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        system.log().info("Factorials will start when 2 backend members are in the cluster.");

        Cluster.get(system).registerOnMemberUp(() -> {
            system.actorOf(FactorialFrontend.props(200, false), "factorialFrontend");
        });
        Cluster.get(system).registerOnMemberRemoved(() -> {
            system.registerOnTermination(() -> System.exit(0));
            system.terminate();

            // In case ActorSystem shutdown takes longer than 10 seconds, exit the JVM forcefully anyway.
            // We must spawn a separate thread to not block current thread, since that would have blocked the shutdown
            // of the ActorSystem.
            new Thread() {
                @Override
                public void run() {
                    try {
                        Await.ready(system.whenTerminated(), Duration.create(10, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        System.exit(-1);
                    }
                }
            }.start();
        });
    }
}
