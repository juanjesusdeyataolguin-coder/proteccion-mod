package com.fraxxz.protection;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ProtectionScreen extends Screen {
    public ProtectionScreen() { super(Text.literal("Protección de áreas")); }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 2 - 55;
        addDrawableChild(ButtonWidget.builder(
            Text.literal(FreecamClient.active ? "Salir de cámara libre" : "Cámara libre (marcar área)"),
            b -> { FreecamClient.toggle(); close(); }).dimensions(x, y, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Proteger área marcada (1 cristal)"), b -> {
            if (FreecamClient.p1 == null || FreecamClient.p2 == null) {
                if (client.player != null) client.player.sendMessage(Text.literal("§cMarca las 2 posiciones primero"), false);
            } else {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBlockPos(FreecamClient.p1);
                buf.writeBlockPos(FreecamClient.p2);
                ClientPlayNetworking.send(ProtectionMod.CLAIM, buf);
                FreecamClient.exit(true);
            }
            close();
        }).dimensions(x, y + 25, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reiniciar selección"), b -> {
            FreecamClient.p1 = null; FreecamClient.p2 = null; close();
        }).dimensions(x, y + 50, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Borrar mi área donde estoy"), b -> {
            ClientPlayNetworking.send(ProtectionMod.REMOVE, PacketByteBufs.create()); close();
        }).dimensions(x, y + 75, 200, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
