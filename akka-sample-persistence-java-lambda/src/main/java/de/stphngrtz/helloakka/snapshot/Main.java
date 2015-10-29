package de.stphngrtz.helloakka.snapshot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class Main {

    /**
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-persistence-java-lambda
     */
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("example");
        ActorRef snapshotActor = system.actorOf(MySnapshotActor.props(), "my-snapshot-actor");

        snapshotActor.tell("a", ActorRef.noSender());
        snapshotActor.tell("b", ActorRef.noSender());
        snapshotActor.tell("snap", ActorRef.noSender());
        snapshotActor.tell("c", ActorRef.noSender());
        snapshotActor.tell("d", ActorRef.noSender());
        snapshotActor.tell("print", ActorRef.noSender());

        Thread.sleep(1000);
        system.terminate();

        // 1st: current state: [a, b, c, d]
        // 2nd: offered snapshot:SnapshotOffer(SnapshotMetadata(sample-id-3,2,1446058088145),[a, b])
        //      current state: [a, b, c, d, a, b, c, d]
    }
}
