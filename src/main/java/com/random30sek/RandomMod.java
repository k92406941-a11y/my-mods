package com.random30sek;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Random;

public class RandomMod implements ModInitializer {
    public static final int PERIOD = 30 * 20; // 30 секунд в тиках

    private static final String[] NAMES = {
            "Зомби-налёт",            // 0
            "Пропажа блоков",         // 1
            "Катапульта",             // 2
            "Скорость III",           // 3
            "Супер-прыжок",           // 4
            "Слепота",                // 5
            "Криперы рядом",          // 6
            "Удары молний",           // 7
            "Дождь из алмазов",       // 8
            "Тяжёлые ноги",           // 9
            "Тьма",                   // 10
            "Голод",                  // 11
            "Горим!",                 // 12
            "Скелеты-снайперы",       // 13
            "Тошнота",                // 14
            "Полное исцеление"        // 15
    };

    private final Random rnd = new Random();
    private boolean running = false;
    private int timer = PERIOD;
    private int lastEvent = -1;
    private String eventName = "";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(TogglePayload.ID, TogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) -> {
            running = !running;
            timer = PERIOD;
            eventName = "";
            MinecraftServer server = context.server();
            Text msg = Text.literal(running ? "30sekRandom: ЗАПУЩЕН" : "30sekRandom: ОСТАНОВЛЕН");
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.sendMessage(msg, true);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (running) {
                timer--;
                if (timer <= 0) {
                    timer = PERIOD;
                    startEvent(server);
                }
            }
            if (server.getTicks() % 5 == 0) {
                StatePayload state = new StatePayload(running, eventName, timer);
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(p, state);
                }
            }
        });
    }

    private void startEvent(MinecraftServer server) {
        int idx;
        do { idx = rnd.nextInt(NAMES.length); } while (idx == lastEvent);
        lastEvent = idx;
        eventName = NAMES[idx];

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity p : players) {
            ServerWorld w = (ServerWorld) p.getEntityWorld();
            switch (idx) {
                case 0 -> { int n = 1 + rnd.nextInt(5); for (int i = 0; i < n; i++) spawnNear(EntityType.ZOMBIE, w, p); }
                case 1 -> vanishBlocks(w, p);
                case 2 -> launch(p);
                case 3 -> effect(p, StatusEffects.SPEED, 2);
                case 4 -> effect(p, StatusEffects.JUMP_BOOST, 6);
                case 5 -> effect(p, StatusEffects.BLINDNESS, 0);
                case 6 -> { int n = 1 + rnd.nextInt(3); for (int i = 0; i < n; i++) spawnNear(EntityType.CREEPER, w, p); }
                case 7 -> { for (int i = 0; i < 3; i++) spawnNear(EntityType.LIGHTNING_BOLT, w, p); }
                case 8 -> {
                    int n = 1 + rnd.nextInt(3);
                    w.spawnEntity(new ItemEntity(w, p.getX(), p.getY() + 1, p.getZ(), new ItemStack(Items.DIAMOND, n)));
                }
                case 9 -> effect(p, StatusEffects.SLOWNESS, 2);
                case 10 -> effect(p, StatusEffects.DARKNESS, 0);
                case 11 -> effect(p, StatusEffects.HUNGER, 4);
                case 12 -> p.setOnFireFor(5);
                case 13 -> { int n = 1 + rnd.nextInt(3); for (int i = 0; i < n; i++) spawnNear(EntityType.SKELETON, w, p); }
                case 14 -> effect(p, StatusEffects.NAUSEA, 0);
                case 15 -> { p.setHealth(p.getMaxHealth()); p.getHungerManager().setFoodLevel(20); }
                default -> {}
            }
        }
    }

    private void effect(ServerPlayerEntity p, RegistryEntry<StatusEffect> eff, int amp) {
        p.addStatusEffect(new StatusEffectInstance(eff, PERIOD, amp));
    }

    private <T extends Entity> void spawnNear(EntityType<T> type, ServerWorld w, ServerPlayerEntity p) {
        T e = type.create(w, SpawnReason.EVENT);
        if (e == null) return;
        double ang = rnd.nextDouble() * Math.PI * 2;
        double dist = 2 + rnd.nextDouble() * 3;
        e.setPosition(p.getX() + Math.cos(ang) * dist, p.getY(), p.getZ() + Math.sin(ang) * dist);
        w.spawnEntity(e);
    }

    private void vanishBlocks(ServerWorld w, ServerPlayerEntity p) {
        int r = 1 + rnd.nextInt(5);
        BlockPos c = p.getBlockPos();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    if (rnd.nextDouble() > 0.5) continue;
                    BlockPos pos = c.add(dx, dy, dz);
                    BlockState s = w.getBlockState(pos);
                    if (s.isAir() || s.isOf(Blocks.BEDROCK)) continue;
                    w.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                }
    }

    private void launch(ServerPlayerEntity p) {
        int h = 3 + rnd.nextInt(13); // 3..15 блоков
        double v = 0.5;
        for (double t = 0.3; t < 6; t += 0.01) {
            double y = 0, vy = t;
            while (vy > 0) { y += vy; vy = (vy - 0.08) * 0.98; }
            if (y >= h) { v = t; break; }
        }
        p.setVelocity(p.getVelocity().x, v, p.getVelocity().z);
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }
}
