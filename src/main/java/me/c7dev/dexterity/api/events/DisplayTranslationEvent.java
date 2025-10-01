package me.c7dev.dexterity.api.events;

import me.c7dev.dexterity.displays.DexterityDisplay;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DisplayTranslationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final DexterityDisplay d;
    private final Location from;
    private final Location to;

    /**
     * Event called when a selection is rotated, such as with a command or the API
     *
     * @param display
     * @param from
     * @param to
     */
    public DisplayTranslationEvent(DexterityDisplay display, Location from, Location to) {
        this.from = from.clone();
        this.to = to.clone();
        d = display;
    }

    static public HandlerList getHandlerList() {
        return handlers;
    }

    public DexterityDisplay getDisplay() {
        return d;
    }

    public Location getFrom() {
        return from.clone();
    }

    public Location getTo() {
        return to.clone();
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
