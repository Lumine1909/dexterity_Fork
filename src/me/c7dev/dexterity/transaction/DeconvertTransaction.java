package me.c7dev.dexterity.transaction;

import org.bukkit.Material;
import org.bukkit.block.Block;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.DexBlockState;

public class DeconvertTransaction extends RemoveTransaction {
	
	public DeconvertTransaction(DexterityDisplay d) {
		super(d);
		isCommitted = true;
	}
	
	@Override
	public DexterityDisplay undo() {
		DexterityDisplay d = super.undo();
		for (DexBlockState state : states) state.getLocation().getBlock().setType(Material.AIR);
		return d;
	}
	
	@Override
	public void redo() {
		super.redo();
		for (DexBlockState state : states) {
			Block b = state.getLocation().getBlock();
			b.setBlockData(state.getBlock());
		}
	}

}
