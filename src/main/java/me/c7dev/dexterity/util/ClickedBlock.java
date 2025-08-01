package me.c7dev.dexterity.util;

import org.bukkit.block.Block;

public class ClickedBlock {

    private final Block block;
    private final double dist;

    public ClickedBlock(Block block, double dist) {
        this.block = block;
        this.dist = dist;
    }

    public Block getBlock() {
        return block;
    }

    public double getDistance() {
        return dist;
    }
}
