package com.fraxxz.protection;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.server.world.ServerWorld;

public class ProtectionMod implements ModInitializer {
    public static final String ID = "protection";
    public static final Identifier CLAIM = new Identifier(ID, "claim");
    public static final Identifier REMOVE = new Identifier(ID, "remove");
    public static final int MAX_SIDE = 128;

    public static final Block PROTECTION_ORE = new ExperienceDroppingBlock(
        FabricBlockSettings.copyOf(Blocks.DIAMOND_ORE), UniformIntProvider.create(3, 7));
    public static final Item PROTECTION_CRYSTAL = new Item(new FabricItemSettings());

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, new Identifier(ID, "protection_ore"), PROTECTION_ORE);
        Registry.register(Registries.ITEM, new Identifier(ID, "protection_ore"), new BlockItem(PROTECTION_ORE, new FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier(ID, "protection_crystal"), PROTECTION_CRYSTAL);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> e.add(PROTECTION_CRYSTAL));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(e -> e.add(PROTECTION_ORE));

        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(ID, "protection_ore")));

        ServerPlayNetworking.registerGlobalReceiver(CLAIM, (server, player, handler, buf, sender) -> {
            BlockPos a = buf.readBlockPos(), b = buf.readBlockPos();
            server.execute(() -> claim(player, a, b));
        });
        ServerPlayNetworking.registerGlobalReceiver(REMOVE, (server, player, handler, buf, sender) ->
            server.execute(() -> remove(player)));

        // Nadie (ni OP, ni creativo) que no sea el dueño puede tocar un área protegida.
        PlayerBlockBreakEvents.BEFORE.register((w, p, pos, st, be) -> !denied(p, w, pos));
        AttackBlockCallback.EVENT.register((p, w, h, pos, dir) -> denied(p, w, pos) ? ActionResult.FAIL : ActionResult.PASS);
        UseBlockCallback.EVENT.register((p, w, h, hit) -> {
            BlockPos c = hit.getBlockPos();
            return (denied(p, w, c) || denied(p, w, c.offset(hit.getSide()))) ? ActionResult.FAIL : ActionResult.PASS;
        });
        UseEntityCallback.EVENT.register((p, w, h, e, hit) -> denied(p, w, e.getBlockPos()) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((p, w, h, e, hit) ->
            (!(e instanceof HostileEntity) && denied(p, w, e.getBlockPos())) ? ActionResult.FAIL : ActionResult.PASS);
    }

    static boolean denied(PlayerEntity p, World w, BlockPos pos) {
        if (w.isClient || !(w instanceof ServerWorld sw)) return false;
        Region r = ProtectionState.get(sw.getServer()).at(w.getRegistryKey().getValue(), pos);
        if (r == null || r.owner().equals(p.getUuid())) return false;
        p.sendMessage(Text.literal("§cÁrea protegida por " + r.ownerName()), true);
        return true;
    }

    static void claim(ServerPlayerEntity p, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        if (max.getX() - min.getX() >= MAX_SIDE || max.getY() - min.getY() >= MAX_SIDE || max.getZ() - min.getZ() >= MAX_SIDE) {
            p.sendMessage(Text.literal("§cMáximo " + MAX_SIDE + " bloques por lado"), false);
            return;
        }
        ProtectionState st = ProtectionState.get(p.server);
        Identifier dim = p.getWorld().getRegistryKey().getValue();
        for (Region r : st.regions) {
            if (r.dim().equals(dim) && !r.owner().equals(p.getUuid()) && r.intersects(min, max)) {
                p.sendMessage(Text.literal("§cEl área se cruza con la de " + r.ownerName()), false);
                return;
            }
        }
        if (!p.isCreative()) {
            ItemStack found = null;
            for (int i = 0; i < p.getInventory().size(); i++) {
                ItemStack s = p.getInventory().getStack(i);
                if (s.isOf(PROTECTION_CRYSTAL)) { found = s; break; }
            }
            if (found == null) {
                p.sendMessage(Text.literal("§cNecesitas un Cristal de protección (sale del Mineral de protección)"), false);
                return;
            }
            found.decrement(1);
        }
        st.add(new Region(p.getUuid(), p.getName().getString(), dim, min, max));
        p.sendMessage(Text.literal("§aÁrea protegida"), false);
    }

    static void remove(ServerPlayerEntity p) {
        ProtectionState st = ProtectionState.get(p.server);
        Region r = st.at(p.getWorld().getRegistryKey().getValue(), p.getBlockPos());
        if (r != null && r.owner().equals(p.getUuid())) {
            st.remove(r);
            p.sendMessage(Text.literal("§eÁrea eliminada"), false);
        } else {
            p.sendMessage(Text.literal("§cNo estás dentro de un área tuya"), false);
        }
    }
}
