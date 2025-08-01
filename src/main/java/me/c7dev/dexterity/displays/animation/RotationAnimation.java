package me.c7dev.dexterity.displays.animation;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.transaction.RotationTransaction;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.QueuedRotation;
import me.c7dev.dexterity.util.RotationPlan;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class RotationAnimation extends Animation {

    private RotationTransaction t;
    private boolean interpolation = true;

    public RotationAnimation(DexterityDisplay display, int ticks, RotationPlan rotation) {
        super(display, ticks);
        RotationPlan r = rotation.clone();
        r.xDeg /= ticks;
        r.yDeg /= ticks;
        r.zDeg /= ticks;
        r.yawDeg /= ticks;
        r.pitchDeg /= ticks;
        r.rollDeg /= ticks;
        r.async = false;

        QueuedRotation rot = display.getRotationManager(true).prepareRotation(r, null);

        super.setRunnable(new BukkitRunnable() {
            @Override
            public void run() {
                if (isPaused()) {
                    return;
                }
                display.getRotationManager().rotate(rot);
                tick();
            }
        });
    }

    public void setBlockInterpolation(boolean b) {
        interpolation = b;
    }

    @Override
    public void beforeStart() {
        t = new RotationTransaction(super.getDisplay());
        if (!interpolation && !super.getDisplay().getPlugin().isLegacy()) {
            for (DexBlock db : super.getDisplay().getBlocks()) {
                db.getEntity().setTeleportDuration(0);
            }
        }
    }

    @Override
    public void beforeFinish() {
        if (!interpolation && !super.getDisplay().getPlugin().isLegacy()) {
            for (DexBlock db : super.getDisplay().getBlocks()) {
                db.getEntity().setTeleportDuration(DexBlock.TELEPORT_DURATION);
            }
        }
    }

    @Override
    public void reset() {
        if (super.isStarted()) {
            super.setPaused(true);
        }
        Location center = super.getDisplay().getCenter();
        t.commit();
        t.undo();
        super.getDisplay().teleport(center);
    }

}
