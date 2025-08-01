package me.c7dev.dexterity.integration;

import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import org.bukkit.entity.Player;

public class Plot2Integration implements Integration {

    @Override
    public boolean canModify(Player player, String world, double x, double y, double z) {
        Location plotLoc = Location.at(world, (int) x, (int) y, (int) z);

        Plot plot = plotLoc.getPlot();
        if (plot == null) {
            return false;
        }

        return plot.getOwners().contains(player.getUniqueId()) ||
            plot.getTrusted().contains(player.getUniqueId());
    }
}
