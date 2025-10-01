package me.c7dev.dexterity.displays.animation;

import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.displays.DexterityDisplay;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class Animation {

    private boolean paused = true, stop_req = false;
    private BukkitRunnable runnable, startDelay;
    private final DexterityDisplay display;
    private List<Animation> subseq = new ArrayList<>();
    private final Dexterity plugin;
    private int ticks = 0, delay = 0, tickCount = 0;
    private int freq = 1;

    public Animation(DexterityDisplay display, int ticks) {
        this.display = display;
        this.plugin = display.getPlugin();
        if (ticks < 1) {
            ticks = 1;
        }
        this.ticks = ticks;
    }

    public boolean tick() {
        tickCount++;
        if (tickCount >= ticks) {
            finish();
        }
        return tickCount >= ticks;
    }

    public int getDuration() {
        return ticks;
    }

    public int getStartDelay() {
        return delay;
    }

    public void setStartDelay(int s) {
        delay = s;
    }

    public boolean isStarted() {
        return !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean b) {
        if (paused == b) {
            return;
        }

        try {
            if (b) {
                beforePause();
            } else {
                beforeUnpause();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        paused = b;
        //if (b && !stopped) start();
        if (b) {
            start();
        }
    }

    public void setSubsequentAnimations(List<Animation> a) {
        subseq = a;
    }

    public List<Animation> getSubsequentAnimations() {
        return subseq;
    }

    public void setFrameRate(int l) {
        freq = l;
    }

    public int getFrameRate() {
        return freq;
    }

    public void setRunnable(BukkitRunnable r) {
        runnable = r;
        if (r == null) {
            return;
        }
        runnable.runTaskTimer(plugin, 0, freq);
    }

    public void start() {
        if (isStarted() || stop_req || runnable == null) {
            return;
        }
        startDelay = new BukkitRunnable() {
            @Override
            public void run() {
                delay = 0;
                try {
                    beforeStart();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                paused = false;
            }
        };
        startDelay.runTaskLater(plugin, delay);
    }

    public void stop() {
        if (stop_req) {
            return;
        }

        try {
            beforeStop();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        stop_req = true;
        if (startDelay != null && !startDelay.isCancelled()) {
            startDelay.cancel();
        }
        startDelay = null;
        for (Animation a : subseq) {
            a.stop();
        }
    }

    public void kill() {
        kill(true);
    }

    public void kill(boolean stop_subseq) {
        if (paused && !stop_subseq) {
            return;
        }
        paused = true;
        tickCount = 0;
        if (startDelay != null && !startDelay.isCancelled()) {
            startDelay.cancel();
        }
        startDelay = null;
        if (stop_subseq) {
            for (Animation a : subseq) {
                if (a != this) {
                    a.kill();
                }
            }
        }
    }

    public void finish() {
        try {
            beforeFinish();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!subseq.contains(this) || delay > 0) {
            kill(false);
        }

        if (!stop_req) {
            for (Animation a : subseq) {
                a.start();
            }
        }
    }

    public DexterityDisplay getDisplay() {
        return display;
    }


    //for api to override

    public void beforeStart() {

    }

    public void beforePause() {

    }

    public void beforeUnpause() {

    }

    public void beforeStop() {

    }

    public void beforeFinish() {

    }

    public void reset() {

    }
}
