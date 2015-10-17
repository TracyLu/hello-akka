package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientSettings;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.stphngrtz.helloakka.actor.*;

public class Main {

    /**
     * https://github.com/typesafehub/activator-akka-distributed-workers-java
     */
    public static void main(String[] args) {
        startMaster(2551);
        startMaster(2552);

        startWorker(1551);

        startFrontend(3551);
    }

    private static void startMaster(int port) {
        Config config = ConfigFactory
                .parseString("akka.cluster.roles=[master]")
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
                .withFallback(ConfigFactory.load());

        ActorSystem system = ActorSystem.create("ClusterSystem", config);

        // startupSharedJournal (akka-persistence)

        system.actorOf(ClusterSingletonManager.props(
                MasterActor.props(),
                PoisonPill.getInstance(),
                ClusterSingletonManagerSettings.create(system).withRole("master")
        ), "master");
    }

    private static void startWorker(int port) {
        Config config = ConfigFactory
                .parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory.load("worker"));

        ActorSystem system = ActorSystem.create("WorkerSystem", config);


        ActorRef clusterClient = system.actorOf(ClusterClient.props(ClusterClientSettings.create(system)), "clusterClient");
        system.actorOf(WorkerActor.props(clusterClient, WorkExecutorActor.props()), "worker");
    }

    private static void startFrontend(int port) {
        Config config = ConfigFactory
                .parseString("akka.cluster.roles=[frontend]")
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
                .withFallback(ConfigFactory.load());

        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        ActorRef frontend = system.actorOf(FrontendActor.props(), "frontend");
        system.actorOf(WorkProducerActor.props(frontend), "producer");
        system.actorOf(WorkResultConsumerActor.props(frontend), "consumer");
    }
}
