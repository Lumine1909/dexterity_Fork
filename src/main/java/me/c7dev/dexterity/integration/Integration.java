package me.c7dev.dexterity.integration;

import org.bukkit.entity.Player;

public interface Integration {

    default boolean canModify(Player player, String world) {
        return canModify(player, world, player.getLocation().x(), player.getLocation().y(), player.getLocation().z());
    }

    default boolean canModify(Player player, String world, int x, int y, int z) {
        return canModify(player, world, (double) x, y, z);
    }

    boolean canModify(Player player, String world, double x, double y, double z);
}