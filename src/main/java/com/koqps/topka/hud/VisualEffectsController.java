package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public final class VisualEffectsController {
    public record TrailPoint(Vec3 position, long createdAt) { }
    public record JumpRing(Vec3 position, long createdAt) { }

    private static final Deque<TrailPoint> TRAIL = new ArrayDeque<>();
    private static final Deque<JumpRing> RINGS = new ArrayDeque<>();
    private static final Random RANDOM = new Random();

    private static boolean lastOnGround = true;
    private static Vec3 lastTrailPoint = Vec3.ZERO;

    private VisualEffectsController() { }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            Minecraft client = Minecraft.getInstance();
            if (player == client.player && TopkaClient.MODULES.byId("hit_particles").enabled()) {
                spawnHitParticles(entity);
            }
            return InteractionResult.PASS;
        });
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            clear();
            return;
        }

        long now = System.currentTimeMillis();
        boolean onGround = player.onGround();
        boolean jumped = lastOnGround && !onGround && player.getDeltaMovement().y > 0.08D;

        if (jumped) {
            if (TopkaClient.MODULES.byId("jump_circles").enabled()) {
                RINGS.addLast(new JumpRing(new Vec3(player.getX(), player.getY() + 0.02D, player.getZ()), now));
            }
            if (TopkaClient.MODULES.byId("jump_particles").enabled()) {
                spawnJumpParticles(player);
            }
        }
        lastOnGround = onGround;

        if (TopkaClient.MODULES.byId("trails").enabled()) {
            Vec3 pos = new Vec3(player.getX(), player.getY() + 0.05D, player.getZ());
            if (lastTrailPoint == Vec3.ZERO || pos.distanceToSqr(lastTrailPoint) > 0.015D) {
                TRAIL.addLast(new TrailPoint(pos, now));
                lastTrailPoint = pos;
            }
        } else {
            TRAIL.clear();
            lastTrailPoint = Vec3.ZERO;
        }

        long trailCutoff = now - Math.max(250L, TopkaClient.CONFIG.get().trailLifetimeMs);
        while (!TRAIL.isEmpty() && TRAIL.peekFirst().createdAt() < trailCutoff) TRAIL.removeFirst();

        long ringCutoff = now - Math.max(250L, TopkaClient.CONFIG.get().jumpCircleLifetimeMs);
        while (!RINGS.isEmpty() && RINGS.peekFirst().createdAt() < ringCutoff) RINGS.removeFirst();
    }

    public static List<TrailPoint> trailSnapshot() {
        return new ArrayList<>(TRAIL);
    }

    public static List<JumpRing> ringSnapshot() {
        return new ArrayList<>(RINGS);
    }

    public static void clear() {
        TRAIL.clear();
        RINGS.clear();
        lastTrailPoint = Vec3.ZERO;
        lastOnGround = true;
    }

    private static void spawnHitParticles(Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        int count = Math.clamp(TopkaClient.CONFIG.get().hitParticleCount, 1, 64);
        int style = Math.clamp(TopkaClient.CONFIG.get().hitParticleStyle, 0, 3);
        for (int i = 0; i < count; i++) {
            double px = target.getX() + (RANDOM.nextDouble() - 0.5D) * Math.max(0.4D, target.getBbWidth());
            double py = target.getY() + 0.25D + RANDOM.nextDouble() * Math.max(0.5D, target.getBbHeight() * 0.8D);
            double pz = target.getZ() + (RANDOM.nextDouble() - 0.5D) * Math.max(0.4D, target.getBbWidth());
            double vx = (RANDOM.nextDouble() - 0.5D) * 0.10D;
            double vy = 0.03D + RANDOM.nextDouble() * 0.10D;
            double vz = (RANDOM.nextDouble() - 0.5D) * 0.10D;
            var type = switch (style) {
                case 1 -> ParticleTypes.HEART;
                case 2 -> ParticleTypes.END_ROD;
                case 3 -> ParticleTypes.CRIT;
                default -> (i & 3) == 0 ? ParticleTypes.HEART : ParticleTypes.CRIT;
            };
            client.level.addParticle(type, px, py, pz, vx, vy, vz);
        }
    }

    private static void spawnJumpParticles(Player player) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        int count = Math.clamp(TopkaClient.CONFIG.get().jumpParticleCount, 1, 48);
        int style = Math.clamp(TopkaClient.CONFIG.get().jumpParticleStyle, 0, 3);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            double radius = 0.35D + RANDOM.nextDouble() * 0.22D;
            double px = player.getX() + Math.cos(angle) * radius;
            double pz = player.getZ() + Math.sin(angle) * radius;
            var type = switch (style) {
                case 1 -> ParticleTypes.HEART;
                case 2 -> ParticleTypes.END_ROD;
                case 3 -> ParticleTypes.CRIT;
                default -> (i % 3 == 0) ? ParticleTypes.END_ROD : ParticleTypes.CRIT;
            };
            client.level.addParticle(
                    type,
                    px, player.getY() + 0.08D, pz,
                    Math.cos(angle) * 0.02D,
                    0.03D + RANDOM.nextDouble() * 0.03D,
                    Math.sin(angle) * 0.02D
            );
        }
    }
}
