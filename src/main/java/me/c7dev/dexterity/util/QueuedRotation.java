package me.c7dev.dexterity.util;

import me.c7dev.dexterity.transaction.RotationTransaction;
import org.joml.Quaterniond;

/**
 * Holds the data queued for a pending rotation in {@link DexRotation}
 */
public class QueuedRotation {

    private final Quaterniond q1;
    private final RotationTransaction t;
    private boolean async = true;

    public QueuedRotation(Quaterniond q1, boolean async, RotationTransaction t) {
        this.q1 = q1;
        this.t = t;
        this.async = async;
    }

    public Quaterniond getQuaternion() {
        return q1;
    }

    public RotationTransaction getTransaction() {
        return t;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof QueuedRotation r)) {
            return false;
        }
        double epsilon = 0.000001;
        Quaterniond q2 = r.getQuaternion();
        return Math.abs(q1.w - q2.w) < epsilon && Math.abs(q1.z - q2.z) < epsilon
            && Math.abs(q1.y - q2.y) < epsilon && Math.abs(q1.x - q2.x) < epsilon
            && r.isAsync() == async; //not checking transaction
    }

}
