package me.c7dev.dexterity.displays.schematics;

import me.c7dev.dexterity.util.DexBlockState;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;

/**
 * Holds the blocks list and needed metadata before spawning the display
 */
public class SimpleDisplayState {

    private String label;
    public boolean setscale = false;
    private Vector x = new Vector(1, 0, 0),
        y = new Vector(0, 1, 0),
        z = new Vector(0, 0, 1),
        scale = new Vector(1, 1, 1);
    private ArrayList<DexBlockState> blocks = new ArrayList<>();

    public SimpleDisplayState(String label) {
        this.label = label;
    }

    public SimpleDisplayState(String label, ArrayList<DexBlockState> blocks) {
        this.label = label;
        this.blocks = blocks;
    }

    public String getLabel() {
        return label;
    }

    public void addBlock(DexBlockState s) {
        if (s.getBlock() != null) {
            blocks.add(s);
        }
    }

    public void setLabel(String s) {
        label = s;
    }

    public ArrayList<DexBlockState> getBlocks() {
        return blocks;
    }

    public Vector getRotationX() {
        return x;
    }

    public Vector getRotationY() {
        return y;
    }

    public Vector getRotationZ() {
        return z;
    }

    public Vector getScale() {
        return scale;
    }

    public BoundingBox getBoundingBox() {
        if (blocks.size() == 0) {
            return new BoundingBox();
        }
        double minx = Double.MAX_VALUE, miny = Double.MAX_VALUE, minz = Double.MAX_VALUE;
        double maxx = Double.MIN_VALUE, maxy = Double.MIN_VALUE, maxz = Double.MIN_VALUE;

        for (DexBlockState db : blocks) {
            Vector v = db.getLocation().toVector(), scale = db.getTransformation().getScale().clone().multiply(0.5);
            Vector a = v.clone().add(scale), b = v.clone().subtract(scale);

            minx = Math.min(a.getX(), Math.min(b.getX(), minx));
            miny = Math.min(a.getY(), Math.min(b.getY(), miny));
            minz = Math.min(a.getZ(), Math.min(b.getZ(), minz));

            maxx = Math.max(a.getX(), Math.max(b.getX(), maxx));
            maxy = Math.max(a.getY(), Math.max(b.getY(), maxy));
            maxz = Math.max(a.getZ(), Math.max(b.getZ(), maxz));
        }

        return new BoundingBox(minx, miny, minz, maxx, maxy, maxz);
    }

}
