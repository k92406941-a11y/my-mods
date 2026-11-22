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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class RandomMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("30sekRandom");
    public static final int PERIOD = 30 * 20; // 30 секунд в тиках

    private static final String[] NAMES = {
            "Зомби-налёт",            // 0
            "Пропажа блоков",         // 1
            "Катапульта",             // 2
            "Скорость III",           // 3  (хорошее)
            "Супер-прыжок",           // 4  (хорошее)
            "Слепота",                // 5
            "Криперы рядом",          // 6
            "Удары молний",           // 7
            "Дождь из алмазов",       // 8  (хорошее)
            "Тяжёлые ноги",           // 9
            "Тьма",                   // 10
            "Голод",                  // 11
            "Горим!",                 // 12
            "Скелеты-снайперы",       // 13
            "Тошнота",                // 14
            "Полное исцеление",       // 15 (хорошее)
            "Яд",                     // 16
            "Слабость",               // 17
            "Усталость",              // 18
            "Левитация",              // 19
            "Иссушение",              // 20
            "TNT рядом",              // 21
            "Пауки",                  // 22
            "Ведьмы",                 // 23
            "Паутина вокруг",         // 24
            "Лава под ногами",        // 25
            "Потеря здоровья",        // 26
            "Потеря еды"              // 27
    };

    // Вес события: чем больше число, тем чаще оно выпадает.
    // Плохие = 3, хорошие = 2. Хочешь ещё больше плохих — поставь хорошим 1.
    private static final int[] WEIGHTS = new int[NAMES.length];
    static {
        for (int i = 0; i < WEIGHTS.length; i++) WEIGHTS[i] = 3;
        WEIGHTS[3] = 2;
        WEIGHTS[4] = 2;
        WEIGHTS[8] = 2;
        WEIGHTS[15] = 2;
    }

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
            LOGGER.info("Toggle received, running = {}", running);
            MinecraftServer server = context.server();
            Text msg = Text.literal(running ? "30sekRandom: ЗАПУЩЕН (первое событие через 30 сек)" : "30sekRandom: ОСТАНОВЛЕН");
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.sendMessage(msg, false);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (running) {
                timer--;
                if (timer <= 0) {
                    timer = PERIOD;
                    try {
                        startEvent(server);
                    } catch (Exception ex) {
                        LOGGER.error("Event failed", ex);
                    }
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

    private int pickEvent() {
        int total = 0;
        for (int i = 0; i < NAMES.length; i++) {
            if (i != lastEvent) total += WEIGHTS[i];
        }
        int roll = rnd.nextInt(total);
        for (int i = 0; i < NAMES.length; i++) {
            if (i == lastEvent) continue;
            roll -= WEIGHTS[i];
            if (roll < 0) return i;
        }
        return 0;
    }

    private void startEvent(MinecraftServer server) {
        int idx = pickEvent();
        lastEvent = idx;
        eventName = NAMES[idx];
        LOGGER.info("Event started: {}", eventName);

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity p : players) {
            p.sendMessage(Text.literal("Событие: " + eventName), false);
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
                case 16 -> effect(p, StatusEffects.POISON, 1, 200);
                case 17 -> effect(p, StatusEffects.WEAKNESS, 1);
                case 18 -> effect(p, StatusEffects.MINING_FATIGUE, 2);
                case 19 -> effect(p, StatusEffects.LEVITATION, 1, 100);
                case 20 -> effect(p, StatusEffects.WITHER, 0, 100);
                case 21 -> { int n = 1 + rnd.nextInt(3); for (int i = 0; i < n; i++) spawnNear(EntityType.TNT, w, p); }
                case 22 -> { int n = 2 + rnd.nextInt(4); for (int i = 0; i < n; i++) spawnNear(EntityType.SPIDER, w, p); }
                case 23 -> { int n = 1 + rnd.nextInt(2); for (int i = 0; i < n; i++) spawnNear(EntityType.WITCH, w, p); }
                case 24 -> cobwebs(w, p);
                case 25 -> w.setBlockState(p.getBlockPos().down(), Blocks.LAVA.getDefaultState(), 3);
                case 26 -> p.setHealth(Math.max(1.0f, p.getHealth() / 2));
                case 27 -> p.getHungerManager().setFoodLevel(2);
                default -> {}
            }
        }
    }

    private void effect(ServerPlayerEntity p, RegistryEntry<StatusEffect> eff, int amp) {
        effect(p, eff, amp, PERIOD);
    }

    private void effect(ServerPlayerEntity p, RegistryEntry<StatusEffect> eff, int amp, int ticks) {
        p.addStatusEffect(new StatusEffectInstance(eff, ticks, amp));
    }

    private <T extends Entity> void spawnNear(EntityType<T> type, ServerWorld w, ServerPlayerEntity p) {
        T e = type.create(w, SpawnReason.EVENT);
        if (e == null) {
            LOGGER.warn("Could not create entity {}", type);
            return;
        }
        double ang = rnd.nextDouble() * Math.PI * 2;
        double dist = 2 + rnd.nextDouble() * 3;
        e.setPosition(p.getX() + Math.cos(ang) * dist, p.getY(), p.getZ() + Math.sin(ang) * dist);
        w.spawnEntity(e);
    }

    private void cobwebs(ServerWorld w, ServerPlayerEntity p) {
        BlockPos c = p.getBlockPos();
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = 0; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = c.add(dx, dy, dz);
                    if (w.getBlockState(pos).isAir()) {
                        w.setBlockState(pos, Blocks.COBWEB.getDefaultState(), 3);
                    }
                }
    }

    private void vanishBlocks(ServerWorld w, ServerPlayerEntity p) {
        int r = 1 + rnd.nextInt(5);
        BlockPos c = p.getBlockPos();
        int removed = 0;
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    if (rnd.nextDouble() > 0.5) continue;
                    BlockPos pos = c.add(dx, dy, dz);
                    BlockState s = w.getBlockState(pos);
                    if (s.isAir() || s.isOf(Blocks.BEDROCK)) continue;
                    w.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                    removed++;
                }
        LOGGER.info("Vanished {} blocks (radius {})", removed, r);
        p.sendMessage(Text.literal("Пропало блоков: " + removed + " (радиус " + r + ")"), false);
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
