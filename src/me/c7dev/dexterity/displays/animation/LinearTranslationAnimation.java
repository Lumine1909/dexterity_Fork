package me.c7dev.dexterity.displays.animation;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.displays.DexterityDisplay;

public class LinearTranslationAnimation extends Animation {
	
	private Location startLoc, endLoc;
			
	public LinearTranslationAnimation(DexterityDisplay display, int ticks, Location end_loc) {
		super(display, ticks);
		
		startLoc = display.getCenter();
		this.endLoc = end_loc;
		Vector displacement = end_loc.toVector().subtract(startLoc.toVector());
				
		super.setRunnable(new BukkitRunnable() {
			Vector dtick = displacement.clone().multiply(1.0/ticks);
						
			@Override
			public void run() {
				if (isPaused()) return;
				display.teleport(dtick);
				tick();
			}
		});
	}
	
	public Location getStartLocation() {
		return startLoc;
	}
	
	public Location getEndLocation() {
		return endLoc;
	}
	
	@Override
	public void reset() {
		if (super.isStarted()) super.setPaused(true);
		getDisplay().teleport(startLoc);
	}
}
