package com.fraxxz.protection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class FreecamClient {
    public static boolean active;
    public static BlockPos p1, p2;
    static Vec3d prev = Vec3d.ZERO, cur = Vec3d.ZERO;

    public static Vec3d pos(float td) {
        return new Vec3d(prev.x + (cur.x - prev.x) * td, prev.y + (cur.y - prev.y) * td, prev.z + (cur.z - prev.z) * td);
    }

    public static void toggle() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null) return;
        active = !active;
        if (active) {
            cur = prev = c.player.getEyePos();
            c.player.sendMessage(Text.literal("§bCámara libre: clic izq = posición 1, clic der = posición 2, P = menú"), true);
        }
    }

    public static void exit(boolean clear) {
        active = false;
        if (clear) { p1 = null; p2 = null; }
    }

    public static void tick(MinecraftClient c) {
        if (!active) return;
        if (c.player == null || c.world == null) { active = false; return; }
        prev = cur;
        Vec3d f = c.player.getRotationVec(1f);
        Vec3d r = new Vec3d(-f.z, 0, f.x).normalize();
        double s = c.options.sprintKey.isPressed() ? 2.5 : 0.8;
        Vec3d m = Vec3d.ZERO;
        if (c.options.forwardKey.isPressed()) m = m.add(f);
        if (c.options.backKey.isPressed()) m = m.subtract(f);
        if (c.options.rightKey.isPressed()) m = m.add(r);
        if (c.options.leftKey.isPressed()) m = m.subtract(r);
        if (c.options.jumpKey.isPressed()) m = m.add(0, 1, 0);
        if (c.options.sneakKey.isPressed()) m = m.add(0, -1, 0);
        cur = cur.add(m.multiply(s));
    }

    public static void select(boolean first) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.world == null) return;
        Vec3d start = cur;
        Vec3d end = start.add(c.player.getRotationVec(1f).multiply(128));
        BlockHitResult hit = c.world.raycast(new RaycastContext(start, end,
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, c.player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            c.player.sendMessage(Text.literal("§cApunta a un bloque (máx. 128)"), true);
            return;
        }
        BlockPos p = hit.getBlockPos();
        if (first) p1 = p; else p2 = p;
        c.player.sendMessage(Text.literal("§aPosición " + (first ? 1 : 2) + ": " + p.getX() + " " + p.getY() + " " + p.getZ()), true);
    }
}
