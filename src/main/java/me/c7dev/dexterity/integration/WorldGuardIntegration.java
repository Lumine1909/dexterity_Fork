package me.c7dev.dexterity.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WorldGuardIntegration implements Integration {

    @Override
    public boolean canModify(Player player, String world, double x, double y, double z) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        Location loc = new Location(BukkitAdapter.adapt(Bukkit.getWorld(world)), x, y, z);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        return query.testState(loc, localPlayer, Flags.BUILD);
    }
}