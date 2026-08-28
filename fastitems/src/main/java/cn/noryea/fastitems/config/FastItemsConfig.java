package cn.noryea.fastitems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.henny.hugoutils.client.config.ConfigManager;
import dev.henny.hugoutils.client.config.ConfigSection;
import dev.henny.hugoutils.client.config.ItemFilter;

import java.nio.file.Path;

public class FastItemsConfig {
    public static boolean enable = false;
    public static boolean castShadows = true;
    public static boolean renderSidesOfItems = false;
    public static boolean affect3DModels = true;
    public static final ItemFilter filter = new ItemFilter();

    public static boolean isActive() {
        return enable;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static void init(Path configDir) {
        ConfigManager.INSTANCE.registerSection(new ConfigSection() {
            @Override
            public String getId() {
                return "fastitems";
            }

            @Override
            public void read(JsonObject json) {
                apply(GSON.fromJson(json, ConfigData.class));
            }

            @Override
            public JsonObject write() {
                return GSON.toJsonTree(snapshot()).getAsJsonObject();
            }
        }, configDir.resolve("fastitems.json"));
    }

    public static void load() {
    }

    public static void save() {
        ConfigManager.INSTANCE.requestSave();
    }

    private static void apply(ConfigData data) {
        if (data == null) return;
        enable = data.enable;
        castShadows = data.castShadows;
        renderSidesOfItems = data.renderSidesOfItems;
        affect3DModels = data.affect3DModels;
        if (data.filter != null) {
            filter.copyFrom(data.filter);
            filter.clamp();
        }
    }

    private static ConfigData snapshot() {
        filter.clamp();
        ConfigData data = new ConfigData();
        data.enable = enable;
        data.castShadows = castShadows;
        data.renderSidesOfItems = renderSidesOfItems;
        data.affect3DModels = affect3DModels;
        data.filter.copyFrom(filter);
        return data;
    }

    private static class ConfigData {
        boolean enable = false;
        boolean castShadows = true;
        boolean renderSidesOfItems = false;
        boolean affect3DModels = true;
        ItemFilter filter = new ItemFilter();
    }
}
