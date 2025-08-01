package me.c7dev.dexterity.api.events;

import me.c7dev.dexterity.displays.DexterityDisplay;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.joml.Quaterniond;

public class DisplayRotationEvent extends Event implements Cancellable {

    private boolean cancelled = false;
    private final DexterityDisplay d;
    private final Quaterniond q;

    /**
     * Event called when a selection is rotated, such as with a command or the API
     *
     * @param display
     * @param rotation
     */
    public DisplayRotationEvent(DexterityDisplay display, Quaterniond rotation) {
        d = display;
        q = rotation;
    }

    public DexterityDisplay getDisplay() {
        return d;
    }

    public Quaterniond getRotation() {
        return q;
    }

    public double getRotatedByDeg() {
        return Math.toDegrees(Math.acos(q.w) * 2);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean b) {
        cancelled = b;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    static public HandlerList getHandlerList() {
        return handlers;
    }
}
