package de.stphngrtz.helloakka;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {

    /**
     * http://doc.akka.io/docs/akka/2.4.0/java/cluster-usage.html
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-cluster-java
     */
    public static void main(String[] args) {
        startup("2551");
        startup("2552");
        startup("2553");

        ActorSystem system = ActorSystem.create("ClusterSystem", ConfigFactory.load("stats"));
        system.actorOf(StatsSampleClient.props("/user/statsService"), "client");
    }

    private static void startup(String port) {
        Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory.parseString("akka.cluster.roles = [compute]")
                                .withFallback(ConfigFactory.load("stats"))
                );

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        system.actorOf(StatsWorker.props(), "statsWorker");
        system.actorOf(StatsService.props(), "statsService");
    }
}
