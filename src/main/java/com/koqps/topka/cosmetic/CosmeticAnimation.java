package com.koqps.topka.cosmetic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Shared movement-aware cosmetic animation values.
 */
public record CosmeticAnimation(double flap, double spreadBoost, double sway, double lift) {
    public static CosmeticAnimation sample(Player player, float speed, float amount) {
        Vec3 velocity = player.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        double stateBoost = 0.0D;
        if (player.isSprinting()) stateBoost += 0.11D;
        if (!player.onGround()) stateBoost += 0.18D;

        double time = System.currentTimeMillis() / 1000.0D;
        double phase = time * Math.max(0.10F, speed) * Math.PI * 2.0D;
        double flap = Math.sin(phase) * amount;
        double sway = Math.sin(phase * 0.52D + 0.8D) * (0.025D + horizontal * 0.09D);
        double spreadBoost = Math.min(0.34D, horizontal * 0.30D + stateBoost);
        double lift = !player.onGround() ? Math.clamp(-velocity.y * 0.16D, -0.10D, 0.18D) : 0.0D;

        if (player.isCrouching()) {
            flap *= 0.45D;
            spreadBoost -= 0.10D;
        }

        return new CosmeticAnimation(flap, spreadBoost, sway, lift);
    }
}
