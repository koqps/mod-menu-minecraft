package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public final class AutoGgController {
    private static long lastSentAt;

    private AutoGgController() { }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> lastSentAt = 0L);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSentAt = 0L);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !TopkaClient.MODULES.byId("auto_gg").enabled()) return;

            String text = message.getString().toLowerCase(Locale.ROOT);
            if (!looksLikeMatchEnd(text)) return;

            long now = System.currentTimeMillis();
            long cooldown = Math.max(5_000L, TopkaClient.CONFIG.get().autoGgCooldownMs);
            if (now - lastSentAt < cooldown) return;

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.player.connection == null) return;

            String configured = TopkaClient.CONFIG.get().autoGgMessage;
            String chat = configured == null || configured.isBlank() ? "gg" : configured.trim();
            if (chat.length() > 80) chat = chat.substring(0, 80);

            final String messageToSend = chat;
            lastSentAt = now;
            client.execute(() -> {
                if (client.player != null && client.player.connection != null
                        && TopkaClient.MODULES.byId("auto_gg").enabled()) {
                    client.player.connection.sendChat(messageToSend);
                }
            });
        });
    }

    private static boolean looksLikeMatchEnd(String text) {
        return text.contains("victory")
                || text.contains("you won")
                || text.contains("winner")
                || text.contains("game over")
                || text.contains("match over")
                || text.contains("won the game")
                || text.contains("1st place")
                || text.contains("first place");
    }
}
