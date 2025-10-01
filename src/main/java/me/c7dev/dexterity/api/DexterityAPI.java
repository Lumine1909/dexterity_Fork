package me.c7dev.dexterity.api;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.command.CommandContext;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.integration.IntegrationManager;
import me.c7dev.dexterity.transaction.ConvertTransaction;
import me.c7dev.dexterity.util.ClickedBlock;
import me.c7dev.dexterity.util.ClickedBlockDisplay;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexBlockState;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.Mask;
import me.c7dev.dexterity.util.OrientationKey;
import me.c7dev.dexterity.util.RollOffset;
import me.c7dev.dexterity.util.SavedBlockState;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DexterityAPI {

    private final Dexterity plugin;
    private final HashMap<UUID, Integer> pidMap = new HashMap<>();
    private final List<UUID> markerPoints = new ArrayList<>();
    private int pid_ = Integer.MIN_VALUE + 1; //min val reserved for getOrDefault
    private final HashMap<OrientationKey, RollOffset> rollOffsets = new HashMap<>();
    private final HashMap<OrientationKey, Vector[]> axes = new HashMap<>();
    private final Vector[][] basisVecsNoRot = getBasisVecs(new Matrix3d(1, 0, 0, 0, 1, 0, 0, 0, 1));
    private final Vector eastUnit = new Vector(1, 0, 0), upUnit = new Vector(0, 1, 0), southUnit = new Vector(0, 0, 1);

    private final BlockFace[] faces = {
        BlockFace.UP,
        BlockFace.DOWN,
        BlockFace.SOUTH,
        BlockFace.NORTH,
        BlockFace.EAST,
        BlockFace.WEST
    };

    private int getNewPID() {
        int p = pid_;
        if (pid_ == Integer.MAX_VALUE) {
            pid_ = Integer.MIN_VALUE + 1;
        } else {
            pid_++;
        }
        return p;
    }

    public DexterityAPI(Dexterity plugin) {
        this.plugin = plugin;

        long msDelete = 30 * 1000;

        new BukkitRunnable() {
            @Override
            public void run() {
                List<OrientationKey> toRemove = new ArrayList<>();
                long time = System.currentTimeMillis();

                for (OrientationKey key : rollOffsets.keySet()) {
                    if (time - key.getLastUsedTime() >= msDelete) {
                        toRemove.add(key);
                    }
                }
                for (OrientationKey key : toRemove) {
                    rollOffsets.remove(key);
                }
                toRemove.clear();

                for (OrientationKey key : axes.keySet()) {
                    if (time - key.getLastUsedTime() >= msDelete) {
                        toRemove.add(key);
                    }
                }
                for (OrientationKey key : toRemove) {
                    axes.remove(key);
                }
            }
        }.runTaskTimer(plugin, 0, msDelete * 20L / 1000);
    }

    /**
     * Returns an instance of the plugin main class
     *
     * @return
     */
    public Dexterity getPlugin() {
        return plugin;
    }

    public static DexterityAPI getInstance() {
        return Dexterity.getPlugin(Dexterity.class).api();
    }

    /**
     * Returns all names of displays that are saved across all worlds
     *
     * @return Unmodifiable set of label strings
     */
    public Set<String> getDisplayLabels() {
        return plugin.getDisplayLabels();
    }

    /**
     * Returns all names of displays that are saved and owned by the player across all worlds
     *
     * @param p
     * @return
     */
    public Set<String> getDisplayLabels(Player p) {
        return plugin.getDisplayLabels(p);
    }

    /**
     * Returns all displays that have been saved across all worlds
     *
     * @return Unmodifiable collection of {@link DexterityDisplay} that have been saved
     */
    public Collection<DexterityDisplay> getDisplays() {
        return plugin.getDisplays();
    }

    /**
     * Retrieves a saved {@link DexterityDisplay} in any world by its raw label name
     *
     * @param label Label of the saved display
     * @return The saved display if it exists, otherwise null
     */
    public DexterityDisplay getDisplay(String label) {
        return plugin.getDisplay(label);
    }

    /**
     * Retrieves the specified player's edit session if it exists
     *
     * @param p The online player whose session is to be retrieved
     * @return The player's session if it exists, otherwise null
     */
    public DexSession getSession(Player p) {
        return plugin.getEditSession(p.getUniqueId());
    }

    /**
     * Retrieves the edit session for the player with the specified UUID.
     * Can be used if the player is offline but the session has not been automatically deleted yet.
     *
     * @param u The UUID of the player whose session is to be retrieved
     * @return The edit session of the player with the specified UUID if it exists, otherwise null
     */
    public DexSession getSession(UUID u) {
        return plugin.getEditSession(u);
    }

    /**
     * Converts all of the blocks inside the cuboid defined by the two locations into Block Displays
     *
     * @param l1    First location
     * @param l2    Second location
     * @param t     Transaction to add blocks
     * @param limit Maximum number to convert for safety
     * @return A {@link DexterityDisplay} selection of the newly created block displays
     */
    public DexterityDisplay convertBlocks(Location l1, Location l2, ConvertTransaction t, int limit, CommandContext ct) { //l1 and l2 bounding box, all blocks inside converted
        if (!l1.getWorld().getName().equals(l2.getWorld().getName())) {
            throw new IllegalArgumentException("Locations must be in the same world!");
        }

        int xmin = Math.min(l1.getBlockX(), l2.getBlockX()), xmax = Math.max(l1.getBlockX(), l2.getBlockX());
        int ymin = Math.min(l1.getBlockY(), l2.getBlockY()), ymax = Math.max(l1.getBlockY(), l2.getBlockY());
        int zmin = Math.min(l1.getBlockZ(), l2.getBlockZ()), zmax = Math.max(l1.getBlockZ(), l2.getBlockZ());

        if ((xmax - xmin) * (ymax - ymin) * (zmax - zmin) > plugin.getMaxVolume()) {
            Bukkit.getLogger().warning("Failed to create a display because it exceeds the maximum volume in config!");
            return null;
        }

        DexterityDisplay d = new DexterityDisplay(plugin);

        for (int x = xmin; x <= xmax; x++) {
            for (int y = ymin; y <= ymax; y++) {
                for (int z = zmin; z <= zmax; z++) {
                    Block b = new Location(l1.getWorld(), x, y, z).getBlock();
                    Location loc = b.getLocation();
                    if (!IntegrationManager.canModify(
                        ct.getPlayer(), ct.getPlayer().getWorld().getName(),
                        loc.x(), loc.y(), loc.z()
                    )) {
                        continue;
                    }
                    if (DexUtils.isAllowedMaterial(b.getType())) {
                        SavedBlockState saved = null;
                        if (t != null) {
                            saved = new SavedBlockState(b);
                        }

                        DexBlock db = new DexBlock(b, d);
                        d.addBlock(db);
                        if (t != null) {
                            t.addBlock(saved, db);
                        }
                    } else if (b.getType() != Material.AIR && b.getType() != Material.BARRIER && b.getType() != Material.LIGHT) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }

        d.recalculateCenter();
        return d;
    }

    /**
     * Returns true if a schematic with this name exists in the schematics folder
     *
     * @param name
     * @return
     */
    public boolean checkSchematicExists(String name) {
        if (name == null) {
            return false;
        }
        if (!name.endsWith(".dexterity")) {
            name += ".dexterity";
        }
        File f = new File(plugin.getDataFolder().getAbsolutePath() + "/schematics/" + name);
        return f.exists();
    }

    private Vector[][] getBasisVecs(Matrix3d mat) {
        Vector a = new Vector(mat.m00, mat.m01, mat.m02).normalize(),
            b = new Vector(mat.m10, mat.m11, mat.m12).normalize(),
            c = new Vector(mat.m20, mat.m21, mat.m22).normalize();
        Vector[][] basisVecs = {{a, c}, {a, c}, {a, b}, {a.clone().multiply(-1), b}, {c.clone().multiply(-1), b}, {c, b}};
        return basisVecs;
    }

    private Vector[][] getBasisVecs(Vector a, Vector b, Vector c) {
        Vector[][] basisVecs = {{a, c}, {a, c}, {a, b}, {a.clone().multiply(-1), b}, {c.clone().multiply(-1), b}, {c, b}};
        return basisVecs;
    }

    /**
     * Calculates the precise block display entity that the player is currently looking at with their cursor
     *
     * @param p
     * @return Unmodifiable data object containing the entity, entity's center, block face, location on the block face, and basis vectors.
     * @return Null if the player is not looking at any block display in range
     */
    public ClickedBlockDisplay getLookingAt(Player p) {
        if (p == null) {
            throw new IllegalArgumentException("Player cannot be null!");
        }
        List<Entity> near = p.getNearbyEntities(4, 4, 4);
        Vector dir = p.getLocation().getDirection();
        Vector eyeLoc = p.getEyeLocation().toVector();
        double minDist = Double.MAX_VALUE;
        ClickedBlockDisplay nearest = null;

        bdLoop:
        for (Entity entity : near) {
            if (!(entity instanceof BlockDisplay e) || markerPoints.contains(entity.getUniqueId())) {
                continue;
            }
            Vector scaleRaw = DexUtils.vector(e.getTransformation().getScale());
            if (scaleRaw.getX() < 0 || scaleRaw.getY() < 0 || scaleRaw.getZ() < 0) {
                continue; //TODO figure out displacement to center
            }
            scaleRaw.multiply(0.5);
            Vector scale = DexUtils.hadimard(DexUtils.getBlockDimensions(e.getBlock()), scaleRaw);

            //check if the player is looking in the general direction of the block, accounting for scale
            Vector diff = e.getLocation().toVector().subtract(eyeLoc).normalize();
            double dot = diff.dot(dir);
            if (dot < (-0.375 * scale.lengthSquared()) + 0.75) {
                continue; //TODO: taylor series to improve the approximation
            }

            Vector upDir, southDir, eastDir;
            Vector[][] basisVecs;
            DexBlock db = plugin.getMappedDisplay(e.getUniqueId());

            //calculate roll and its offset
            RollOffset ro = null;
            Location loc;
            if (db == null) {
                Vector displacement = DexUtils.vector(e.getTransformation().getTranslation());
                if (e.getTransformation().getLeftRotation().w != 0) {
                    OrientationKey key = new OrientationKey(e.getTransformation().getScale().x,
                        e.getTransformation().getScale().y,
                        e.getTransformation().getLeftRotation());
                    ro = rollOffsets.get(key); //does not account for pitch and yaw built into the rotation quaternion, assumed that blocks managed by other plugins are not built on
                    if (ro == null) {
                        ro = new RollOffset(e.getTransformation().getLeftRotation(), DexUtils.vector(e.getTransformation().getScale()));
                        rollOffsets.put(key, ro);
                    }
                    displacement.subtract(ro.getOffset());
                }
                loc = e.getLocation().add(displacement).add(scaleRaw);
            } else {
                loc = db.getLocation(); //already handled by DexTransformation
            }

            //if rotated, we need to transform the displacement vecs and basis vectors accordingly
            if (e.getLocation().getYaw() != 0 || e.getLocation().getPitch() != 0 || e.getTransformation().getLeftRotation().w != 1) {

                OrientationKey key = new OrientationKey(e.getLocation().getYaw(), e.getLocation().getPitch(), e.getTransformation().getLeftRotation());
                Vector[] res = axes.get(key);
                if (res == null) {
                    Vector3f east_dir_d = new Vector3f(1, 0, 0), up_dir_d = new Vector3f(0, 1, 0), south_dir_d = new Vector3f(0, 0, 1);
                    Quaternionf q = DexUtils.cloneQ(e.getTransformation().getLeftRotation());
                    q.z = -q.z;
                    q.rotateX((float) -Math.toRadians(e.getLocation().getPitch()));
                    q.rotateY((float) Math.toRadians(e.getLocation().getYaw()));

                    q.transformInverse(east_dir_d);
                    q.transformInverse(up_dir_d);
                    q.transformInverse(south_dir_d);

                    eastDir = DexUtils.vector(east_dir_d);
                    upDir = DexUtils.vector(up_dir_d);
                    southDir = DexUtils.vector(south_dir_d);
                    basisVecs = getBasisVecs(eastDir, upDir, southDir);

                    Vector[] res2 = {eastDir, upDir, southDir};
                    axes.put(key, res2);

                } else {
                    eastDir = res[0];
                    upDir = res[1];
                    southDir = res[2];
                    basisVecs = getBasisVecs(eastDir, upDir, southDir);
                }
            } else {
                eastDir = eastUnit;
                upDir = upUnit;
                southDir = southUnit;
                basisVecs = basisVecsNoRot;
            }

            //calculate location of visual display accounting for axis asymmetry
            loc.add(upDir.clone().multiply(scale.getY() - scaleRaw.getY()));
            Vector locv = loc.toVector();

            //block face centers
            Vector up = locv.clone().add(upDir.clone().multiply(scale.getY())),
                down = locv.clone().add(upDir.clone().multiply(-scale.getY())),
                south = locv.clone().add(southDir.clone().multiply(scale.getZ())),
                north = locv.clone().add(southDir.clone().multiply(-scale.getZ())),
                east = locv.clone().add(eastDir.clone().multiply(scale.getX())),
                west = locv.clone().add(eastDir.clone().multiply(-scale.getX()));

            Vector[] locs = {up, down, south, north, east, west};

            boolean firstFace = false; //always intersects 2 faces - convex
            for (int i = 0; i < locs.length; i++) {
                if (!firstFace && i == locs.length - 1) {
                    continue bdLoop; //not looking at this block, no need to check last face
                }

                Vector basis1 = basisVecs[i][0], basis2 = basisVecs[i][1];

                // Solve `(FaceCenter) + a(basis1) + b(basis2) = c(dir) + (EyeLoc)` to find intersection of block face plane
                Vector L = eyeLoc.clone().subtract(locs[i]);
                Matrix3f matrix = new Matrix3f(
                    (float) basis1.getX(), (float) basis1.getY(), (float) basis1.getZ(),
                    (float) basis2.getX(), (float) basis2.getY(), (float) basis2.getZ(),
                    (float) dir.getX(), (float) dir.getY(), (float) dir.getZ()
                );
                matrix.invert();
                Vector3f cf = new Vector3f();
                Vector soln = DexUtils.vector(matrix.transform(DexUtils.vector(L), cf));
                double dist = -soln.getZ(); //distance from player's eye to precise location on surface in units of blocks (magic :3)
                if (dist < 0) {
                    continue; //behind head
                }

                switch (i) { //check within block face
                    case 0:
                    case 1:
                        if (Math.abs(soln.getX()) > scale.getX()) {
                            continue;
                        }
                        if (Math.abs(soln.getY()) > scale.getZ()) {
                            continue;
                        }
                        break;
                    case 2:
                    case 3:
                        if (Math.abs(soln.getX()) > scale.getX()) {
                            continue;
                        }
                        if (Math.abs(soln.getY()) > scale.getY()) {
                            continue;
                        }
                        break;
                    default:
                        if (Math.abs(soln.getX()) > scale.getZ()) {
                            continue;
                        }
                        if (Math.abs(soln.getY()) > scale.getY()) {
                            continue;
                        }
                }

                if (dist < minDist) {
                    Vector rawOffset = basis1.clone().multiply(soln.getX())
                        .add(basis2.clone().multiply(soln.getY()));
                    Vector blockoffset = locs[i].clone().add(rawOffset); //surface location

                    minDist = dist;
                    nearest = new ClickedBlockDisplay(e, faces[i], rawOffset, DexUtils.location(loc.getWorld(), blockoffset),
                        loc, upDir, eastDir, southDir, dist);
                    if (ro != null) {
                        nearest.setRollOffset(ro);
                    }
                }

                if (firstFace) {
                    continue bdLoop;
                } else {
                    firstFace = true;
                }
            }
        }

        return nearest;
    }

    /**
     * Calculates the precise placed block that the player is currently looking at with their cursor
     *
     * @param p
     * @return Unmodifiable data object containing the block and distance (in block units) away.
     * @return Null if the player is not looking at any block in range.
     */
    public ClickedBlock getPhysicalBlockLookingAt(Player p) {
        return getPhysicalBlockLookingAtRaw(p, 0.01, 5); // same as getBlockLookingAt(p, 100);
    }

    /**
     * Calculates the precise placed block that the player is currently looking at with their cursor
     *
     * @param p
     * @param stepMultiplier Defines the step size, and thus the precision to check
     * @param maxDist        Defines the maximum radius, in blocks, to search
     * @return Unmodifiable data object containing the block and distance (in block units) away.
     * @return Null if the player is not looking at any block in range.
     */
    public ClickedBlock getPhysicalBlockLookingAtRaw(Player p, double stepMultiplier, double maxDist) {
        Vector step = p.getLocation().getDirection().multiply(stepMultiplier);

        Location loc = p.getEyeLocation();
        int i = 0, max = (int) (maxDist / stepMultiplier);
        Block b = null;
        boolean found = false;
        while (i < max) {
            loc.add(step);
            i++;
            b = loc.getBlock();
            if (b.getType() == Material.AIR) {
                continue;
            }

            Vector size = DexUtils.getBlockDimensions(b.getBlockData());
            size.setX(size.getX() / 2);
            size.setZ(size.getZ() / 2);
            Vector locv = b.getLocation().add(0.5, 0, 0.5).toVector(); //TODO add 0.5 to y for upper slabs
            Vector l1 = locv.clone().subtract(size.clone().setY(0)), l2 = locv.clone().add(size);

            if (loc.getX() >= l1.getX() && loc.getX() <= l2.getX() &&
                loc.getY() >= l1.getY() && loc.getY() <= l2.getY() &&
                loc.getZ() >= l1.getZ() && loc.getZ() <= l2.getZ()) {
                found = true;
                break;
            }
        }

        if (!found || b == null || b.getType() == Material.AIR) {
            return null;
        }
        return new ClickedBlock(b, i * stepMultiplier);
    }

    /**
     * Creates a clone of the passed in display or selection and returns the clone. Does not merge the new clone.
     *
     * @param d The display or selection to clone
     */
    public DexterityDisplay clone(DexterityDisplay d) {
        DexterityDisplay clone = new DexterityDisplay(plugin, d.getCenter(), d.getScale());
        unTempHighlight(d);

        //start clone
        List<DexBlock> blocks = new ArrayList<>();
        for (DexBlock db : d.getBlocks()) {
            DexBlockState state = db.getState();
            state.setDisplay(clone);
            state.setUniqueId(null);
            blocks.add(new DexBlock(state));
        }
        clone.setBlocks(blocks, false);

        return clone;
    }

    /**
     * Creates a very small block display with a glow to illustrate a location with precision. Useful for debugging or verifying location variables.
     *
     * @param loc
     * @param glow    The color of the marker point
     * @param seconds The number of seconds until the marker point is removed, negative if permanent
     * @return The {@link BlockDisplay} of the created marker point
     */
    public BlockDisplay markerPoint(Location loc, Color glow, double seconds) {
        float size = 0.035f;
        Location loc_ = loc.clone();
        loc_.setPitch(0);
        loc_.setYaw(0);
        BlockDisplay disp = plugin.spawn(loc_, BlockDisplay.class, a -> {
            a.setBlock(Bukkit.createBlockData(Material.WHITE_CONCRETE));
            if (glow != null) {
                a.setGlowColorOverride(glow);
                a.setGlowing(true);
            }
            Transformation t = a.getTransformation();
            Transformation t2 = new Transformation(new Vector3f(-size / 2, -size / 2, -size / 2), t.getLeftRotation(),
                new Vector3f(size, size, size), t.getRightRotation());
            a.setTransformation(t2);
        });
        markerPoints.add(disp.getUniqueId());

        if (seconds >= 0.05) {
            new BukkitRunnable() {
                public void run() {
                    markerPoints.remove(disp.getUniqueId());
                    disp.remove();
                }
            }.runTaskLater(plugin, (int) Math.round(seconds * 20L));
        }
        return disp;
    }

    /**
     * Creates a small block display between from_loc and a second location defined by a vector offset. Useful for debugging or verifying location variables.
     *
     * @param fromLoc
     * @param delta
     * @param mat     Required material type
     * @param glow    The color of the vector
     * @param seconds The number of seconds until marker vector is removed, negative if permanent
     * @return The BlockDisplays used in the created marker vector, formatted as {body, head}
     */
    public BlockDisplay[] markerVector(Location fromLoc, Vector delta, Material mat, Color glow, double seconds) {
        return markerVector(fromLoc, fromLoc.clone().add(delta), mat, glow, seconds);
    }

    /**
     * Creates a small block display between from_loc and a second location defined by a vector offset. Useful for debugging or verifying location variables.
     *
     * @param from_loc
     * @param delta
     * @param glow     The color of the vector
     * @param seconds  The number of seconds until marker vector is removed, negative if permanent
     * @return The BlockDisplays used in the created marker vector, formatted as {body, head}
     */
    public BlockDisplay[] markerVector(Location from_loc, Vector delta, Color glow, double seconds) {
        return markerVector(from_loc, from_loc.clone().add(delta), Material.WHITE_CONCRETE, glow, seconds);
    }

    /**
     * Creates a small block display between the from_loc to the to_loc with precision. Useful for debugging or verifying location variables.
     *
     * @param from_loc
     * @param to_loc
     * @param glow     The color of the vector
     * @param seconds  The number of seconds until marker vector is removed, negative if permanent
     * @return The BlockDisplays used in the created marker vector, formatted as {body, head}
     */
    public BlockDisplay[] markerVector(Location from_loc, Location to_loc, Color glow, double seconds) {
        return markerVector(from_loc, to_loc, Material.WHITE_CONCRETE, glow, seconds);
    }


    /**
     * Creates a small block display between the from_loc to the to_loc with precision. Useful for debugging or verifying location variables.
     *
     * @param fromLoc
     * @param toLoc
     * @param mat     Required material type
     * @param glow    The color of the vector
     * @param seconds The number of seconds until marker vector is removed, negative if permanent
     * @return The BlockDisplays used in the created marker vector, formatted as {body, head}
     */
    public BlockDisplay[] markerVector(Location fromLoc, Location toLoc, Material mat, Color glow, double seconds) {
        float width = 0.035f;
        Location from = fromLoc.clone(), to = toLoc.clone();
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        float yaw = (float) -(Math.toDegrees(Math.atan2(dx, dz)));
        float pitch = (float) Math.toDegrees(Math.atan2(Math.sqrt(dz * dz + dx * dx), to.getY() - from.getY()));

        from.setYaw(yaw);
        from.setPitch(pitch);
        float len = (float) fromLoc.distance(toLoc);
        BlockData block = Bukkit.createBlockData(mat == null ? Material.WHITE_CONCRETE : mat);

        //body
        BlockDisplay body = plugin.spawn(from, BlockDisplay.class, a -> {
            a.setBlock(block);
            if (glow != null) {
                a.setGlowColorOverride(glow);
                a.setGlowing(true);
            }
            a.setTransformation(new Transformation(
                new Vector3f(-width / 2, 0, -width / 2), new Quaternionf(),
                new Vector3f(width, len, width), new Quaternionf()
            ));
        });

        //head
        float headDisp = 0.08f;
        Location tipStart = from.clone().add(to.clone().subtract(from).toVector().normalize().multiply(len - headDisp));
        BlockDisplay head = null;
        if (len > 0.25 && len <= 3) {
            head = plugin.spawn(tipStart, BlockDisplay.class, a -> {
                a.setBlock(block);
                if (glow != null) {
                    a.setGlowColorOverride(glow);
                    a.setGlowing(true);
                }
                a.setTransformation(new Transformation(
                    new Vector3f(-width * 1.5f, 0, -width * 1.5f), new Quaternionf(),
                    new Vector3f(width * 3, width, width * 3), new Quaternionf()
                ));
            });
            markerPoints.add(head.getUniqueId());
        }

        markerPoints.add(body.getUniqueId());
        final BlockDisplay headf = head;
        if (seconds >= 0.05) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                markerPoints.remove(body.getUniqueId());
                if (headf != null) {
                    markerPoints.remove(headf.getUniqueId());
                }
                body.remove();
                headf.remove();
            }, (int) Math.round(seconds * 20L));
        }
        return new BlockDisplay[]{body, head};
    }

    /**
     * Removes a marker point before the timer expires
     *
     * @param b Marker point block display entity
     */
    public void removeMarker(BlockDisplay b) {
        if (b == null) {
            return;
        }
        if (markerPoints.contains(b.getUniqueId())) {
            markerPoints.remove(b.getUniqueId());
            b.remove();
        }
    }

    /**
     * Removes a marker vector before the timer expires
     *
     * @param v Marker point block display entity
     */
    public void removeMarker(BlockDisplay[] v) {
        if (v == null) {
            return;
        }
        for (BlockDisplay bd : v) {
            if (bd == null) {
                continue;
            }
            if (markerPoints.contains(bd.getUniqueId())) {
                markerPoints.remove(bd.getUniqueId());
                bd.remove();
            }
        }
    }

    /**
     * Deletes all marker points and marker vectors
     */
    public void clearAllMarkers() {
        for (UUID u : markerPoints) {
            Entity e = Bukkit.getEntity(u);
            if (e != null) {
                e.remove();
            }
        }
        markerPoints.clear();
    }

    public String getAuthor() {
        String a = "%%__USER__%%, %%__RESOURCE__%%, %%__NONCE__%%"; //for the pirates that will remove this line, please read below:
        final String forPirates = "Make it FREE and LINK the original spigot or github page. If you make money off my work, I will find you >:3 (Translation: Y'arr thar' be gold to plunder from ye)";
        return ("ytrew").replace('y', 'C').replace('w', 'v').replace('t', '7').replace('r', 'd');
    }

    /**
     * Temporarily makes a {@link DexterityDisplay} glow for a period of time if it is not glowing already. Helpful for debugging or to visualize a selection.
     *
     * @param d
     * @param ticks The number of ticks (0.05 of a second) until the highlighting is reset.
     */
    public void tempHighlight(DexterityDisplay d, int ticks) {
        tempHighlight(d, ticks, Color.SILVER);
    }

    /**
     * Temporarily makes a {@link DexterityDisplay} glow for a period of time if it is not glowing already. Helpful for debugging or to visualize a selection.
     *
     * @param d
     * @param ticks The number of ticks (0.05 of a second) until the highlighting is reset.
     * @param c     The glow color to highlight with
     */
    public void tempHighlight(DexterityDisplay d, int ticks, Color c) {
        List<BlockDisplay> blocks = new ArrayList<>();
        for (DexBlock db : d.getBlocks()) {
            blocks.add(db.getEntity());
        }
        tempHighlight(blocks, ticks, c);
    }

    /**
     * Temporarily makes a block display glow for a period of time if it is not glowing already. Helpful for debugging or to visualize a selection.
     *
     * @param block The block display to highlight with
     * @param ticks The number of ticks (0.05 of a second) until the highlighting is reset.
     * @param c     The color to highlight with.
     */
    public void tempHighlight(BlockDisplay block, int ticks, Color c) {
        List<BlockDisplay> blocks = new ArrayList<>();
        blocks.add(block);
        tempHighlight(blocks, ticks, c);
    }

    private void tempHighlight(List<BlockDisplay> blocks, int ticks, Color c) {
        List<UUID> unhighlight = new ArrayList<>();
        int pid = getNewPID();
        for (BlockDisplay block : blocks) {
            if (!block.isGlowing() || pidMap.containsKey(block.getUniqueId())) {
                unhighlight.add(block.getUniqueId());
                block.setGlowColorOverride(c);
                block.setGlowing(true);
                pidMap.put(block.getUniqueId(), pid);
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID u : unhighlight) {
                if (pidMap.getOrDefault(u, Integer.MIN_VALUE) != pid) {
                    continue;
                }
                pidMap.remove(u);
                Entity e = Bukkit.getEntity(u);
                if (e != null) {
                    e.setGlowing(false);
                }
            }
        }, ticks);
    }

    /**
     * Undo any temporarily glow for a selection.
     *
     * @param d The selection or display to remove temporary highlighting from
     */
    public void unTempHighlight(DexterityDisplay d) {
        if (d == null) {
            return;
        }
        for (DexBlock db : d.getBlocks()) {
            if (isInProcess(db.getEntity().getUniqueId())) {
                db.getEntity().setGlowing(false);
            }
        }
    }

    /**
     * Check if the UUID of a block display is a marker point that will be removed in the future.
     * Used to check that a repeating process does not go into an infinite loop if it can create marker points.
     *
     * @param u
     * @return true if the entity with that UUID is a marker point or otherwise will be removed in the near future.
     */
    public boolean isInProcess(UUID u) {
        return pidMap.containsKey(u);
    }

    /**
     * Retrieves all of the block display entities within the cuboid defined by the two integer block locations
     *
     * @param l1r The first block location
     * @param l2r The second block location
     * @return Unmodifiable list of Block Display entities that are within the cuboid
     */
    public List<BlockDisplay> getBlockDisplaysInRegion(Location l1r, Location l2r) {
        return getBlockDisplaysInRegionContinuous(DexUtils.blockLoc(l1r.clone()), DexUtils.blockLoc(l2r.clone()), new Vector(0, 0, 0), new Vector(1, 1, 1));
    }

    /**
     * Retrieves all of the block display entities within the precise cuboid defined by the two continuous locations
     *
     * @param l1r The first location
     * @param l2r The second location
     * @param l1o The offset added to the minimum of the coordinates
     * @param l2o The offset added to the maximum of the coordinates
     * @return Unmodifiable list of Block Display entities that are within the cuboid
     */
    public List<BlockDisplay> getBlockDisplaysInRegionContinuous(Location l1r, Location l2r, Vector l1o, Vector l2o) {
        List<BlockDisplay> blocks = new ArrayList<>();

        Location l1 = l1r.clone(), l2 = l2r.clone();

        if (l1.getX() > l2.getX()) {
            double xt = l1.getX();
            l1.setX(l2.getX());
            l2.setX(xt);
        }
        if (l1.getY() > l2.getY()) {
            double yt = l1.getY();
            l1.setY(l2.getY());
            l2.setY(yt);
        }
        if (l1.getZ() > l2.getZ()) {
            double zt = l1.getZ();
            l1.setZ(l2.getZ());
            l2.setZ(zt);
        }

        l1.subtract(l1o);
        l2.add(l2o);

        int xchunks = (int) Math.ceil((l2.getX() - l1.getX()) / 16) + 1;
        int zchunks = (int) Math.ceil((l2.getZ() - l1.getZ()) / 16) + 1;
        Location sel = l1.clone();
        for (int x = 0; x < xchunks; x++) {
            Location xsel = sel.clone();
            for (int z = 0; z < zchunks; z++) {
                Chunk chunk = xsel.getChunk();
                if (!chunk.isLoaded()) {
                    continue;
                }
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof BlockDisplay bd && !markerPoints.contains(entity.getUniqueId())) {
                        if (entity.getLocation().getX() >= l1.getX() && entity.getLocation().getX() <= l2.getX()
                            && entity.getLocation().getY() >= l1.getY() && entity.getLocation().getY() <= l2.getY()
                            && entity.getLocation().getZ() >= l1.getZ() && entity.getLocation().getZ() <= l2.getZ()) {

                            blocks.add(bd);
                        }
                    }
                }
                xsel.add(0, 0, 16);
            }
            sel.add(16, 0, 0);
        }

        return blocks;
    }

    /**
     * Create a new {@link DexterityDisplay} of valid block displays within the precise cuboid defined by the two locations.
     * Equivalent to using the wand to select in-game.
     *
     * @param l1 The first location
     * @param l2 The second location
     * @return A new DexterityDisplay representing the selection made by the two locations.
     */
    public DexterityDisplay selectFromLocations(Location l1, Location l2) {
        return selectFromLocations(l1, l2, null, null, null);
    }

    /**
     * Create a new {@link DexterityDisplay} of valid block displays within the precise cuboid defined by the two locations.
     * Equivalent to using the wand to select in-game.
     *
     * @param l1   The first location
     * @param l2   The second location
     * @param mask A mask to use, or null for no mask
     * @return A new DexterityDisplay representing the selection made by the two locations.
     */
    public DexterityDisplay selectFromLocations(Location l1, Location l2, Mask mask) {
        return selectFromLocations(l1, l2, mask, null, null);
    }

    /**
     * Create a new {@link DexterityDisplay} of valid block displays within the precise cuboid defined by the two locations.
     * Equivalent to using the wand to select in-game.
     *
     * @param l1           The first location
     * @param l2           The second location
     * @param mask         A mask to use, or null for no mask
     * @param scaleOffset1 The offset added to the minimum of the coordinates
     * @param scaleOffset2 The offset added to the maximum of the coordinates
     * @return A new DexterityDisplay representing the selection made by the two locations.
     */
    public DexterityDisplay selectFromLocations(Location l1, Location l2, Mask mask, Vector scaleOffset1, Vector scaleOffset2) {
        if (!(l1 != null && l2 != null && l1.getWorld().getName().equals(l2.getWorld().getName()))) {
            return null;
        }

        if (scaleOffset1 == null) {
            scaleOffset1 = new Vector(0, 0, 0);
        }
        if (scaleOffset2 == null) {
            scaleOffset2 = new Vector(1, 1, 1);
        }

        if (DexUtils.getVolume(l1, l2) > plugin.getMaxVolume()) {
            throw new IllegalArgumentException("Location 1 and 2 for selected exceed maximum configured volume!");
        }

        int maxVol = plugin.getMaxVolume();
        List<BlockDisplay> blocks = plugin.api().getBlockDisplaysInRegionContinuous(l1, l2, scaleOffset1, scaleOffset2);
        if (blocks.isEmpty()) {
            return null;
        }
        DexterityDisplay s = new DexterityDisplay(plugin);
        List<DexBlock> dblocks = new ArrayList<>();
        HashMap<OrientationKey, RollOffset> rollCache = new HashMap<>();

        for (BlockDisplay bd : blocks) {
            if (mask != null && !mask.isAllowed(bd.getBlock().getMaterial())) {
                continue;
            }
            DexBlock db = plugin.getMappedDisplay(bd.getUniqueId());
            if (db == null) {
                db = new DexBlock(bd, s);
                db.loadRoll(rollCache); //TODO possibly make this async
            } else if (db.getDexterityDisplay().isSaved()) {
                continue;
            }
            if (db.getDexterityDisplay().getEditingLock() == null) {
                db.setDexterityDisplay(s);
            }
            dblocks.add(db);
            if (dblocks.size() >= maxVol) {
                break;
            }
        }

        if (dblocks.isEmpty()) {
            return null;
        }
        s.setBlocks(dblocks, true);
        return s;
    }
}