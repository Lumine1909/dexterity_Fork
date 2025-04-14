package me.c7dev.dexterity;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.api.events.PlayerClickBlockDisplayEvent;
import me.c7dev.dexterity.api.events.TransactionCompletionEvent;
import me.c7dev.dexterity.api.events.TransactionEvent;
import me.c7dev.dexterity.api.events.TransactionRedoEvent;
import me.c7dev.dexterity.api.events.TransactionUndoEvent;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.displays.animation.Animation;
import me.c7dev.dexterity.displays.animation.RideableAnimation;
import me.c7dev.dexterity.displays.schematics.Schematic;
import me.c7dev.dexterity.transaction.RemoveTransaction;
import me.c7dev.dexterity.util.ClickedBlock;
import me.c7dev.dexterity.util.ClickedBlockDisplay;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.InteractionCommand;

public class EventListeners implements Listener {
	
	private Dexterity plugin;
	private HashMap<UUID, Long> clickDelay = new HashMap<>();
	
	public EventListeners(Dexterity plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public boolean clickDelay(UUID u) {
		int delay = 100;
		if (System.currentTimeMillis() - clickDelay.getOrDefault(u, 0l) < delay) return true;
		final long newdelay = System.currentTimeMillis() + delay;
		clickDelay.put(u, newdelay);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (clickDelay.getOrDefault(u, 0l) == newdelay) clickDelay.remove(u);
			}
		}.runTaskLater(plugin, (int) (delay*0.02));
		return false;
	}
		
	@EventHandler
	public void onBlockClick(PlayerInteractEvent e) {
		
		if (!e.getPlayer().hasPermission("dexterity.click") && !e.getPlayer().hasPermission("dexterity.build")) return;
			
		if (clickDelay(e.getPlayer().getUniqueId())) return;
		boolean rightClick = e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK;

		//calculate if player clicked a block display
		ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
		ClickedBlockDisplay clicked = (DexUtils.isAllowedMaterial(hand.getType()) || !rightClick || hand.getType() == Material.AIR) ? plugin.api().getLookingAt(e.getPlayer()) : null;

		ClickedBlock clickedBlockData = plugin.api().getPhysicalBlockLookingAtRaw(e.getPlayer(), 0.1, clicked == null ? 5 : clicked.getDistance());
		if (clickedBlockData != null && clickedBlockData.getBlock().getType() == Material.AIR) clickedBlockData = null;
		
		boolean clickedBlock = rightClick;
		if (clicked != null) clickedBlock = clickedBlockData != null && clickedBlockData.getBlock().getLocation().distance(e.getPlayer().getEyeLocation()) < clicked.getDistance();
		
		DexSession session = plugin.getEditSession(e.getPlayer().getUniqueId());
		DexterityDisplay clickedDisplay = null;
		DexBlock clickedDB = null;
		boolean holdingWand = hand.getType() == Material.WOODEN_AXE || (hand.getType() == plugin.getWandType() && hand.getItemMeta().getDisplayName().equals(plugin.getConfigString("wand-title", "§fDexterity Wand")));

		if (clicked != null) {
			if (clicked.getBlockDisplay().getMetadata("dex-ignore").size() > 0) return;

			clickedDB = plugin.getMappedDisplay(clicked.getBlockDisplay().getUniqueId());
			if (clickedDB != null) clickedDisplay = clickedDB.getDexterityDisplay();
		}
		
		//normal player or saved display click
		if (clickedDisplay != null && clickedDisplay.isSaved() && (!holdingWand || !e.getPlayer().hasPermission("dexterity.build"))) {
			if (clicked == null || clickedBlock) return;
			//click a display as normal player or with nothing in hand
			RideableAnimation ride = (RideableAnimation) clickedDisplay.getAnimation(RideableAnimation.class);
			e.setCancelled(true);

			//drop display item
			if (clickedDisplay.getDropItem() != null && !rightClick && clickedDisplay.hasOwner(e.getPlayer())) {
				if (e.getPlayer().getGameMode() == GameMode.CREATIVE && e.getPlayer().getInventory().containsAtLeast(clickedDisplay.getDropItem(), 1)) {
					clickedDisplay.remove();
				}
				else clickedDisplay.dropNaturally();
				BlockData bdata = Bukkit.createBlockData(clickedDisplay.getDropItem().getType());
				e.getPlayer().playSound(clickedDisplay.getCenter(), bdata.getSoundGroup().getBreakSound(), 1f, 1f);
			}
			//seat or ride
			else if (ride != null && ride.getMountedPlayer() == null) {
				ride.mount(e.getPlayer());
				Animation anim = (Animation) ride;
				anim.start();
			}

			InteractionCommand[] cmds = clickedDisplay.getCommands();
			if (cmds.length == 0) {
				if ((e.getPlayer().hasPermission("dexterity.buid") || e.getPlayer().hasPermission("dexterity.command.cmd"))
						&& clickedDisplay.hasOwner(e.getPlayer()) && clickedDisplay.getDropItem() == null) {
					session.clickMsg();
				}
			} else for (InteractionCommand cmd : cmds) cmd.exec(e.getPlayer(), rightClick);
			
		}
		else if (e.getPlayer().hasPermission("dexterity.build")) {
			//wand click
			if (holdingWand) {
				e.setCancelled(true);

				//select display with wand
				if (!clickedBlock && clickedDisplay != null && clickedDisplay.getLabel() != null) {
					session.setSelected(clickedDisplay, true);
					return;
				}

				boolean msg = hand.getType() != Material.WOODEN_AXE || e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR;
				if (clicked != null && !clickedBlock) { //click block with wand (set pos1 or pos2)
					boolean is_l1 = !rightClick;
					Vector scale = DexUtils.hadimard(DexUtils.vector(clicked.getBlockDisplay().getTransformation().getScale()), DexUtils.getBlockDimensions(clicked.getBlockDisplay().getBlock()));
					session.setContinuousLocation(clicked.getDisplayCenterLocation(), is_l1, scale, msg);
				} else if (e.getClickedBlock() != null) {
					if (e.getAction() == Action.LEFT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), true, msg); //pos1
					else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) session.setLocation(e.getClickedBlock().getLocation(), false, msg); //pos2
				}
				return;
			} 

			//break or place block display
			else {
				if (clicked == null || clickedBlock) return;
				e.setCancelled(true);

				if (clickedDisplay != null && !clickedDisplay.hasOwner(e.getPlayer())) return;

				//send event
				PlayerClickBlockDisplayEvent clickEvent = new PlayerClickBlockDisplayEvent(e.getPlayer(), clicked, e.getAction(), clickedDisplay);
				Bukkit.getPluginManager().callEvent(clickEvent);
				if (clickEvent.isCancelled()) return;

				//place a block display
				if (rightClick) {
					if (hand.getType() != Material.AIR) {

						BlockData bdata;
						switch(hand.getType()) {
						case NETHER_STAR:
							bdata = Bukkit.createBlockData(Material.NETHER_PORTAL);
							break;
						case FLINT_AND_STEEL:
							bdata = Bukkit.createBlockData(Material.FIRE);
							break;
						default:
							if (hand.getType() == clicked.getBlockDisplay().getBlock().getMaterial()) bdata = clicked.getBlockDisplay().getBlock();
							else {
								try {
									bdata = Bukkit.createBlockData(hand.getType());
								} catch (Exception ex) {
									return;
								}
							}
						}
						
						BlockDisplay b = plugin.putBlock(clicked, bdata);
						
						if (b != null) {
							e.getPlayer().playSound(b.getLocation(), bdata.getSoundGroup().getPlaceSound(), 1f, 1f);
							
							DexBlock new_db = plugin.getMappedDisplay(b.getUniqueId());
							if (clickedDisplay != null && session != null && new_db != null) session.pushBlock(new_db, true);
						}
					}

				} else { //break a block display
					e.getPlayer().playSound(clicked.getBlockDisplay().getLocation(), clicked.getBlockDisplay().getBlock().getSoundGroup().getBreakSound(), 1f, 1f);

					if (clickedDB == null) clicked.getBlockDisplay().remove();
					else {
						if (session != null) session.pushBlock(clickedDB, false);
						clickedDB.remove();
					}
				}
			}
		}
	}
	
	
	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true) //placing display item
	public void onPlace(BlockPlaceEvent e) {
		if (e.isCancelled()) return;
		
		ItemStack hand = e.getItemInHand();
		if (hand == null || hand.getType() == Material.AIR) return;

		ItemMeta meta = hand.getItemMeta();
		NamespacedKey key = new NamespacedKey(plugin, "dex-schem-label");
		String schemName = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if (!plugin.api().checkSchematicExists(schemName)) return;
		
		ItemStack item = hand.clone();
		item.setAmount(1);
		e.setCancelled(true);
		Schematic schem = new Schematic(plugin, schemName);
		
		Vector diff = e.getBlock().getLocation().toVector().subtract(e.getBlockAgainst().getLocation().toVector());
		BoundingBox box = schem.getPlannedBoundingBox();
		Location loc = e.getBlock().getLocation();
		
		if (diff.getY() == 1) { //clicked up face
			Vector againstDims = DexUtils.getBlockDimensions(e.getBlockAgainst().getBlockData());
			if (e.getBlockAgainst().getBlockData() instanceof Slab) {
				Slab slab = (Slab) e.getBlockAgainst().getBlockData();
				if (slab.getType() != Slab.Type.BOTTOM) againstDims = new Vector(1, 1, 1);
			}
			else if (e.getBlockAgainst().getBlockData() instanceof TrapDoor) {
				TrapDoor td = (TrapDoor) e.getBlockAgainst().getBlockData();
				if (td.getHalf() == Bisected.Half.TOP) againstDims = new Vector(1, 1, 1);
			}
			loc.add(0.5, -box.getMinY() + againstDims.getY() - 1, 0.5);
		}
		else if (diff.getY() == -1) loc.add(0.5, 1 - box.getMaxY(), 0.5); //clicked down face
		else if (diff.getX() == 1) loc.add(-box.getMinX(), -box.getMinY(), 0.5); //clicked west face
		else if (diff.getX() == -1) loc.add(1 - box.getMaxX(), -box.getMinY(), 0.5); //clicked east face
		else if (diff.getZ() == 1) loc.add(0.5, -box.getMinY(), -box.getMinZ()); //clicked south face
		else if (diff.getZ() == -1) loc.add(0.5, -box.getMinY(), 1 - box.getMaxZ()); //clicked north face
		
		DexterityDisplay d = schem.paste(loc);
		d.setListed(false);
		d.addOwner(e.getPlayer());
		d.setDropItem(item, schemName);
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.getPlayer().getInventory().removeItem(item);
	}
	
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		DexSession session = plugin.getEditSession(e.getPlayer().getUniqueId());
		if (session == null || !session.isFollowing() || session.getSelected() == null) return;
		if (!session.getSelected().getCenter().getWorld().getName().equals(e.getPlayer().getWorld().getName())) {
			session.cancelEdit();
			session.setSelected(null, false);
			return;
		}
		
		Location loc = e.getPlayer().getLocation();
		if (!e.getPlayer().isSneaking()) loc = DexUtils.blockLoc(loc); //block location
		else loc.add(-0.5, 0, -0.5); //precise location
		
		loc.add(session.getFollowingOffset());
		
		Location center = session.getSelected().getCenter();
		if (loc.getX() == center.getX() && loc.getY() == center.getY() && loc.getZ() == center.getZ()) return;
		
		double cutoff = 0.01; //follow player
		if (Math.abs(e.getTo().getX() - e.getFrom().getX()) > cutoff || Math.abs(e.getTo().getY() - e.getFrom().getY()) > cutoff || Math.abs(e.getTo().getZ() - e.getFrom().getZ()) > cutoff) {
			session.getSelected().teleport(loc);
		}
	}
	
	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (!e.getPlayer().hasPermission("worldedit.selection.pos") || !e.getPlayer().hasPermission("dexterity.command")) return;
		if (e.getMessage().equalsIgnoreCase("//pos1") || e.getMessage().equalsIgnoreCase("//pos2")) {
			DexSession s = plugin.api().getSession(e.getPlayer());
			if (s != null) {
				s.setLocation(e.getPlayer().getLocation(), e.getMessage().equalsIgnoreCase("//pos1"), false);
			}
		}
	}
	
	@EventHandler
	public void onPhysics(BlockPhysicsEvent e) {
		Location loc = e.getBlock().getLocation();
		for (Entry<UUID, DexSession> entry : plugin.editSessionIter()) {
			DexSession session = entry.getValue();
			if (session.isCancellingPhysics() && loc.getWorld().getName().equals(session.getLocation1().getWorld().getName())) {
				if (loc.getX() >= Math.min(session.getLocation1().getX(), session.getLocation2().getX())
						&& loc.getX() <= Math.max(session.getLocation1().getX(), session.getLocation2().getX())
						&& loc.getY() >= Math.min(session.getLocation1().getY(), session.getLocation2().getY())
						&& loc.getY() <= Math.max(session.getLocation1().getY(), session.getLocation2().getY())
						&& loc.getZ() >= Math.min(session.getLocation1().getZ(), session.getLocation2().getZ())
						&& loc.getZ() <= Math.max(session.getLocation1().getZ(), session.getLocation2().getZ())) {
					e.setCancelled(true);
					return;
				}
			}
		}
	}
	
	private void updateAxes(TransactionEvent e) {
		if (e.getSession().isShowingAxes()) {
			if (e.getTransaction() instanceof RemoveTransaction) e.getSession().setShowingAxes(null);
			else e.getSession().updateAxisDisplays();
		}
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		plugin.processUnloadedDisplaysInChunk(e.getChunk());
	}
	
	@EventHandler
	public void onTransactionPush(TransactionCompletionEvent e) {
		updateAxes(e);
	}
	
	@EventHandler
	public void onTransactionUndo(TransactionUndoEvent e) {
		updateAxes(e);
	}
	
	@EventHandler
	public void onTransactionRedo(TransactionRedoEvent e) {
		updateAxes(e);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		UUID u = e.getPlayer().getUniqueId();
		DexSession session = plugin.getEditSession(u);
		if (session != null) {
			session.cancelEdit();
			new BukkitRunnable() {
				@Override
				public void run() {
					Player p = Bukkit.getPlayer(u);
					if (p == null || !p.isOnline()) plugin.deleteEditSession(u);
				}
			}.runTaskLater(plugin, 600l); //TODO make this configurable
		}
	}
}
