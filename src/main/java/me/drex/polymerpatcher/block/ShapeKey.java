package me.drex.polymerpatcher.block;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public record ShapeKey(List<AABB> boxes) {

    private static final double EPS = 1e-7;

    public static ShapeKey of(VoxelShape shape) {
        if (shape.isEmpty()) return new ShapeKey(List.of());

        List<AABB> boxes = new ArrayList<>(shape.toAabbs());

        boxes.sort(Comparator
            .comparingDouble((AABB bb) -> bb.minX)
            .thenComparingDouble(bb -> bb.minY)
            .thenComparingDouble(bb -> bb.minZ)
            .thenComparingDouble(bb -> bb.maxX)
            .thenComparingDouble(bb -> bb.maxY)
            .thenComparingDouble(bb -> bb.maxZ)
        );

        return new ShapeKey(boxes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ShapeKey(List<AABB> boxes1))) return false;
        if (boxes.size() != boxes1.size()) return false;

        for (int i = 0; i < boxes.size(); i++) {
            AABB a = boxes.get(i);
            AABB b = boxes1.get(i);

            if (Math.abs(a.minX - b.minX) > EPS) return false;
            if (Math.abs(a.minY - b.minY) > EPS) return false;
            if (Math.abs(a.minZ - b.minZ) > EPS) return false;
            if (Math.abs(a.maxX - b.maxX) > EPS) return false;
            if (Math.abs(a.maxY - b.maxY) > EPS) return false;
            if (Math.abs(a.maxZ - b.maxZ) > EPS) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (AABB bb : boxes) {
            h = 31 * h + java.util.Objects.hash(
                bb.minX, bb.minY, bb.minZ,
                bb.maxX, bb.maxY, bb.maxZ
            );
        }
        return h;
    }
}