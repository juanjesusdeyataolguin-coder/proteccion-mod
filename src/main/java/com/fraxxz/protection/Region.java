package com.fraxxz.protection;

import java.util.Map;
import java.util.UUID;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record Region(UUID owner, String ownerName, Identifier dim, BlockPos min, BlockPos max, Map<UUID, String> trusted) {
    public boolean allows(UUID id) { return owner.equals(id) || trusted.containsKey(id); }
    public boolean contains(Identifier d, BlockPos p) {
        return dim.equals(d)
            && p.getX() >= min.getX() && p.getX() <= max.getX()
            && p.getY() >= min.getY() && p.getY() <= max.getY()
            && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
    }
    public boolean intersects(BlockPos a, BlockPos b) {
        return a.getX() <= max.getX() && b.getX() >= min.getX()
            && a.getY() <= max.getY() && b.getY() >= min.getY()
            && a.getZ() <= max.getZ() && b.getZ() >= min.getZ();
    }
}
