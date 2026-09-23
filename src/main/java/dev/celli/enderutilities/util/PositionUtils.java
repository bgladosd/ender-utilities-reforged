package dev.celli.enderutilities.util;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PositionUtils {
    public static final Direction[] ADJACENT_SIDES_ZY = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH
    };
    public static final Direction[] ADJACENT_SIDES_XY = {
            Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST
    };
    public static final Direction[] ADJACENT_SIDES_XZ = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private static final Direction[][] FROM_TO_CW_ROTATION_AXES = {
            {null, null, Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH},
            {null, null, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
            {Direction.EAST, Direction.WEST, null, null, Direction.DOWN, Direction.UP},
            {Direction.WEST, Direction.EAST, null, null, Direction.UP, Direction.DOWN},
            {Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN, null, null},
            {Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.UP, null, null}
    };

    private PositionUtils() {
    }

    public static Direction[] getSidesForAxis(Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            return ADJACENT_SIDES_ZY;
        }
        return axis == Direction.Axis.Z ? ADJACENT_SIDES_XY : ADJACENT_SIDES_XZ;
    }

    public static BlockPos[] getAdjacentPositions(BlockPos center, Direction front, boolean diagonals) {
        if (diagonals) {
            throw new UnsupportedOperationException();
        }
        Direction[] sides = getSidesForAxis(front.getAxis());
        BlockPos[] positions = new BlockPos[sides.length];
        for (int i = 0; i < sides.length; i++) {
            positions[i] = center.relative(sides[i]);
        }
        return positions;
    }

    public static Vec3 rotatePointAroundAxis(double x, double y, double z, Vec3 reference, Direction from, Direction to) {
        if (to == from.getOpposite()) {
            double rx = reference.x;
            if (from.getAxis().isHorizontal()) {
                double rz = reference.z;
                x = rx + (rx - x);
                z = rz + (rz - z);
            } else {
                double ry = reference.y;
                x = rx + (rx - x);
                y = ry + (ry - y);
            }
            return new Vec3(x, y, z);
        }
        return rotatePointCwAroundAxis(x, y, z, reference, FROM_TO_CW_ROTATION_AXES[from.get3DDataValue()][to.get3DDataValue()]);
    }

    public static Vec3 rotatePointCwAroundAxis(double x, double y, double z, Vec3 reference, Direction facing) {
        if (facing == null) {
            return new Vec3(x, y, z);
        }
        double rx = reference.x;
        double ry = reference.y;
        double rz = reference.z;
        double newX = x;
        double newY = y;
        double newZ = z;
        if (facing.getAxis() == Direction.Axis.Y) {
            if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                newX = rx - (z - rz);
                newZ = rz + (x - rx);
            } else {
                newX = rx + (z - rz);
                newZ = rz - (x - rx);
            }
        } else if (facing.getAxis() == Direction.Axis.Z) {
            if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                newX = rx + (y - ry);
                newY = ry - (x - rx);
            } else {
                newX = rx - (y - ry);
                newY = ry + (x - rx);
            }
        } else if (facing.getAxis() == Direction.Axis.X) {
            if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                newZ = rz - (y - ry);
                newY = ry + (z - rz);
            } else {
                newZ = rz + (y - ry);
                newY = ry - (z - rz);
            }
        }
        return new Vec3(newX, newY, newZ);
    }

    public static AABB rotateBoxAroundPoint(AABB bb, Vec3 reference, Direction from, Direction to) {
        if (from == to) {
            return bb;
        }
        Vec3 min = rotatePointAroundAxis(bb.minX, bb.minY, bb.minZ, reference, from, to);
        Vec3 max = rotatePointAroundAxis(bb.maxX, bb.maxY, bb.maxZ, reference, from, to);
        return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    @Nullable
    public static <T> T getPointedBox(Entity entity, double reach, Map<T, AABB> boxMap) {
        Vec3 eyes = entity.getEyePosition();
        Vec3 look = entity.getViewVector(1.0F);
        Vec3 lookEnd = eyes.add(look.x * reach, look.y * reach, look.z * reach);
        double distance = reach;
        T key = null;
        for (Map.Entry<T, AABB> entry : boxMap.entrySet()) {
            AABB bb = entry.getValue();
            var hit = bb.clip(eyes, lookEnd);
            if (bb.contains(eyes)) {
                if (distance >= 0.0) {
                    distance = 0.0;
                    key = entry.getKey();
                }
            } else if (hit.isPresent()) {
                double distanceTmp = eyes.distanceTo(hit.get());
                if (distanceTmp < distance) {
                    distance = distanceTmp;
                    key = entry.getKey();
                }
            }
        }
        return key;
    }
}
