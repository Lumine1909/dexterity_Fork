package me.c7dev.dexterity.integration;

import me.c7dev.dexterity.Dexterity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class IntegrationManager {

    private static final Set<Integration> integrations = new HashSet<>();

    public static void init(Dexterity plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlotSquared")) {
            integrations.add(new Plot2Integration());
        }
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            integrations.add(new WorldGuardIntegration());
        }
    }

    public static boolean canModify(Player player, String world, int x, int y, int z) {
        if (player.hasPermission("dexterity.admin")) {
            return true;
        }
        for (Integration integration : integrations) {
            if (!integration.canModify(player, world, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canModify(Player player, String world, double x, double y, double z) {
        if (player.hasPermission("dexterity.admin")) {
            return true;
        }
        for (Integration integration : integrations) {
            if (!integration.canModify(player, world, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canModify(Player player, String world) {
        if (player.hasPermission("dexterity.admin")) {
            return true;
        }
        for (Integration integration : integrations) {
            if (!integration.canModify(player, world)) {
                return false;
            }
        }
        return true;
    }
}
