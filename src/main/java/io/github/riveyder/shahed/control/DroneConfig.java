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
    public static final Settings DEFAULTS = new Settings(true,19,true,1,true,true,4096);
    private static Settings settings=DEFAULTS;
    public record Settings(boolean destroyBlocks,float explosionPower,boolean entityDamage,float damageScale,
                           boolean particles,boolean sound,int maxBlocks,boolean stabilization,
                           float dragMultiplier,int activeLimit,boolean spectators,int telemetryInterval,
                           boolean realFire,boolean fireSpread,float effectScale,int audioRange) {
        public Settings(boolean blocks,float power,boolean damage,float scale,boolean particles,boolean sound,int cap,
                        boolean stabilization,float drag,int active,boolean spectators,int telemetry) {
            this(blocks,power,damage,scale,particles,sound,cap,stabilization,drag,active,spectators,telemetry,false,false,2.5F,1024);
        }
        public Settings(boolean blocks,float power,boolean damage,float scale,boolean particles,boolean sound,int cap) {
            this(blocks,power,damage,scale,particles,sound,cap,true,1,16,true,5);
        }
        public Settings(boolean blocks,float power) {this(blocks,power,true,1,true,true,256);}
        public Settings sanitized() {
            return new Settings(destroyBlocks,Float.isFinite(explosionPower)?Math.clamp(explosionPower,1,32):19,
                    entityDamage,Float.isFinite(damageScale)?Math.clamp(damageScale,0,1):.25F,particles,sound,Math.clamp(maxBlocks,0,16384),
                    stabilization,Float.isFinite(dragMultiplier)?Math.clamp(dragMultiplier,.5F,2):1,
                    Math.clamp(activeLimit,1,64),spectators,Math.clamp(telemetryInterval,1,20),
                    realFire&&destroyBlocks,fireSpread&&realFire&&destroyBlocks,
                    Float.isFinite(effectScale)?Math.clamp(effectScale,1,4):2.5F,Math.clamp(audioRange,64,2048));
        }
        public Settings preset(int preset) {var explosion=switch(preset) {
            case 1 -> DEFAULTS;
            case 2 -> new Settings(true,28,true,1,true,true,12288);
            default -> new Settings(false,19,false,0,true,true,0);
        };return new Settings(explosion.destroyBlocks,explosion.explosionPower,explosion.entityDamage,
                explosion.damageScale,explosion.particles,explosion.sound,explosion.maxBlocks,
                stabilization,dragMultiplier,activeLimit,spectators,telemetryInterval,
                preset!=0&&realFire,preset!=0&&fireSpread,preset==2?4:2.5F,audioRange);}
    }
    public static Settings get() { return settings; }
    public static boolean needsConfirmation(Settings s) {
        s=s.sanitized();return s.realFire() || s.destroyBlocks() && (s.explosionPower()>8 || s.maxBlocks()>512);
    }
    public static void load() {
        settings=DEFAULTS;
        try {
            if(Files.isRegularFile(FILE)) settings=read(FILE); else write(FILE,DEFAULTS);
        } catch(Exception exception) {
            settings=DEFAULTS.preset(0);
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
