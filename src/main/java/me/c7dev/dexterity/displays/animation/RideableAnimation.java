package me.c7dev.dexterity.displays.animation;

import org.bukkit.entity.Player;

public interface RideableAnimation {

    Player getMountedPlayer();

    boolean mount(Player p);

    void dismount();
}
