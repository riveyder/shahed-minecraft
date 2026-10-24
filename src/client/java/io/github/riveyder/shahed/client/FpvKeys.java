package io.github.riveyder.shahed.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.riveyder.shahed.ShahedMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Ordinary vanilla mappings: all controls can be rebound in Options > Controls. */
public final class FpvKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ShahedMod.id("fpv"));
    public static final KeyMapping FORWARD = key("forward", GLFW.GLFW_KEY_W);
    public static final KeyMapping BACKWARD = key("backward", GLFW.GLFW_KEY_S);
    public static final KeyMapping LEFT = key("left", GLFW.GLFW_KEY_A);
    public static final KeyMapping RIGHT = key("right", GLFW.GLFW_KEY_D);
    public static final KeyMapping UP = key("up", GLFW.GLFW_KEY_SPACE);
    public static final KeyMapping DOWN = key("down", GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyMapping BRAKE = key("brake", GLFW.GLFW_KEY_LEFT_SHIFT);
    public static final KeyMapping YAW_LEFT = key("yaw_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping YAW_RIGHT = key("yaw_right", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping PITCH_UP = key("pitch_up", GLFW.GLFW_KEY_UP);
    public static final KeyMapping PITCH_DOWN = key("pitch_down", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping RELEASE_PAYLOAD = key("release_payload", GLFW.GLFW_KEY_G);
    public static final KeyMapping EXIT = key("exit", GLFW.GLFW_KEY_R);
    // O opens Minecraft 26.2's friends list; keep the default usable without a conflict.
    public static final KeyMapping SETTINGS = key("settings", GLFW.GLFW_KEY_K);
    public static final KeyMapping HUD_MODE = key("hud_mode", GLFW.GLFW_KEY_H);

    private static KeyMapping key(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.shahed." + name,
                InputConstants.Type.KEYSYM, key, CATEGORY));
    }

    public static void initialize() {
        // Class initialization registers the mappings exactly once.
    }

    public static boolean isSessionAction(KeyMapping mapping) {
        return mapping == EXIT || mapping == SETTINGS || mapping == HUD_MODE || mapping == RELEASE_PAYLOAD;
    }

    public static void clearSessionClicks() {
        while (RELEASE_PAYLOAD.consumeClick()) { }
        while (EXIT.consumeClick()) { }
        while (SETTINGS.consumeClick()) { }
        while (HUD_MODE.consumeClick()) { }
    }

    private FpvKeys() {
    }
}
