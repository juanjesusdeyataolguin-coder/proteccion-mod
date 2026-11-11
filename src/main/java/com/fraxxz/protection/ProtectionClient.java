package com.fraxxz.protection;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ProtectionClient implements ClientModInitializer {
    static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.protection.menu", GLFW.GLFW_KEY_P, "key.categories.protection"));

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while (openKey.wasPressed()) {
                if (c.currentScreen == null && c.player != null) c.setScreen(new ProtectionScreen());
            }
            FreecamClient.tick(c);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            client.inGameHud.setTitleTicks(10, 70, 20);
            client.inGameHud.setSubtitle(Text.literal("§ePROTECCIÓN DE ÁREAS"));
            client.inGameHud.setTitle(Text.literal("§6MOD BY FRAXXZ"));
        }));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (FreecamClient.p1 == null && FreecamClient.p2 == null) return;
            BlockPos a = FreecamClient.p1 != null ? FreecamClient.p1 : FreecamClient.p2;
            BlockPos b = FreecamClient.p2 != null ? FreecamClient.p2 : FreecamClient.p1;
            double x1 = Math.min(a.getX(), b.getX()), y1 = Math.min(a.getY(), b.getY()), z1 = Math.min(a.getZ(), b.getZ());
            double x2 = Math.max(a.getX(), b.getX()) + 1, y2 = Math.max(a.getY(), b.getY()) + 1, z2 = Math.max(a.getZ(), b.getZ()) + 1;
            Vec3d cam = ctx.camera().getPos();
            MatrixStack m = ctx.matrixStack();
            VertexConsumer vc = ctx.consumers().getBuffer(RenderLayer.getLines());
            m.push();
            m.translate(-cam.x, -cam.y, -cam.z);
            WorldRenderer.drawBox(m, vc, x1, y1, z1, x2, y2, z2, 1f, 0.2f, 0.2f, 1f);
            m.pop();
        });
    }
}
