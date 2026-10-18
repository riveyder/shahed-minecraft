package io.github.riveyder.shahed.control;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.github.riveyder.shahed.ShahedMod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;

/** Server-thread policy. Persist before publishing live updates; migrate missing fields safely. */
public final class DroneConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("shahed-server.json");
    public static final Settings DEFAULTS = new Settings(false,8,true,.25F,true,true,256);
    private static Settings settings=DEFAULTS;
    public record Settings(boolean destroyBlocks,float explosionPower,boolean entityDamage,float damageScale,
                           boolean particles,boolean sound,int maxBlocks,boolean stabilization,
                           float dragMultiplier,int activeLimit,boolean spectators,int telemetryInterval) {
        public Settings(boolean blocks,float power,boolean damage,float scale,boolean particles,boolean sound,int cap) {
            this(blocks,power,damage,scale,particles,sound,cap,true,1,16,true,5);
        }
        public Settings(boolean blocks,float power) {this(blocks,power,true,1,true,true,256);}
        public Settings sanitized() {
            return new Settings(destroyBlocks,Float.isFinite(explosionPower)?Math.clamp(explosionPower,1,12):8,
                    entityDamage,Float.isFinite(damageScale)?Math.clamp(damageScale,0,1):.25F,particles,sound,Math.clamp(maxBlocks,0,4096),
                    stabilization,Float.isFinite(dragMultiplier)?Math.clamp(dragMultiplier,.5F,2):1,
                    Math.clamp(activeLimit,1,64),spectators,Math.clamp(telemetryInterval,1,20));
        }
        public Settings preset(int preset) {var explosion=switch(preset) {
            case 1 -> new Settings(true,6,true,1,true,true,256);
            case 2 -> new Settings(true,12,true,1,true,true,1024);
            default -> DEFAULTS;
        };return new Settings(explosion.destroyBlocks,explosion.explosionPower,explosion.entityDamage,
                explosion.damageScale,explosion.particles,explosion.sound,explosion.maxBlocks,
                stabilization,dragMultiplier,activeLimit,spectators,telemetryInterval);}
    }
    public static Settings get() { return settings; }
    public static void load() {
        settings=DEFAULTS;
        try {
            if(Files.isRegularFile(FILE)) settings=read(FILE); else write(FILE,DEFAULTS);
        } catch(Exception exception) {
            settings=DEFAULTS;
            ShahedMod.LOGGER.warn("Could not load drone server settings; using safe defaults",exception);
        }
    }
    public static Settings read(Path file) throws IOException {
        var merged=GSON.toJsonTree(DEFAULTS).getAsJsonObject();
        var incoming=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        for(var e:incoming.entrySet()) if(merged.has(e.getKey())&&!e.getValue().isJsonNull()) merged.add(e.getKey(),e.getValue());
        return GSON.fromJson(merged,Settings.class).sanitized();
    }
    public static void write(Path file,Settings candidate) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        Files.writeString(tmp,GSON.toJson(candidate.sanitized())+System.lineSeparator());
        try {Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(java.nio.file.AtomicMoveNotSupportedException e) {Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
    }
    public static boolean update(Settings candidate) {
        try {var safe=candidate.sanitized();write(FILE,safe);settings=safe;return true;}
        catch(IOException|RuntimeException e) {ShahedMod.LOGGER.warn("Could not save drone policy",e);return false;}
    }
    private DroneConfig() { }
}
