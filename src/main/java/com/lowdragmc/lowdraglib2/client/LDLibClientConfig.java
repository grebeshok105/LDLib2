package com.lowdragmc.lowdraglib2.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.LDLib2;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client only settings for LDLib2, everything currently under the {@code font} section.
 * <p>
 * Fabric port: NeoForge's {@code ModConfigSpec} is replaced by a small JSON file at
 * {@code config/ldlib2-client.json} with the same public accessors. Saving fires the reload listeners, which
 * is what rebuilds the glyph atlases - the same trigger {@code ModConfigEvent.Reloading} provided upstream.
 */
public class LDLibClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ldlib2-client.json");
    private static final List<Runnable> RELOAD_LISTENERS = new CopyOnWriteArrayList<>();

    /**
     * Prefix for the translation keys the configuration UI looks up.
     */
    private static final String LANG = LDLib2.MOD_ID + ".configuration.";

    /**
     * How LDLib turns an outline into pixels.
     */
    public enum FontRenderMode {
        /**
         * Hand text to Minecraft's own font renderer and stay out of the way entirely. The baseline everything
         * else is compared against.
         */
        VANILLA,
        /**
         * Always sample a signed distance field. One atlas serves every size, scales and rotates smoothly, but
         * has no hinting so small text is softer than a hand tuned bitmap font, and sharp corners round off.
         */
        SDF,
        /**
         * Rasterize every glyph at the size it is actually drawn at, the way desktop UI toolkits do. Nothing is
         * approximated, so this is as sharp as the font gets, but each size needs its own atlas and a new size
         * has to be baked before it can be drawn. Only text beyond {@code fontRasterMaxSize} falls back, and
         * that is a memory guard rather than a judgement about how it looks.
         */
        RASTER,
        /**
         * Rasterize text that is sitting still on the pixel grid, and use the distance field for anything
         * scaled, rotated, skewed or animated, where it is both smoother and free of re-baking.
         */
        AUTO;

        public Component getTranslatedName() {
            return Component.translatable(LANG + "font.fontRenderMode." + name().toLowerCase(Locale.ROOT));
        }
    }

    private static FontRenderMode fontRenderMode = FontRenderMode.AUTO;
    private static int fontAtlasSize = 1024;
    private static int sdfEmSize = 48;
    private static double sdfSharpness = 1.0;
    private static double sdfWeight = 0.0;
    private static int fontRasterMaxSize = 256;
    private static int fontRasterEvictSeconds = 30;
    private static boolean textLayoutCache = true;

    private static boolean loaded = false;

    /**
     * Reads the config file once; safe to call during client init. Missing keys keep their defaults.
     */
    public static synchronized void init() {
        if (loaded) return;
        loaded = true;
        if (Files.isRegularFile(FILE)) {
            try {
                var json = GsonHelper.parse(Files.readString(FILE));
                fontRenderMode = parseEnum(json, "fontRenderMode", FontRenderMode.AUTO);
                fontAtlasSize = clamp(GsonHelper.getAsInt(json, "fontAtlasSize", 1024), 256, 4096);
                sdfEmSize = clamp(GsonHelper.getAsInt(json, "sdfEmSize", 48), 16, 128);
                sdfSharpness = clamp(GsonHelper.getAsDouble(json, "sdfSharpness", 1.0), 0.1, 4.0);
                sdfWeight = clamp(GsonHelper.getAsDouble(json, "sdfWeight", 0.0), -0.5, 0.5);
                fontRasterMaxSize = clamp(GsonHelper.getAsInt(json, "fontRasterMaxSize", 256), 16, 512);
                fontRasterEvictSeconds = clamp(GsonHelper.getAsInt(json, "fontRasterEvictSeconds", 30), 1, 600);
                textLayoutCache = GsonHelper.getAsBoolean(json, "textLayoutCache", true);
            } catch (Throwable e) {
                LDLib2.LOGGER.warn("Failed to read {}, using defaults", FILE, e);
            }
        }
    }

    /**
     * Subscribes to config saves; listeners fire after the file is written.
     */
    public static void addReloadListener(Runnable listener) {
        RELOAD_LISTENERS.add(listener);
    }

    private static FontRenderMode parseEnum(JsonObject json, String key, FontRenderMode fallback) {
        if (json.has(key)) {
            try {
                return FontRenderMode.valueOf(GsonHelper.getAsString(json, key).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void save() {
        var json = new JsonObject();
        json.addProperty("fontRenderMode", fontRenderMode.name());
        json.addProperty("fontAtlasSize", fontAtlasSize);
        json.addProperty("sdfEmSize", sdfEmSize);
        json.addProperty("sdfSharpness", sdfSharpness);
        json.addProperty("sdfWeight", sdfWeight);
        json.addProperty("fontRasterMaxSize", fontRasterMaxSize);
        json.addProperty("fontRasterEvictSeconds", fontRasterEvictSeconds);
        json.addProperty("textLayoutCache", textLayoutCache);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(json));
        } catch (IOException e) {
            LDLib2.LOGGER.warn("Failed to write {}", FILE, e);
        }
        for (var listener : RELOAD_LISTENERS) {
            listener.run();
        }
    }

    /**
     * @return true when LDLib renders text itself rather than handing it to the vanilla renderer
     */
    public static boolean isSmoothFont() {
        // fallback to modern ui if installed
        return fontRenderMode() != FontRenderMode.VANILLA && !LDLib2.isModLoaded("modernui");
    }

    public static int atlasSize() {
        return fontAtlasSize;
    }

    public static int emSize() {
        return sdfEmSize;
    }

    public static float sharpness() {
        return (float) sdfSharpness;
    }

    public static float weight() {
        return (float) sdfWeight;
    }

    public static boolean isTextLayoutCache() {
        return textLayoutCache;
    }

    public static FontRenderMode fontRenderMode() {
        return fontRenderMode;
    }

    /**
     * Writes the mode back to the config file, which is what the development command uses. Saving fires the
     * reload listeners, so the mode both survives a restart and rebuilds the glyph atlases.
     */
    public static void setFontRenderMode(FontRenderMode mode) {
        fontRenderMode = mode;
        save();
    }

    public static int rasterMaxSize() {
        return fontRasterMaxSize;
    }

    public static int rasterEvictSeconds() {
        return fontRasterEvictSeconds;
    }
}
