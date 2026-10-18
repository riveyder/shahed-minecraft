package io.github.riveyder.shahed.race;

import com.google.gson.*;
import io.github.riveyder.shahed.ShahedMod;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Atomic world-local storage. A corrupt file is never silently replaced by an empty database. */
public final class RaceStorage {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    public boolean writable=true;
    public RaceStorage(MinecraftServer server) {file=server.getWorldPath(LevelResource.ROOT).resolve("shahed-races.json");}
    public Map<String,RaceCourse> load() {
        var result=new LinkedHashMap<String,RaceCourse>();
        if(!Files.exists(file))return result;
        try {
            if(Files.size(file)>16*1024*1024)throw new IllegalArgumentException("Race data too large");
            var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1)throw new IllegalArgumentException("Race schema version");
            var courses=root.getAsJsonArray("courses");
            if(courses.size()>RaceCourse.MAX_COURSES)throw new IllegalArgumentException("Too many courses");
            for(var row:courses) {var course=JSON.fromJson(row,RaceCourse.class);
                if(result.putIfAbsent(course.id(),course)!=null)throw new IllegalArgumentException("Duplicate course");}
            return result;
        } catch(Exception error) {
            writable=false;ShahedMod.LOGGER.error("Cannot load race data {}; preserving file and disabling writes",file,error);return new LinkedHashMap<>();
        }
    }
    public boolean save(Map<String,RaceCourse> courses) {
        if(!writable||courses.size()>RaceCourse.MAX_COURSES)return false;
        try {
            var root=new JsonObject();root.addProperty("version",1);root.add("courses",JSON.toJsonTree(courses.values()));
            var tmp=file.resolveSibling(file.getFileName()+".tmp");Files.createDirectories(file.getParent());
            Files.writeString(tmp,JSON.toJson(root)+System.lineSeparator());
            try {Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e) {Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
            return true;
        }catch(Exception error){ShahedMod.LOGGER.error("Could not persist race data",error);return false;}
    }
}
