package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.control.DroneInput;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.network.ExitPayload;
import io.github.riveyder.shahed.network.InputPayload;
import io.github.riveyder.shahed.network.SessionPayload;
import io.github.riveyder.shahed.network.TelemetryPayload;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** One local session. The server owns authority, motion, ownership and telemetry. */
public final class FpvController {
    private static final FpvSettings SETTINGS = FpvSettings.load();
    private static UUID sessionId;
    private static UUID droneUuid;
    private static int entityId;
    private static int sequence;
    private static boolean spectating;
    private static ClientLevel sessionLevel;
    private static LocalPlayer sessionPlayer;
    private static Entity previousCamera;
    private static CameraType previousPerspective;
    private static DroneEntity drone;
    private static TelemetryPayload telemetry;
    private static double pendingYawDegrees;
    private static double pendingPitchDegrees;
    private static float cameraYaw;
    private static float cameraPitch;
    private static float cameraRoll;
    private static long lastCameraFrame;
    private static boolean cameraInitialized;
    private static long returnFadeStart;

    public static void initialize() {
        FpvKeys.initialize();
        ClientPlayNetworking.registerGlobalReceiver(SessionPayload.TYPE,
                (payload, context) -> context.client().execute(() -> receiveSession(payload)));
        ClientPlayNetworking.registerGlobalReceiver(TelemetryPayload.TYPE,
                (payload, context) -> context.client().execute(() -> receiveTelemetry(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> exit(false));
        ClientTickEvents.START_CLIENT_TICK.register(FpvController::tick);
        // Remove body UI and equipment overlays only during FPV. Chat and menus remain available.
        for (Identifier element : new Identifier[] {VanillaHudElements.MISC_OVERLAYS,
                VanillaHudElements.CROSSHAIR, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR,
                VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR,
                VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.INFO_BAR,
                VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.HELD_ITEM_TOOLTIP,
                VanillaHudElements.SPECTATOR_TOOLTIP}) {
            HudElementRegistry.replaceElement(element, original -> (graphics, delta) -> {
                if (!isActive()) original.extractRenderState(graphics, delta);
            });
        }
        HudElementRegistry.addLast(ShahedMod.id("fpv_telemetry"), FpvController::extractHud);
    }

    public static boolean acceptsReplay(UUID id) { return id.equals(sessionId); }

    public static FpvSettings settings() { return SETTINGS; }

    public static boolean isActive() {
        return sessionId != null || ReplayController.isPlaying();
    }

    public static boolean isCameraDrone(Entity entity) {
        return isActive() && entity == drone;
    }

    private static void receiveSession(SessionPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (!payload.active()) {
            if (sessionId != null && sessionId.equals(payload.sessionId())) {
                boolean destroyed=payload.reason().equals("message.shahed.session.reason.destroyed");
                exit(false);
                if(destroyed) client.gui.setScreen(new SignalLostScreen());
                if (client.player != null && !payload.reason().isBlank()) {
                    client.player.sendOverlayMessage(Component.translatable(payload.reason()));
                }
            }
            return;
        }
        if (client.player == null || client.level == null || !client.player.isAlive()) {
            if (client.getConnection() != null) ClientPlayNetworking.send(new ExitPayload(payload.sessionId()));
            return;
        }
        if (payload.sessionId().equals(sessionId)) {
            return;
        }
        exit(false);
        sessionId = payload.sessionId();
        spectating=payload.reason().equals("spectator");
        droneUuid = payload.droneUuid();
        entityId = payload.entityId();
        sessionLevel = client.level;
        sessionPlayer = client.player;
        previousCamera = client.getCameraEntity();
        previousPerspective = client.options.getCameraType();
        sequence = 0;
        clearMouse();
        // Do not let the click that connected the controller use a second item or mine a block.
        drainVanillaClicks(client);
        FpvKeys.clearSessionClicks();
    }

    private static void receiveTelemetry(TelemetryPayload payload) {
        if (sessionId != null && sessionId.equals(payload.sessionId()) && droneUuid.equals(payload.droneUuid())) {
            telemetry = payload;
            FpvHud.receive(payload);
        }
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            exit(false);
            return;
        }
        if (client.gui.screen() == null && client.isWindowActive()) {
            if(FpvKeys.HUD_MODE.consumeClick()) { SETTINGS.minimalHud=!SETTINGS.minimalHud;SETTINGS.save(); }
            if (FpvKeys.EXIT.consumeClick() && isActive()) {
                exit(true);
                return;
            }
            if (FpvKeys.SETTINGS.consumeClick()) {
                client.gui.setScreen(new FpvSettingsScreen(null, SETTINGS));
            }
        } else {
            FpvKeys.clearSessionClicks();
        }
        if (sessionId == null) return;

        if (client.level != sessionLevel || client.player != sessionPlayer || !client.player.isAlive()) {
            exit(true);
            return;
        }
        Entity candidate = client.level.getEntity(entityId);
        if (candidate instanceof DroneEntity tracked && tracked.getUUID().equals(droneUuid) && !tracked.isRemoved()) {
            drone = tracked;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            if (client.getCameraEntity() != drone) client.setCameraEntity(drone);
        }
        // A chunk-stream transition can briefly remove an entity. Retain the lens and reacquire its ID;
        // only the server's explicit session end decides whether the drone was actually destroyed.

        if(spectating) { clearMouse();return; }

        boolean canControl = drone != null && client.gui.screen() == null && client.gui.overlay() == null
                && client.isWindowActive() && client.mouseHandler.isMouseGrabbed();
        DroneInput input;
        if (canControl) {
            float yaw = axis(FpvKeys.YAW_RIGHT.isDown(), FpvKeys.YAW_LEFT.isDown())
                    + (float) (pendingYawDegrees / 4.5);
            float pitch = axis(FpvKeys.PITCH_DOWN.isDown(), FpvKeys.PITCH_UP.isDown())
                    + (float) (pendingPitchDegrees / 3.0);
            input = new DroneInput(axis(FpvKeys.FORWARD.isDown(), FpvKeys.BACKWARD.isDown()),
                    axis(FpvKeys.RIGHT.isDown(), FpvKeys.LEFT.isDown()),
                    axis(FpvKeys.UP.isDown(), FpvKeys.DOWN.isDown()),
                    Mth.clamp(yaw, -1.0F, 1.0F), Mth.clamp(pitch, -1.0F, 1.0F), FpvKeys.BRAKE.isDown());
        } else {
            // Menus/focus loss release the controls; the physical aircraft continues coasting and descending.
            input = DroneInput.IDLE;
        }
        clearMouse();
        if (client.getConnection() != null && ClientPlayNetworking.canSend(InputPayload.TYPE)) {
            ClientPlayNetworking.send(new InputPayload(sessionId, droneUuid, sequence++, input));
        } else {
            exit(false);
        }
    }

    private static float axis(boolean positive, boolean negative) {
        return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
    }

    /** Receives vanilla relative mouse deltas after sensitivity, raw input and X/Y inversion. */
    public static void mouseTurn(double vanillaX, double vanillaY) {
        Minecraft client = Minecraft.getInstance();
        if (!isActive() || client.gui.screen() != null || !client.isWindowActive()) return;
        pendingYawDegrees += vanillaX * 0.15 * SETTINGS.sensitivity;
        pendingPitchDegrees += vanillaY * 0.15 * SETTINGS.sensitivity * (SETTINGS.invertPitch ? -1 : 1);
    }

    /** Frame independent smoothing of orientation; position uses vanilla entity interpolation. */
    public static CameraPose cameraPose(float partialTick) {
        float targetYaw = drone.getYRot(partialTick);
        float targetPitch = drone.getXRot(partialTick);
        float targetRoll = drone.getRoll(partialTick);
        long now = System.nanoTime();
        if (!cameraInitialized) {
            cameraYaw = targetYaw;
            cameraPitch = targetPitch;
            cameraRoll = targetRoll;
            cameraInitialized = true;
        } else {
            double dt = Math.clamp((now - lastCameraFrame) / 1_000_000_000.0, 0.0, 0.25);
            float blend = SETTINGS.smoothingSeconds <= 0 ? 1 : (float) (1 - Math.exp(-dt / SETTINGS.smoothingSeconds));
            cameraYaw += Mth.wrapDegrees(targetYaw - cameraYaw) * blend;
            cameraPitch = Mth.lerp(blend, cameraPitch, targetPitch);
            cameraRoll = Mth.lerp(blend, cameraRoll, targetRoll);
        }
        lastCameraFrame = now;
        return new CameraPose(cameraYaw, cameraPitch, cameraRoll);
    }

    public static void exit(boolean notifyServer) {
        ReplayController.stop(notifyServer);
        if (sessionId == null) return;
        Minecraft client = Minecraft.getInstance();
        UUID endingSession = sessionId;
        sessionId = null;
        if (notifyServer && client.getConnection() != null && ClientPlayNetworking.canSend(ExitPayload.TYPE)) {
            ClientPlayNetworking.send(new ExitPayload(endingSession));
        }
        Entity restore = previousCamera;
        if (restore == null || restore.isRemoved() || restore.level() != client.level || restore == drone
                || restore == sessionPlayer && client.player != sessionPlayer) {
            restore = client.player;
        }
        client.setCameraEntity(restore);
        if (previousPerspective != null) client.options.setCameraType(previousPerspective);
        drone = null;
        droneUuid = null;
        sessionLevel = null;
        sessionPlayer = null;
        previousCamera = null;
        previousPerspective = null;
        telemetry = null;
        FpvHud.reset();
        cameraInitialized = false;
        clearMouse();
        drainVanillaClicks(client);
        FpvKeys.clearSessionClicks();
        KeyMapping.releaseAll();
    }

    private static void clearMouse() {
        pendingYawDegrees = pendingPitchDegrees = 0;
    }

    /** Also called from the keybind mixin to keep suppressed actions from queuing after exit. */
    public static void drainVanillaClicks(Minecraft client) {
        for (var key : client.options.keyMappings) {
            if (FpvKeys.isSessionAction(key)) continue;
            while (key.consumeClick()) { }
        }
    }

    public static void beginReturnFade() { returnFadeStart=System.nanoTime(); }

    private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if(returnFadeStart!=0) {
            float remaining=1-(System.nanoTime()-returnFadeStart)/450_000_000F;
            if(remaining<=0) returnFadeStart=0;
            else graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),((int)(remaining*255)<<24)|0x050908);
        }
        if (!isActive()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.gui.hud.isHidden()) return;
        if(ReplayController.isPlaying()) {
            graphics.text(client.font,Component.literal(ReplayController.label()),10,10,0xFFB9FFD1);
            return;
        }
        FpvHud.render(graphics,spectating);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public record CameraPose(float yaw, float pitch, float roll) { }

    private FpvController() {
    }
}
