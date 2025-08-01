package me.c7dev.dexterity.util;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mask {

    private final List<Material> blocks = new ArrayList<>();
    private boolean negative = false;

    public Mask() {

    }

    public void addMaterialsList(String s) { //split by ,
        for (String section : s.split(",")) {
            try {
                blocks.add(Material.valueOf(section.toUpperCase()));
            } catch (Exception ex) {
                throw new IllegalArgumentException(section);
            }
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        if (negative) {
            s.append("(-) ");
        }
        for (int i = 0; i < blocks.size(); i++) {
            s.append(blocks.get(i).toString().toLowerCase());
            if (i < blocks.size() - 1) {
                s.append(", ");
            }
        }
        return s.toString();
    }

    public Mask(Material... materials) {
        Collections.addAll(blocks, materials);
    }

    public void setNegative(boolean b) {
        negative = b;
    }

    public boolean isNegative() {
        return negative;
    }

    public List<Material> getBlocks() {
        return blocks;
    }

    public boolean isAllowed(Material m) {
        if (blocks.size() == 0) {
            return true;
        }
        boolean included = blocks.contains(m);
        if (negative) {
            included = !included;
        }
        return included;
    }

}
