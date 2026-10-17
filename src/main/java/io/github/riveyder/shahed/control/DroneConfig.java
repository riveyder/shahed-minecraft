package io.github.riveyder.shahed.control;

import com.google.gson.GsonBuilder;
import io.github.riveyder.shahed.ShahedMod;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

/** Server-owned gameplay policy, loaded once at startup. Safe defaults on invalid input. */
public final class DroneConfig {
    public static final Settings DEFAULTS = new Settings(false, 2.0F);
    private static Settings settings=DEFAULTS;
    public record Settings(boolean destroyBlocks,float explosionPower) {
        public Settings sanitized() {
            return new Settings(destroyBlocks,Float.isFinite(explosionPower)
                    ? Math.clamp(explosionPower,0.5F,4.0F):2.0F);
        }
    }
    public static Settings get() { return settings; }
    public static void load() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("shahed-server.json");
        var gson=new GsonBuilder().setPrettyPrinting().create();
        settings=DEFAULTS;
        try {
            if(Files.isRegularFile(path)) {
                // Read onto defaults: an omitted explosionPower must not become zero.
                var json=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                settings=new Settings(json.has("destroyBlocks") && json.get("destroyBlocks").getAsBoolean(),
                        json.has("explosionPower")?json.get("explosionPower").getAsFloat():2.0F).sanitized();
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path,gson.toJson(DEFAULTS)+System.lineSeparator());
            }
        } catch(Exception exception) {
            settings=DEFAULTS;
            ShahedMod.LOGGER.warn("Could not load drone server settings; using safe defaults",exception);
        }
    }
    private DroneConfig() { }
}
