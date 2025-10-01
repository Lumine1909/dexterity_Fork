package me.c7dev.dexterity.api.events;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.displays.DexterityDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SessionSelectionChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final DexterityDisplay from;
    private final DexterityDisplay to;
    private final DexSession session;
    private boolean cancelled = false;

    public SessionSelectionChangeEvent(DexSession session, DexterityDisplay from, DexterityDisplay to) {
        this.from = from;
        this.to = to;
        this.session = session;
    }

    static public HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Returns the DexterityDisplay that the player is about to un-select, or null
     *
     * @return
     */
    public DexterityDisplay getFromSelectedDisplay() {
        return from;
    }

    /**
     * Returns the DexterityDisplay that the player is about to select, or null
     *
     * @return
     */
    public DexterityDisplay getToSelectedDisplay() {
        return to;
    }

    public DexSession getSession() {
        return session;
    }

    public Player getPlayer() {
        return session.getPlayer();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean b) {
        cancelled = b;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
