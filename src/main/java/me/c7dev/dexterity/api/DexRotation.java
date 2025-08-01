package me.c7dev.dexterity.api;

import me.c7dev.dexterity.api.events.DisplayRotationEvent;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.transaction.RotationTransaction;
import me.c7dev.dexterity.util.AxisPair;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.QueuedRotation;
import me.c7dev.dexterity.util.RotationPlan;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Manages all rotation events of a {@link DexterityDisplay}
 */
public class DexRotation {

    private final HashMap<Vector, Vector> dirs = new HashMap<>();
    private final HashMap<Vector, AxisPair> axisPairs = new HashMap<>();
    private final DexterityDisplay d;
    private Vector3d x = new Vector3d(1, 0, 0), y = new Vector3d(0, 1, 0), z = new Vector3d(0, 0, 1);
    private QueuedRotation last = null;
    private double baseX = 0, baseY = 0, baseZ = 0, basePitch = 0, baseRoll = 0, baseYaw = 0;
    private final List<BlockDisplay> points = new ArrayList<>();
    private RotationTransaction t = null;
    private final LinkedList<QueuedRotation> queue = new LinkedList<>();
    private boolean processing = false;

    public static final double cutoff = 0.000001;

    /**
     * Manages all rotations and data used for rotations for a specific DexterityDisplay
     *
     * @param d
     */
    public DexRotation(DexterityDisplay d) {
        if (d == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        this.d = d;
        refreshAxis();
    }

    /**
     * Manages all rotations and data used for rotations for a specific DexterityDisplay
     *
     * @param d
     * @param x The orthogonal unit vector for the selection's x direction
     * @param y The orthogonal unit vector for the selection's y direction
     * @param z The orthogonal unit vector for the selection's z direction
     */
    public DexRotation(DexterityDisplay d, Vector x, Vector y, Vector z) {
        if (d == null || x == null || y == null || z == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        if (!DexUtils.isOrthonormal(x, y, z)) {
            throw new IllegalArgumentException("Axes are not orthonormal!");
        }
        this.d = d;
        this.x = DexUtils.vectord(x);
        this.y = DexUtils.vectord(y);
        this.z = DexUtils.vectord(z);
    }

    /**
     * Manages all rotations and data used for rotations for a specific DexterityDisplay
     *
     * @param d
     * @param yaw   The selection's yaw in degrees
     * @param pitch The selection's pitch in degrees
     * @param roll  The selection's roll in degrees
     */
    public DexRotation(DexterityDisplay d, double yaw, double pitch, double roll) {
        if (d == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        this.d = d;
        baseYaw = yaw;
        basePitch = pitch;
        baseRoll = roll;
        Quaterniond s = new Quaterniond(0, 0, 0, 1);
        s.rotateZ(-Math.toRadians(baseRoll));
        s.rotateX(-Math.toRadians(basePitch));
        s.rotateY(Math.toRadians(baseYaw));

        x = new Vector3d(1, 0, 0);
        y = new Vector3d(0, 1, 0);
        z = new Vector3d(0, 0, 1);
        s.transformInverse(x);
        s.transformInverse(y);
        s.transformInverse(z);
    }

    /**
     * Recalculate the axes of the selection based on the most common {@link DexBlock} rotation
     */
    public void refreshAxis() {

        clearCached();

        //Finds the mode of all three axes, rather than the closest to zero
        double yawMode = 0, pitchMode = 0, rollMode = 0;
        int count = 0;
        for (DexBlock db : d.getBlocks()) {
            double yaw = db.getEntity().getLocation().getYaw(), pitch = db.getEntity().getLocation().getPitch(), roll = db.getRoll();

            if (yaw == yawMode && pitch == pitchMode && roll == rollMode) {
                count++;
            } else {
                count--;
                if (count < 0) {
                    yawMode = yaw;
                    pitchMode = pitch;
                    rollMode = roll;
                    count = 0;
                }
            }
        }

        baseYaw = yawMode;
        basePitch = pitchMode;
        baseRoll = rollMode;

        Quaterniond s = new Quaterniond(0, 0, 0, 1);
        s.rotateZ(Math.toRadians(-baseRoll));
        s.rotateX(Math.toRadians(-basePitch));
        s.rotateY(Math.toRadians(baseYaw));

        x = new Vector3d(1, 0, 0);
        y = new Vector3d(0, 1, 0);
        z = new Vector3d(0, 0, 1);
        s.transformInverse(x);
        s.transformInverse(y);
        s.transformInverse(z);

    }

    public Vector getXAxis() {
        return DexUtils.vector(x);
    }

    public Vector getYAxis() {
        return DexUtils.vector(y);
    }

    public Vector getZAxis() {
        return DexUtils.vector(z);
    }

    /**
     * Set a transaction to be used to be able to undo a rotation
     *
     * @param t2
     */
    public void setTransaction(RotationTransaction t2) { //async callback
        t = t2;
    }

    /**
     * Clear the cached or pre-calculated data pertaining to the selection's rotations
     */
    public void clearCached() {
        dirs.clear();
        axisPairs.clear();
    }

    public double getX() {
        return baseX;
    }

    public double getY() {
        return baseY;
    }

    public double getZ() {
        return baseZ;
    }

    public double getYaw() {
        return baseYaw;
    }

    public double getPitch() {
        return basePitch;
    }

    public double getRoll() {
        return baseRoll;
    }

    /**
     * Overwrite the axes of the selection, providing new orthogonal unit vectors
     *
     * @param x
     * @param y
     * @param z
     */
    public void setAxes(Vector x, Vector y, Vector z) {
        if (x == null || y == null || z == null) {
            throw new IllegalArgumentException("Axes cannot be null!");
        }
        if (!DexUtils.isOrthonormal(x, y, z)) {
            throw new IllegalArgumentException("Axes are not orthonormal!");
        }
        this.x = new Vector3d(x.getX(), x.getY(), x.getZ());
        this.y = new Vector3d(y.getX(), y.getY(), y.getZ());
        this.z = new Vector3d(z.getX(), z.getY(), z.getZ());
        clearCached();
    }

    /**
     * Recalculate the y, x, and z unit vectors by providing yaw, pitch, and roll in degrees
     *
     * @param yaw
     * @param pitch
     * @param roll
     */
    public void setAxes(float yaw, float pitch, float roll) {
        Quaterniond s = new Quaterniond(0, 0, 0, 1);
        s.rotateZ(-Math.toRadians(roll));
        s.rotateX(-Math.toRadians(pitch));
        s.rotateY(Math.toRadians(yaw));

        baseYaw = yaw;
        basePitch = pitch;
        baseRoll = roll;

        x = new Vector3d(1, 0, 0);
        y = new Vector3d(0, 1, 0);
        z = new Vector3d(0, 0, 1);
        s.transformInverse(x);
        s.transformInverse(y);
        s.transformInverse(z);
    }

    /**
     * Rotate around the yaw axis in degrees
     *
     * @param yawDeg
     */
    public void rotate(float yawDeg) {
        rotate(yawDeg, 0, 0);
    }

    /**
     * Rotate around the yaw, pitch, and roll directions in degrees
     */
    public Quaterniond rotate(float yawDeg, float pitchDeg, float rollDeg) {
        RotationPlan p = new RotationPlan();
        p.yawDeg = yawDeg;
        p.pitchDeg = pitchDeg;
        p.rollDeg = rollDeg;
        return rotate(p);
    }

    /**
     * Rotate by a specified plan with details about every axis and if it is asynchronous.
     *
     * @param plan
     * @return Unmodifiable quaternion for the queued rotation
     */
    public Quaterniond rotate(RotationPlan plan) {

        double delX, delY, delZ, delYaw, delPitch, delRoll;
        if (plan.reset) {
            delX = -plan.xDeg;
            delY = -plan.yDeg;
            delZ = -plan.zDeg;
            delPitch = plan.pitchDeg;
            delYaw = plan.yawDeg;
            delRoll = plan.rollDeg;
            baseY = baseX = baseZ = baseYaw = basePitch = baseRoll = 0;
        } else {
            delX = plan.setX ? -plan.xDeg - baseX : -plan.xDeg; //right hand rule
            delY = plan.setY ? -plan.yDeg - baseY : -plan.yDeg;
            delZ = plan.setZ ? -plan.zDeg - baseZ : -plan.zDeg;
            delYaw = plan.setYaw ? plan.yawDeg - baseYaw : plan.yawDeg;
            delPitch = plan.setPitch ? plan.pitchDeg - basePitch : plan.pitchDeg;
            delRoll = plan.setRoll ? plan.rollDeg - baseRoll : plan.rollDeg;
            if (delX == 0 && delY == 0 && delZ == 0 && delYaw == 0 && delPitch == 0 && delRoll == 0) {
                return null;
            }
        }

        Quaterniond q = new Quaterniond(0, 0, 0, 1);
        if (plan.reset) {
            q = resetQuaternion();
        }
        if (delZ != 0) {
            q = zQuaternion(Math.toRadians(delZ), q);
        }
        if (delRoll != 0) {
            q = rollQuaternion(Math.toRadians(delRoll), q);
        }
        if (delX != 0) {
            q = xQuaternion(Math.toRadians(delX), q);
        }
        if (delPitch != 0) {
            q = pitchQuaternion(Math.toRadians(delPitch), q);
        }
        if (delYaw != 0) {
            q = yawQuaternion(Math.toRadians(delYaw), q);
        }
        if (delY != 0) {
            q = yQuaternion(Math.toRadians(delY), q);
        }

        Quaterniond q1 = new Quaterniond();
        q.invert(q1);

        rotate(q1, plan.async);

        baseY = (baseY + delY) % 360;
        baseX = (baseX + delX) % 360;
        baseZ = (baseZ + delZ) % 360;
        baseYaw = (baseYaw + delYaw) % 360;
        basePitch = (basePitch + delPitch) % 360;
        baseRoll = (baseRoll + delRoll) % 360;

        return DexUtils.cloneQ(q1);
    }

    /**
     * Prepare a rotation that can be reused.
     * Note that using this will not update the internal yaw, pitch, roll, x, y, or z, so refreshAxis() will be needed afterwards.
     *
     * @param plan
     * @param transaction
     * @return
     */
    public QueuedRotation prepareRotation(RotationPlan plan, RotationTransaction transaction) {
        double delX, delY, delZ, delYaw, delPitch, delRoll;
        if (plan.reset) {
            delX = -plan.xDeg;
            delY = -plan.yDeg;
            delZ = -plan.zDeg;
            delPitch = plan.pitchDeg;
            delYaw = plan.yawDeg;
            delRoll = plan.rollDeg;
            baseY = baseX = baseZ = baseYaw = basePitch = baseRoll = 0;
        } else {
            delX = plan.setX ? -plan.xDeg - baseX : -plan.xDeg; //right hand rule
            delY = plan.setY ? -plan.yDeg - baseY : -plan.yDeg;
            delZ = plan.setZ ? -plan.zDeg - baseZ : -plan.zDeg;
            delYaw = plan.setYaw ? plan.yawDeg - baseYaw : plan.yawDeg;
            delPitch = plan.setPitch ? plan.pitchDeg - basePitch : plan.pitchDeg;
            delRoll = plan.setRoll ? plan.rollDeg - baseRoll : plan.rollDeg;
            if (delX == 0 && delY == 0 && delZ == 0 && delYaw == 0 && delPitch == 0 && delRoll == 0) {
                return null;
            }
        }

        Quaterniond q = new Quaterniond(0, 0, 0, 1);
        if (plan.reset) {
            q = resetQuaternion();
        }
        if (delZ != 0) {
            q = zQuaternion(Math.toRadians(delZ), q);
        }
        if (delRoll != 0) {
            q = rollQuaternion(Math.toRadians(delRoll), q);
        }
        if (delX != 0) {
            q = xQuaternion(Math.toRadians(delX), q);
        }
        if (delPitch != 0) {
            q = pitchQuaternion(Math.toRadians(delPitch), q);
        }
        if (delYaw != 0) {
            q = yawQuaternion(Math.toRadians(delYaw), q);
        }
        if (delY != 0) {
            q = yQuaternion(Math.toRadians(delY), q);
        }

        Quaterniond q1 = new Quaterniond();
        q.invert(q1);

        return new QueuedRotation(q1, plan.async, transaction);
    }

    private Quaterniond yQuaternion(double rads, Quaterniond src) {
        double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads / 2);
        Quaterniond qY = new Quaterniond(0, sintheta, 0, costheta);
        qY.transformInverse(x);
        qY.transformInverse(y);
        qY.transformInverse(z);
        return src.mul(qY);
    }

    private Quaterniond yawQuaternion(double rads, Quaterniond src) {
        double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads / 2);
        Quaterniond qYaw = new Quaterniond(sintheta * y.x, sintheta * y.y, sintheta * y.z, costheta);
        qYaw.transformInverse(x);
        qYaw.transformInverse(y);
        qYaw.transformInverse(z);
        return src.mul(qYaw);
    }

    private Quaterniond xQuaternion(double rads, Quaterniond src) {
        double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads / 2);
        Quaterniond qX = new Quaterniond(sintheta, 0, 0, costheta);
        qX.transformInverse(x);
        qX.transformInverse(y);
        qX.transformInverse(z);
        return src.mul(qX);
    }

