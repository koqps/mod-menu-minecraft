package com.koqps.topka.cosmetic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Body-space anchor basis for third-person cosmetics.
 * Uses body yaw rather than look/head yaw so cosmetics stay fixed to the torso.
 */
public record CosmeticAnchor(Vec3 center, Vec3 right, Vec3 back, Vec3 up) {
    public static CosmeticAnchor forBack(Player player, double verticalOffset, double backOffset) {
        double yaw = Math.toRadians(player.getVisualRotationYInDegrees());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 back = forward.scale(-1.0D);
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        AABB box = player.getBoundingBox();
        double crouch = player.isCrouching() ? -0.10D : 0.0D;
        Vec3 center = new Vec3(
                (box.minX + box.maxX) * 0.5D,
                box.maxY + verticalOffset + crouch,
                (box.minZ + box.maxZ) * 0.5D
        ).add(back.scale(backOffset));

        return new CosmeticAnchor(center, right, back, up);
    }

    public Vec3 local(double x, double y, double z) {
        return center.add(right.scale(x)).add(up.scale(y)).add(back.scale(z));
    }
}
