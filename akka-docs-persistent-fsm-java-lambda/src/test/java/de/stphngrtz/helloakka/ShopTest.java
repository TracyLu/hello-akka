package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.persistence.fsm.PersistentFSM;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * http://doc.akka.io/docs/akka/2.4.0/java/lambda-persistence.html#Persistent_FSM
 * https://github.com/akka/akka/blob/master/akka-persistence/src/test/java/akka/persistence/fsm/AbstractPersistentFSMTest.java
 */
public class ShopTest {

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
    public void fsmFunctionalTest() throws Exception {
        new JavaTestKit(system) {{
            TestProbe dummyReportActor = new TestProbe(system);
            String persistenceId = UUID.randomUUID().toString();

            ActorRef shop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(shop);
            shop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            MyStateData.Item shirt = new MyStateData.Item("1", "Shirt", 59.99f);
            MyStateData.Item shoes = new MyStateData.Item("1", "Shoes", 89.99f);
            MyStateData.Item coat = new MyStateData.Item("1", "Coat", 119.99f);

            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            shop.tell(new Protocol.AddItem(shirt), getRef());
            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            shop.tell(new Protocol.AddItem(shoes), getRef());
            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            shop.tell(new Protocol.AddItem(coat), getRef());
            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            shop.tell(Protocol.Buy.INSTANCE, getRef());
            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            shop.tell(Protocol.Leave.INSTANCE, getRef());

            PersistentFSM.CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.LOOKING_AROUND);

            MyStateData.ShoppingCart shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertTrue(shoppingCart.getItems().isEmpty());

            PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.LOOKING_AROUND, MyState.SHOPPING);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt, shoes);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt, shoes, coat);

            transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.SHOPPING, MyState.PAID);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt, shoes, coat);

            Terminated terminated = expectMsgClass(Terminated.class);
            assertEquals(terminated.getActor(), shop);
        }};
    }

    @Test
    public void fsmTimeoutTest() throws Exception {
        new JavaTestKit(system) {{
            TestProbe dummyReportActor = new TestProbe(system);
            String persistenceId = UUID.randomUUID().toString();

            ActorRef shop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(shop);
            shop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            MyStateData.Item shirt = new MyStateData.Item("1", "Shirt", 59.99f);

            shop.tell(new Protocol.AddItem(shirt), getRef());

            PersistentFSM.CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.LOOKING_AROUND);

            PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.LOOKING_AROUND, MyState.SHOPPING);

            new Within(duration("0.9 seconds"), duration("1.9 seconds")) {
                @Override
                protected void run() {
                    PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
                    assertTransition(transition, shop, MyState.SHOPPING, MyState.INACTIVE);
                }
            };

            new Within(duration("1.9 seconds"), duration("2.9 seconds")) {
                @Override
                protected void run() {
                    expectTerminated(shop);
                }
            };
        }};
    }

    @Test
    public void testSuccessfulRecoveryWithCorrectStateData() throws Exception {
        new JavaTestKit(system) {{
            TestProbe dummyReportActor = new TestProbe(system);
            String persistenceId = UUID.randomUUID().toString();

            ActorRef shop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(shop);
            shop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            MyStateData.Item shirt = new MyStateData.Item("1", "Shirt", 59.99f);
            MyStateData.Item shoes = new MyStateData.Item("1", "Shoes", 89.99f);
            MyStateData.Item coat = new MyStateData.Item("1", "Coat", 119.99f);

            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            shop.tell(new Protocol.AddItem(shirt), getRef());
            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            shop.tell(new Protocol.AddItem(shoes), getRef());
            shop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());

            PersistentFSM.CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.LOOKING_AROUND);

            MyStateData.ShoppingCart shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertTrue(shoppingCart.getItems().isEmpty());

            PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.LOOKING_AROUND, MyState.SHOPPING);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt, shoes);

            shop.tell(PoisonPill.getInstance(), ActorRef.noSender());
            expectTerminated(shop);

            ActorRef recoveredShop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(recoveredShop);
            recoveredShop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            recoveredShop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            recoveredShop.tell(new Protocol.AddItem(coat), getRef());
            recoveredShop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());

            recoveredShop.tell(Protocol.Buy.INSTANCE, getRef());
            recoveredShop.tell(Protocol.GetCurrentCart.INSTANCE, getRef());
            recoveredShop.tell(Protocol.Leave.INSTANCE, getRef());

            currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.SHOPPING);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt, shoes);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt, shoes, coat);

            transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, recoveredShop, MyState.SHOPPING, MyState.PAID);

            shoppingCart = expectMsgClass(MyStateData.ShoppingCart.class);
            assertItems(shoppingCart.getItems(), shirt, shoes, coat);

            expectTerminated(recoveredShop);
        }};
    }

    @Test
    public void testExecutionOfDefinedActionsFollowingSuccessfulPersistence() throws Exception {
        new JavaTestKit(system) {{
            TestProbe dummyReportActor = new TestProbe(system);
            String persistenceId = UUID.randomUUID().toString();

            ActorRef shop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(shop);
            shop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            MyStateData.Item shirt = new MyStateData.Item("1", "Shirt", 59.99f);
            MyStateData.Item shoes = new MyStateData.Item("1", "Shoes", 89.99f);
            MyStateData.Item coat = new MyStateData.Item("1", "Coat", 119.99f);

            shop.tell(new Protocol.AddItem(shirt), getRef());
            shop.tell(new Protocol.AddItem(shoes), getRef());
            shop.tell(new Protocol.AddItem(coat), getRef());
            shop.tell(Protocol.Buy.INSTANCE, getRef());
            shop.tell(Protocol.Leave.INSTANCE, getRef());

            PersistentFSM.CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.LOOKING_AROUND);

            PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.LOOKING_AROUND, MyState.SHOPPING);

            transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.SHOPPING, MyState.PAID);

            Protocol.PurchaseWasMade purchaseWasMade = dummyReportActor.expectMsgClass(Protocol.PurchaseWasMade.class);
            assertItems(purchaseWasMade.items, shirt, shoes, coat);

            expectTerminated(shop);
        }};
    }

    @Test
    public void testExecutionOfDefinedActionsFollowingSuccessfulPersistenceOfFSMStop() throws Exception {
        new JavaTestKit(system) {{
            TestProbe dummyReportActor = new TestProbe(system);
            String persistenceId = UUID.randomUUID().toString();

            ActorRef shop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(shop);
            shop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            MyStateData.Item shirt = new MyStateData.Item("1", "Shirt", 59.99f);
            MyStateData.Item shoes = new MyStateData.Item("1", "Shoes", 89.99f);
            MyStateData.Item coat = new MyStateData.Item("1", "Coat", 119.99f);

            shop.tell(new Protocol.AddItem(shirt), getRef());
            shop.tell(new Protocol.AddItem(shoes), getRef());
            shop.tell(new Protocol.AddItem(coat), getRef());
            shop.tell(Protocol.Leave.INSTANCE, getRef());

            PersistentFSM.CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.LOOKING_AROUND);

            PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.LOOKING_AROUND, MyState.SHOPPING);

            dummyReportActor.expectMsgClass(Protocol.ShoppingCartDiscarded.class);

            expectTerminated(shop);
        }};
    }

    @Test
    public void testCorrectStateTimeoutFollowingRecovery() throws Exception {
        new JavaTestKit(system) {{
            TestProbe dummyReportActor = new TestProbe(system);
            String persistenceId = UUID.randomUUID().toString();

            ActorRef shop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(shop);
            shop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            MyStateData.Item shirt = new MyStateData.Item("1", "Shirt", 59.99f);

            shop.tell(new Protocol.AddItem(shirt), getRef());

            PersistentFSM.CurrentState currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.LOOKING_AROUND);

            PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
            assertTransition(transition, shop, MyState.LOOKING_AROUND, MyState.SHOPPING);

            expectNoMsg(duration("0.6 seconds")); // randomly chosen delay, less than the timeout, before stopping the FSM

            shop.tell(PoisonPill.getInstance(), ActorRef.noSender());
            expectTerminated(shop);

            ActorRef recoveredShop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(recoveredShop);
            recoveredShop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.SHOPPING);

            new Within(duration("0.9 seconds"), duration("1.9 seconds")) {
                @Override
                protected void run() {
                    PersistentFSM.Transition transition = expectMsgClass(PersistentFSM.Transition.class);
                    assertTransition(transition, recoveredShop, MyState.SHOPPING, MyState.INACTIVE);
                }
            };

            expectNoMsg(duration("0.6 seconds")); // randomly chosen delay, less than the timeout, before stopping the FSM

            recoveredShop.tell(PoisonPill.getInstance(), ActorRef.noSender());
            expectTerminated(recoveredShop);

            ActorRef rerecoveredShop = system.actorOf(Shop.props(dummyReportActor.ref(), persistenceId));

            watch(rerecoveredShop);
            rerecoveredShop.tell(new PersistentFSM.SubscribeTransitionCallBack(getRef()), getRef());

            currentState = expectMsgClass(PersistentFSM.CurrentState.class);
            assertEquals(currentState.state(), MyState.INACTIVE);

            new Within(duration("1.9 seconds"), duration("2.9 seconds")) {
                @Override
                protected void run() {
                    expectTerminated(rerecoveredShop);
                }
            };
        }};
    }

    private static void assertTransition(PersistentFSM.Transition transition, ActorRef actorRef, MyState from, MyState to) {
        assertEquals(transition.fsmRef(), actorRef);
        assertEquals(transition.from(), from);
        assertEquals(transition.to(), to);
    }

    private static void assertItems(List<MyStateData.Item> a, MyStateData.Item... bs) {
        for (MyStateData.Item b : bs) {
            if (!a.contains(b))
                fail(b + " not contained in " + a);
        }
    }
}
