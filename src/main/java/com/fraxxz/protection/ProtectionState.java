package com.fraxxz.protection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

public class ProtectionState extends PersistentState {
    public final List<Region> regions = new ArrayList<>();

    public static ProtectionState get(MinecraftServer s) {
        return s.getOverworld().getPersistentStateManager()
            .getOrCreate(ProtectionState::read, ProtectionState::new, "protection_regions");
    }

    public static ProtectionState read(NbtCompound nbt) {
        ProtectionState st = new ProtectionState();
        NbtList list = nbt.getList("r", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            int[] a = c.getIntArray("min"), b = c.getIntArray("max");
            Map<UUID, String> tr = new LinkedHashMap<>();
            NbtList tl = c.getList("t", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < tl.size(); j++) {
                NbtCompound e = tl.getCompound(j);
                tr.put(e.getUuid("id"), e.getString("name"));
            }
            st.regions.add(new Region(c.getUuid("owner"), c.getString("name"), new Identifier(c.getString("dim")),
                new BlockPos(a[0], a[1], a[2]), new BlockPos(b[0], b[1], b[2]), tr));
        }
        return st;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Region r : regions) {
            NbtCompound c = new NbtCompound();
            c.putUuid("owner", r.owner());
            c.putString("name", r.ownerName());
            c.putString("dim", r.dim().toString());
            c.putIntArray("min", new int[]{r.min().getX(), r.min().getY(), r.min().getZ()});
            c.putIntArray("max", new int[]{r.max().getX(), r.max().getY(), r.max().getZ()});
            NbtList tl = new NbtList();
            for (Map.Entry<UUID, String> e : r.trusted().entrySet()) {
                NbtCompound x = new NbtCompound();
                x.putUuid("id", e.getKey());
                x.putString("name", e.getValue());
                tl.add(x);
            }
            c.put("t", tl);
            list.add(c);
        }
        nbt.put("r", list);
        return nbt;
    }

    public Region at(Identifier dim, BlockPos pos) {
        for (Region r : regions) if (r.contains(dim, pos)) return r;
        return null;
    }
    public Region owned(Identifier dim, BlockPos pos, UUID id) {
        for (Region r : regions) if (r.contains(dim, pos) && r.owner().equals(id)) return r;
        return null;
    }
    public void add(Region r) { regions.add(r); markDirty(); }
    public void remove(Region r) { regions.remove(r); markDirty(); }
}
