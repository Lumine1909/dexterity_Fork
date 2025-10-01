package me.c7dev.dexterity.util;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Mask {

    private final Set<Material> blocks = new HashSet<>();
    private boolean negative = false;

    public Mask() {

    }

    public Mask(Material... materials) {
        for (Material m : materials) {
            blocks.add(m);
        }
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
        int i = 0;
        for (Material mat : blocks) {
            s.append(mat.toString().toLowerCase());
            if (i < blocks.size() - 1) {
                s.append(", ");
            }
            i++;
        }
        return s.toString();
    }

    public void setNegative(boolean b) {
        negative = b;
    }

    public boolean isNegative() {
        return negative;
    }

    public List<Material> getBlocks() {
        return new ArrayList<>(blocks);
    }

    public boolean isAllowed(Material m) {
        if (blocks.isEmpty()) {
            return true;
        }
        boolean included = blocks.contains(m);
        if (negative) {
            included = !included;
        }
        return included;
    }

}
