package com.koqps.topka.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.koqps.topka.TopkaClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("mod-menu.json");
    private TopkaConfig config = new TopkaConfig();

    public TopkaConfig get() { return config; }

    public void load() {
        if (Files.exists(PATH)) {
            try {
                TopkaConfig loaded = GSON.fromJson(Files.readString(PATH), TopkaConfig.class);
                if (loaded != null) config = loaded;
            } catch (Exception ignored) {
                config = new TopkaConfig();
            }
        }
        TopkaClient.MODULES.all().forEach(module -> {
            Boolean state = config.modules.get(module.id());
            if (state != null) module.setEnabled(state);
            config.modules.put(module.id(), module.enabled());
        });
        save();
    }

    public void save() {
        TopkaClient.MODULES.all().forEach(module -> config.modules.put(module.id(), module.enabled()));
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException ignored) { }
    }
}