    private Quaterniond pitchQuaternion(double rads, Quaterniond src) {
        double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads / 2);
        Quaterniond qPitch = new Quaterniond(sintheta * x.x, sintheta * x.y, sintheta * x.z, costheta);
        qPitch.transformInverse(x);
        qPitch.transformInverse(y);
        qPitch.transformInverse(z);
        return src.mul(qPitch);
    }

    private Quaterniond zQuaternion(double rads, Quaterniond src) {
        double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads / 2);
        Quaterniond qZ = new Quaterniond(0, 0, sintheta, costheta);
        qZ.transformInverse(x);
        qZ.transformInverse(y);
        qZ.transformInverse(z);
        return src.mul(qZ);
    }

    private Quaterniond rollQuaternion(double rads, Quaterniond src) {
        double sintheta = Math.sin(rads / 2), costheta = Math.cos(rads / 2);
        Quaterniond qRoll = new Quaterniond(sintheta * z.x, sintheta * z.y, sintheta * z.z, costheta);
        qRoll.transformInverse(x);
        qRoll.transformInverse(y);
        qRoll.transformInverse(z);
        return src.mul(qRoll);
    }

    private Quaterniond resetQuaternion() {
        Vector3d cross = new Vector3d(), cross2 = new Vector3d(),
            xTarget = new Vector3d(1, 0, 0), yTarget = new Vector3d(0, 1, 0);
        yTarget.cross(y, cross);
        Quaterniond qResetY = new Quaterniond(cross.x, cross.y, cross.z, 1 + y.y);
        qResetY.transformInverse(x);
        qResetY.transformInverse(y);
        qResetY.transformInverse(z);

        xTarget.cross(x, cross2);
        Quaterniond qResetXZ = new Quaterniond(cross2.x, cross2.y, cross2.z, 1 + x.x);
        qResetXZ.transformInverse(x);
        qResetXZ.transformInverse(y);
        qResetXZ.transformInverse(z);

        Quaterniond r = new Quaterniond(0, 0, 0, 1);
        return r.mul(qResetY).mul(qResetXZ);
    }

    /**
     * Run the previous rotation again
     */
    public void again() {
        rotate(last);
    }

    /**
     * Queue a rotation
     *
     * @param q1
     */
    public void rotate(Quaterniond q1) {
        rotate(q1, true);
    }

    /**
     * Queue a rotation
     *
     * @param q1
     * @param async
     */
    public void rotate(Quaterniond q1, boolean async) {
        if (q1 == null) {
            throw new IllegalArgumentException("Quaternion cannot be null!");
        }
        rotate(new QueuedRotation(q1, async, t));
    }


    /**
     * Queue a rotation
     *
     * @param rotation
     */
    public void rotate(QueuedRotation rotation) {
        if (rotation == null) {
            throw new IllegalArgumentException("Rotation cannot be null!");
        }
        queue.addLast(rotation);
        if (!processing) {
            dequeue();
        }
    }

    private void dequeue() {
        if (queue.size() == 0) {
            processing = false;
            t = null;
            return;
        }
        QueuedRotation r = queue.getFirst();
        queue.removeFirst();
        if (r.isAsync()) {
            executeRotationAsync(r);
        } else {
            executeRotation(r);
        }
    }

    /**
     * Clears the queue of scheduled rotations
     */
    public void clearQueue() {
        queue.clear();
    }

    /**
     * Get the data used for the previous rotation
     *
     * @return Unmodifiable object containing quaternion, transaction, and async boolean
     */
    public QueuedRotation getPreviousRotation() {
        return last;
    }

    //avg 0.00048400 ms per block :3
    private void executeRotation(QueuedRotation rot) {
        Quaterniond q1 = rot.getQuaternion();
        RotationTransaction trans = rot.getTransaction();
        if (d == null) {
            throw new IllegalArgumentException("Quaternion cannot be null!");
        }

        DisplayRotationEvent event = new DisplayRotationEvent(d, q1);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            dequeue();
            return;
        }

        //y rotation simple
        if (q1.x == 0 && q1.z == 0) {
            last = rot;
            processing = true;
            simpleRotateY(q1, trans);
            return;
        }

        if (!rot.equals(last)) {
            dirs.clear(); //TODO only do this when different rotation
        }

        Vector centerv = d.getCenter().toVector();
        processing = true;
        for (DexBlock db : d.getBlocks()) {
            Vector key = new Vector(db.getEntity().getLocation().getPitch(), db.getEntity().getLocation().getYaw(), db.getRoll());
            Vector dir = dirs.get(key);
            if (dir == null) {
                AxisPair a = axisPairs.get(key);
                if (a == null) {
                    a = new AxisPair(db);
                }

                a.transform(q1);
                dir = a.getPitchYawRoll();
                dirs.put(key, dir);
                axisPairs.put(dir, a);
            }

            Vector r = db.getLocation().toVector().subtract(centerv);
            Vector3d rTrans = DexUtils.vectord(r);
            q1.transform(rTrans);

            Vector offset = DexUtils.vector(rTrans).subtract(r);
            db.move(offset);
            db.getEntity().setRotation((float) dir.getY(), (float) dir.getX());
            db.setRoll((float) dir.getZ());
        }

        if (trans != null) {
            trans.commit();
        }

        last = rot;
        dequeue();
    }

    private void executeRotationAsync(QueuedRotation rot) {
        Quaterniond q1 = rot.getQuaternion();
        RotationTransaction trans = rot.getTransaction();
        if (q1 == null) {
            throw new IllegalArgumentException("Quaternion cannot be null!");
        }

        DisplayRotationEvent event = new DisplayRotationEvent(d, q1);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            dequeue();
            return;
        }

        //y rotation simple
        if (q1.x == 0 && q1.z == 0) {
            last = rot;
            processing = true;
            simpleRotateY(q1, trans);
            return;
        }

        if (!rot.equals(last)) {
            dirs.clear();
        }

        processing = true;
        Vector centerv = d.getCenter().toVector();
        new BukkitRunnable() {
            @Override
            public void run() {

                HashMap<UUID, Vector> offsets = new HashMap<>(), rots = new HashMap<>();

                for (DexBlock db : d.getBlocks()) { //mapping
                    Vector key = new Vector(db.getEntity().getLocation().getPitch(), db.getEntity().getLocation().getYaw(), db.getRoll());
                    Vector dir = dirs.get(key);
                    if (dir == null) {
                        AxisPair a = axisPairs.get(key);
                        if (a == null) {
                            a = new AxisPair(db);
                        }

                        a.transform(q1);
                        dir = a.getPitchYawRoll();
                        dirs.put(key, dir);
                        axisPairs.put(dir, a);
                    }

                    Vector r = db.getLocation().toVector().subtract(centerv);
                    Vector3d r_trans = DexUtils.vectord(r);
                    q1.transform(r_trans);
                    Vector offset = DexUtils.vector(r_trans).subtract(r);

                    offsets.put(db.getEntity().getUniqueId(), offset);
                    rots.put(db.getEntity().getUniqueId(), dir);

                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (DexBlock db : d.getBlocks()) {

                            Vector offset = offsets.get(db.getEntity().getUniqueId()), dir = rots.get(db.getEntity().getUniqueId());
                            if (offset == null || dir == null) {
                                continue;
                            }

                            db.move(offset);
                            db.getEntity().setRotation((float) dir.getY(), (float) dir.getX());
                            db.setRoll((float) dir.getZ());
                        }

                        if (trans != null) {
                            trans.commit();
                        }

                        last = rot;
                        dequeue();

                    }
                }.runTask(d.getPlugin());

            }
        }.runTaskAsynchronously(d.getPlugin());
    }

    //if rotating around absolute y axis, only need a simple rotation matrix
    private void simpleRotateY(Quaterniond q, RotationTransaction trans) {
        float rad = 2 * (float) Math.asin(q.y);
        double cosy = Math.cos(rad), siny = Math.sin(rad);
        float deg = (float) Math.toDegrees(rad);

        Matrix3d rotmat = new Matrix3d(
            cosy, 0, -siny,
            0, 1, 0,
            siny, 0, cosy
        );
        Vector centerv = d.getCenter().toVector();

        for (DexBlock db : d.getBlocks()) {
            Vector3d r = DexUtils.vectord(db.getLocation().toVector().subtract(centerv));
            rotmat.transform(r);
            Location to = DexUtils.location(d.getWorld(), DexUtils.vector(r).add(centerv));
            to.setYaw(db.getEntity().getLocation().getYaw() - deg);
            to.setPitch(db.getEntity().getLocation().getPitch());
            db.teleport(to);
        }

        if (trans != null) {
            trans.commit();
        }

        dequeue();
    }

    /**
     * Create marker points illustrating the direction of each axis.
     * X: Red, Y: Lime, Z: Blue
     *
     * @param seconds The number of seconds that the marker points should last.
     */
    public void highlightAxes(int seconds) {
        for (BlockDisplay b : points) {
            b.remove();
        }
        points.clear();

        points.add(d.getPlugin().api().markerPoint(d.getCenter().add(DexUtils.vector(x)), Color.RED, seconds));
        points.add(d.getPlugin().api().markerPoint(d.getCenter().add(DexUtils.vector(y)), Color.LIME, seconds));
        points.add(d.getPlugin().api().markerPoint(d.getCenter().add(DexUtils.vector(z)), Color.BLUE, seconds));
        points.add(d.getPlugin().api().markerPoint(d.getCenter(), Color.SILVER, seconds));
    }
}
