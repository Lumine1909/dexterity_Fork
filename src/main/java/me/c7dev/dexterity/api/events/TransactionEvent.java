package me.c7dev.dexterity.api.events;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.transaction.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TransactionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Transaction t;
    private final DexSession s;

    public TransactionEvent(DexSession session, Transaction transaction) {
        s = session;
        t = transaction;
    }

    static public HandlerList getHandlerList() {
        return handlers;
    }

    public Transaction getTransaction() {
        return t;
    }

    public DexSession getSession() {
        return s;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
