package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;

/**
 * http://doc.akka.io/docs/akka/2.4.0/java/lambda-fsm.html
 */
public class BuncherTest {

    private static ActorSystem system;

    @BeforeClass
    public static void before() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void after() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testBuncherActorBatchesCorrectly() throws Exception {
        new JavaTestKit(system) {{
            ActorRef buncher = system.actorOf(Buncher.props());

            buncher.tell(new Protocol.SetTarget(getRef()), getRef());
            buncher.tell(new Protocol.Queue(42), getRef());
            buncher.tell(new Protocol.Queue(43), getRef());

            LinkedList<Object> l1 = new LinkedList<>();
            l1.add(42);
            l1.add(43);
            expectMsgEquals(new Protocol.Batch(l1));

            buncher.tell(new Protocol.Queue(44), getRef());
            buncher.tell(Protocol.Flush.Flush, getRef());
            buncher.tell(new Protocol.Queue(45), getRef());

            LinkedList<Object> l2 = new LinkedList<>();
            l2.add(44);
            expectMsgEquals(new Protocol.Batch(l2));

            LinkedList<Object> l3 = new LinkedList<>();
            l3.add(45);
            expectMsgEquals(new Protocol.Batch(l3));

            system.stop(buncher);
        }};
    }

    @Test
    public void testBuncherActorDoesntBatchUninitialized() throws Exception {
        new JavaTestKit(system) {{
            ActorRef buncher = system.actorOf(Buncher.props());

            buncher.tell(new Protocol.Queue(42), getRef());
            expectNoMsg();

            system.stop(buncher);
        }};
    }
}
