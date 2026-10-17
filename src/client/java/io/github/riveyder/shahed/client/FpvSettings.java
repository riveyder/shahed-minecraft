package io.github.riveyder.shahed.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.riveyder.shahed.ShahedMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/** Local preferences only: no server state or controller credentials are stored here. */
public final class FpvSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shahed-fpv.json");
    public double sensitivity = 1.0;
    public double smoothingSeconds = 0.12;
    public boolean invertPitch;

    public static FpvSettings load() {
        if (Files.isRegularFile(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) {
                FpvSettings settings = GSON.fromJson(reader, FpvSettings.class);
                if (settings != null) {
                    settings.sanitize();
                    return settings;
                }
            } catch (IOException | RuntimeException exception) {
                ShahedMod.LOGGER.warn("Could not read FPV preferences; using defaults", exception);
            }
        }
        return new FpvSettings();
    }

    public void save() {
        sanitize();
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(temporary, GSON.toJson(this) + System.lineSeparator());
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            ShahedMod.LOGGER.warn("Could not save FPV preferences", exception);
        }
    }

    private void sanitize() {
        sensitivity = Double.isFinite(sensitivity) ? Math.clamp(sensitivity, 0.1, 3.0) : 1.0;
        smoothingSeconds = Double.isFinite(smoothingSeconds) ? Math.clamp(smoothingSeconds, 0.0, 0.5) : 0.12;
    }
}
