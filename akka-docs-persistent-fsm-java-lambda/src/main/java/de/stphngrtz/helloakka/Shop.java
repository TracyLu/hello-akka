package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.persistence.fsm.AbstractPersistentFSM;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Shop extends AbstractPersistentFSM<MyState, MyStateData.ShoppingCart, MyEvent.DomainEvent> {

    public static Props props(ActorRef reportActor, String persistenceId) {
        return Props.create(Shop.class, reportActor, persistenceId);
    }

    private final String persistenceId;

    public Shop(ActorRef reportActor, String persistenceId) {
        this.persistenceId = persistenceId;

        startWith(MyState.LOOKING_AROUND, new MyStateData.ShoppingCart());

        when(MyState.LOOKING_AROUND,
                matchEvent(Protocol.AddItem.class,
                        (event, data) -> goTo(MyState.SHOPPING).applying(new MyEvent.ItemAdded(event.item)).forMax(Duration.create(1, TimeUnit.SECONDS))
                ).event(Protocol.GetCurrentCart.class,
                        (event, data) -> stay().replying(data)
                )
        );

        when(MyState.SHOPPING,
                matchEvent(Protocol.AddItem.class,
                        (event, data) -> stay().applying(new MyEvent.ItemAdded(event.item)).forMax(Duration.create(1, TimeUnit.SECONDS))
                ).event(Protocol.Buy.class,
                        (event, data) -> goTo(MyState.PAID).applying(MyEvent.OrderExecuted.INSTANCE).andThen(exec(cart -> reportActor.tell(new Protocol.PurchaseWasMade(cart.getItems()), self())))
                ).event(Protocol.Leave.class,
                        (event, data) -> stop().applying(MyEvent.OrderDiscarded.INSTANCE).andThen(exec(cart -> reportActor.tell(Protocol.ShoppingCartDiscarded.INSTANCE, self())))
                ).event(Protocol.GetCurrentCart.class,
                        (event, data) -> stay().replying(data)
                ).event(StateTimeout$.class,
                        (event, data) -> goTo(MyState.INACTIVE).forMax(Duration.create(2, TimeUnit.SECONDS))
                )
        );

        when(MyState.PAID,
                matchEvent(Protocol.Leave.class,
                        (event, data) -> stop()
                ).event(Protocol.GetCurrentCart.class,
                        (event, data) -> stay().replying(data)
                )
        );

        when(MyState.INACTIVE,
                matchEvent(Protocol.AddItem.class,
                        (event, data) -> goTo(MyState.SHOPPING).applying(new MyEvent.ItemAdded(event.item)).forMax(Duration.create(1, TimeUnit.SECONDS))
                ).event(Protocol.GetCurrentCart.class,
                        (event, data) -> stay().replying(data)
                ).event(StateTimeout$.class,
                        (event, data) -> stop().applying(MyEvent.OrderDiscarded.INSTANCE).andThen(exec(cart -> reportActor.tell(Protocol.ShoppingCartDiscarded.INSTANCE, self())))
                )
        );

        initialize();
    }

    @Override
    public Class<MyEvent.DomainEvent> domainEventClass() {
        return MyEvent.DomainEvent.class;
    }

    @Override
    public MyStateData.ShoppingCart applyEvent(MyEvent.DomainEvent domainEvent, MyStateData.ShoppingCart shoppingCart) {
        if (domainEvent instanceof MyEvent.ItemAdded) {
            shoppingCart.addItem(((MyEvent.ItemAdded) domainEvent).item);
            return shoppingCart;
        }
        else if (domainEvent instanceof MyEvent.OrderExecuted) {
            return shoppingCart;
        }
        else if (domainEvent instanceof MyEvent.OrderDiscarded) {
            shoppingCart.clear();
            return shoppingCart;
        }

        throw new RuntimeException("unhandled event: " +  domainEvent);
    }

    @Override
    public String persistenceId() {
        return persistenceId;
    }
}
