package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SimpleActorTest {

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
    public void simpleTest() throws Exception {
        new JavaTestKit(system){{
            ActorRef simpleActor = system.actorOf(SimpleActor.props());
            JavaTestKit probe = new JavaTestKit(system);

            simpleActor.tell(probe.getRef(), getRef());
            expectMsgEquals(Duration.create(1, TimeUnit.SECONDS), "done");

            new Within(Duration.create(3, TimeUnit.SECONDS)) {

                @Override
                protected void run() {
                    simpleActor.tell("hello", getRef());

                    new AwaitCond() {
                        @Override
                        protected boolean cond() {
                            return probe.msgAvailable();
                        }
                    };

                    expectMsgEquals(Duration.Zero(), "world");
                    probe.expectMsgEquals(Duration.Zero(), "hello");
                    assertEquals(getRef(), probe.getLastSender());

                    expectNoMsg();
                }
            };
        }};
    }
}
