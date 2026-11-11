package com.fraxxz.protection;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ProtectionScreen extends Screen {
    private TextFieldWidget nameField;
    private int topY;

    public ProtectionScreen() { super(Text.literal("Protección de áreas")); }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = Math.max(24, height / 2 - 66);
        topY = y;

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
        }).dimensions(x, y + 22, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Reiniciar selección"), b -> {
            FreecamClient.p1 = null; FreecamClient.p2 = null; close();
        }).dimensions(x, y + 44, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Borrar mi área donde estoy"), b -> {
            ClientPlayNetworking.send(ProtectionMod.REMOVE, PacketByteBufs.create()); close();
        }).dimensions(x, y + 66, 200, 20).build());

        nameField = new TextFieldWidget(textRenderer, x, y + 88, 200, 20, Text.literal("Jugador"));
        nameField.setMaxLength(16);
        nameField.setSuggestion("Nombre del amigo");
        nameField.setChangedListener(s -> nameField.setSuggestion(s.isEmpty() ? "Nombre del amigo" : ""));
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Dar permiso"), b -> sendTrust(true))
            .dimensions(x, y + 110, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Quitar permiso"), b -> sendTrust(false))
            .dimensions(x + 102, y + 110, 98, 20).build());
    }

    private void sendTrust(boolean add) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            if (client.player != null) client.player.sendMessage(Text.literal("§cEscribe el nombre del jugador"), false);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(name, 32);
        buf.writeBoolean(add);
        ClientPlayNetworking.send(ProtectionMod.TRUST, buf);
        nameField.setText("");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("MOD BY FRAXXZ"), width / 2, topY - 14, 0xFFAA00);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P && !nameField.isFocused()) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
