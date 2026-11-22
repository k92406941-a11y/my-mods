package com.random30sek;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class RandomModClient implements ClientModInitializer {
    private static boolean running = false;
    private static String eventName = "";
    private static int ticksLeft = RandomMod.PERIOD;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("random30sek", "main"));

        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.random30sek.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB,
                category));

        // Запасная клавиша на случай, если TAB конфликтует со списком игроков
        KeyBinding altKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.random30sek.toggle_alt",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean pressed = false;
            while (toggleKey.wasPressed()) pressed = true;
            while (altKey.wasPressed()) pressed = true;
            if (pressed && client.player != null) {
                client.player.sendMessage(Text.literal("30sekRandom: клавиша нажата"), false);
                ClientPlayNetworking.send(new TogglePayload());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StatePayload.ID, (payload, context) -> {
            running = payload.running();
            eventName = payload.eventName();
            ticksLeft = payload.ticksLeft();
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            var tr = mc.textRenderer;
            int x = 6, y = 6;

            String top;
            if (!running) top = "30sekRandom: нажми TAB или R для старта";
            else top = "Событие: " + (eventName.isEmpty() ? "—" : eventName);
            String bottom = running ? "Следующее через: " + (ticksLeft + 19) / 20 + " c" : "";

            ctx.drawTextWithShadow(tr, Text.literal(top), x, y, 0xFFFFFF55);
            if (running) ctx.drawTextWithShadow(tr, Text.literal(bottom), x, y + 11, 0xFFFFFFFF);
        });
    }
}
