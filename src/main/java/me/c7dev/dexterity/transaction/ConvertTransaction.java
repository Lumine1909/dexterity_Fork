package me.c7dev.dexterity.transaction;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.SavedBlockState;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class ConvertTransaction implements Transaction {

    private final List<SavedBlockState> blocks = new ArrayList<>();
    private final List<DexBlock> dexblocks = new ArrayList<>();
    private final DexterityDisplay disp = null;
    private boolean isCommitted = false, isUndone = false;

    public ConvertTransaction() {
    }

    public void addBlock(SavedBlockState from, DexBlock to) {
        if (from != null) {
            blocks.add(from);
        }
        dexblocks.add(to);
        isCommitted = true;
    }

    public DexterityDisplay undo() {
        if (!isCommitted || isUndone) {
            return null;
        }
        isUndone = true;
        for (DexBlock db : dexblocks) {
            db.remove();
        }
        dexblocks.clear();
        for (SavedBlockState state : blocks) {
            Block b = state.getLocation().getBlock();
            b.setType(state.getMaterial());
            b.setBlockData(state.getData());
        }
        return null;
    }

    public void redo() {
//		if (!isCommitted || !isUndone) return null;
//		isUndone = false;
//		disp = new DexterityDisplay(plugin);
//		for (SavedBlockState state : blocks) {
//			Block b = state.getLocation().getBlock();
//			DexBlock db = new DexBlock(b, disp);
//			dexblocks.add(db);
//		}
//		return disp;
    }

    public boolean isUndone() {
        return isUndone;
    }

    public boolean isCommitted() {
        return isCommitted;
    }

    public boolean isPossible() {
        return !isCommitted || !isUndone;
    }

}
