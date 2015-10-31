package de.stphngrtz.helloakka;

import akka.persistence.fsm.PersistentFSM;

public enum MyState implements PersistentFSM.FSMState {

    LOOKING_AROUND, // LookingAround customer is browsing the site, but hasn't added anything to the shopping cart
    SHOPPING, // Shopping customer has recently added items to the shopping cart
    INACTIVE, // Inactive customer has items in the shopping cart, but hasn't added anything recently
    PAID; // Paid customer has purchased the items

    @Override
    public String identifier() {
        return name();
    }
}
