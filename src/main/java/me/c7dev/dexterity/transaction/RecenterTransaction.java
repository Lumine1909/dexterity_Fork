package me.c7dev.dexterity.transaction;

import me.c7dev.dexterity.displays.DexterityDisplay;
import org.bukkit.Color;
import org.bukkit.Location;

public class RecenterTransaction implements Transaction {

    private final Location old_loc;
    private Location new_loc = null;
    private final DexterityDisplay disp;
    private final boolean isUndone = false;
    private final boolean isCommitted = false;

    public RecenterTransaction(DexterityDisplay d) {
        disp = d;
        old_loc = d.getCenter();
    }

    public void commit(Location loc) {
        if (isCommitted || loc == null) {
            return;
        }
        new_loc = loc.clone();
    }

    public DexterityDisplay undo() {
        disp.setCenter(old_loc);
        disp.getPlugin().api().markerPoint(old_loc, Color.AQUA, 4);
        return null;
    }

    public void redo() {
        disp.setCenter(new_loc);
        disp.getPlugin().api().markerPoint(new_loc, Color.AQUA, 4);
    }

    public boolean isPossible() {
        return true;
    }

    public boolean isUndone() {
        return isUndone;
    }

    public boolean isCommitted() {
        return isCommitted;
    }
}
