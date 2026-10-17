package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.network.InputPayload;
import io.github.riveyder.shahed.network.SessionPayload;
import io.github.riveyder.shahed.network.TelemetryPayload;
import io.github.riveyder.shahed.registry.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Server-thread-only authority for equipment, ownership, session identity and one flight step per tick. */
public final class DroneSessions {
    public static final float MAX_RANGE = 96.0F;
    public static final int INPUT_TIMEOUT_TICKS = 40;
    private static final Map<UUID, Session> PLAYERS = new HashMap<>();
    private static final Map<UUID, UUID> DRONES = new HashMap<>();

    private DroneSessions() {
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(DroneSessions::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> end(handler.player, "disconnect"));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Session session : new ArrayList<>(PLAYERS.values())) {
                end(session.player, "server_stopping");
            }
        });
        AttackBlockCallback.EVENT.register((player, level, hand, position, direction) ->
                controllingResult(player instanceof ServerPlayer serverPlayer && isControlling(serverPlayer)));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                controllingResult(player instanceof ServerPlayer serverPlayer && isControlling(serverPlayer)));
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                controllingResult(player instanceof ServerPlayer serverPlayer && isControlling(serverPlayer)));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                controllingResult(player instanceof ServerPlayer serverPlayer && isControlling(serverPlayer)));
        UseItemCallback.EVENT.register((player, level, hand) -> controllingResult(
                player instanceof ServerPlayer serverPlayer && isControlling(serverPlayer)
                        && !player.getItemInHand(hand).is(ModItems.CONTROLLER)));
    }

    private static InteractionResult controllingResult(boolean controlling) {
        return controlling ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    public static boolean isControlling(ServerPlayer player) {
        return PLAYERS.containsKey(player.getUUID());
    }

    public static Optional<UUID> sessionId(ServerPlayer player) {
        Session session = PLAYERS.get(player.getUUID());
        return session == null ? Optional.empty() : Optional.of(session.id);
    }

    public static Optional<UUID> controlledDrone(ServerPlayer player) {
        Session session = PLAYERS.get(player.getUUID());
        return session == null ? Optional.empty() : Optional.of(session.drone.getUUID());
    }

    public static float rangeFor(ServerPlayer player) {
        int viewDistance = Math.min(player.requestedViewDistance(), player.level().getServer().getPlayerList().getViewDistance());
        // Keep a complete chunk of margin at both ends of the player's streamed terrain.
        return Math.min(MAX_RANGE, Math.max(16.0F, (viewDistance - 2) * 16.0F));
    }

    public static boolean connect(ServerPlayer player, DroneEntity drone, ItemStack stack) {
        if (!ControllerItem.getBoundOwner(stack).filter(player.getUUID()::equals).isPresent()
                || !drone.getOwnerUuid().filter(player.getUUID()::equals).isPresent()) {
            return reject(player, "wrong_owner");
        }
        if (!ControllerItem.getBoundDrone(stack).filter(drone.getUUID()::equals).isPresent()
                || !heldBinding(player, drone.getUUID())) {
            return reject(player, "not_bound");
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.FPV_GOGGLES)) {
            return reject(player, "goggles_required");
        }
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()
                || player.isRemoved() || isControlling(player)) {
            return reject(player, "invalid_body");
        }
        if (drone.isRemoved() || !drone.isAlive() || drone.level() != player.level()) {
            return reject(player, "drone_unavailable");
        }
        if (DRONES.containsKey(drone.getUUID())) {
            return reject(player, "occupied");
        }
        float range = rangeFor(player);
        if (player.position().distanceToSqr(drone.position()) > range * range
                || !loaded(player.level(), drone.position()) || !tracked(player, drone.position())) {
            return reject(player, "too_far");
        }
        Session session = new Session(player, drone, range);
        PLAYERS.put(player.getUUID(), session);
        DRONES.put(drone.getUUID(), player.getUUID());
        player.setDeltaMovement(Vec3.ZERO);
        if (player.connection != null) {
            player.connection.resetFlyingTicks();
        }
        drone.setControlInput(DroneInput.IDLE);
        send(player, new SessionPayload(true, session.id, drone.getUUID(), drone.getId(), range, ""));
        telemetry(session);
        message(player, Component.translatable("message.shahed.controller.connected", drone.getUUID().toString().substring(0, 8)));
        return true;
    }

    /** Accept latest state, never advance flight here. Replay/foreign-session and non-finite input is rejected. */
    public static boolean accept(ServerPlayer player, InputPayload payload) {
        Session session = PLAYERS.get(player.getUUID());
        if (session == null || !session.id.equals(payload.sessionId())
                || !session.drone.getUUID().equals(payload.droneUuid())
                || payload.sequence() < 0 || payload.sequence() <= session.sequence
                || !payload.input().isFinite() || session.packetsThisTick >= 16) {
            return false;
        }
        session.packetsThisTick++;
        session.sequence = payload.sequence();
        session.pending = payload.input().sanitized();
        session.ticksSinceInput = 0;
        DroneInput input = session.pending;
        if (input.forward() != 0 || input.strafe() != 0 || input.vertical() != 0
                || input.yaw() != 0 || input.pitch() != 0 || input.brake()) {
            // Custom flight controls replace vanilla actions, which normally update the idle timer.
            player.resetLastActionTime();
        }
        return true;
    }

    public static void exit(ServerPlayer player, UUID sessionId) {
        if (sessionId(player).filter(sessionId::equals).isPresent()) {
            end(player, "exit");
        }
    }

    public static void end(ServerPlayer player, String reason) {
        Session session = PLAYERS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        DRONES.remove(session.drone.getUUID(), player.getUUID());
        session.drone.stopControl();
        player.setDeltaMovement(Vec3.ZERO);
        if (player.connection != null) {
            // Resume ordinary gravity without inheriting floating ticks from the anchored body.
            player.connection.resetFlyingTicks();
        }
        send(player, new SessionPayload(false, session.id, session.drone.getUUID(), session.drone.getId(), session.range,
                "message.shahed.session.reason." + reason));
        if (player.connection != null && player.isAlive() && player.level() == session.level
                && !reason.equals("disconnect") && !reason.equals("server_stopping")) {
            // Reconcile the physical pilot after the client restores its camera and normal movement.
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
        if (!reason.equals("disconnect")) {
            message(player, Component.translatable("message.shahed.session.ended",
                    Component.translatable("message.shahed.session.reason." + reason)));
        }
    }

    public static void tick(MinecraftServer server) {
        for (Session session : new ArrayList<>(PLAYERS.values())) {
            // Static state can coexist with test servers; each server only ticks its own sessions.
            if (session.level.getServer() != server) {
                continue;
            }
            String failure = invalidReason(session);
            if (failure != null) {
                end(session.player, failure);
                continue;
            }
            session.player.setDeltaMovement(Vec3.ZERO);
            if (session.player.connection != null) {
                // Movement packets are suppressed, so vanilla cannot refresh its floating checks.
                session.player.connection.resetFlyingTicks();
            }
            // Brief packet loss preserves motion; longer silence brakes before the hard disconnect.
            session.drone.setControlInput(session.ticksSinceInput >= 10
                    ? new DroneInput(0, 0, 0, 0, 0, true) : session.pending);
            session.ticksSinceInput++;
            session.packetsThisTick = 0;
            session.previousHurtTime = session.player.hurtTime;
            if (++session.telemetryTicks % 5 == 0) {
                telemetry(session);
            }
        }
    }

    private static String invalidReason(Session session) {
        ServerPlayer player = session.player;
        DroneEntity drone = session.drone;
        if (!player.isAlive() || player.isRemoved()) {
            return "death";
        }
        if (player.level() != session.level) {
            return "dimension";
        }
        if (player.getHealth() < session.health || player.hurtTime > session.previousHurtTime) {
            return "damaged";
        }
        if (player.position().distanceToSqr(session.bodyPosition) > 0.04
                || player.isPassenger() || player.isSleeping() || player.isSpectator()) {
            return "body_moved";
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.FPV_GOGGLES)
                || !heldBinding(player, drone.getUUID())) {
            return "equipment";
        }
        if (drone.isRemoved() || !drone.isAlive() || drone.level() != session.level
                || !drone.getOwnerUuid().filter(player.getUUID()::equals).isPresent()) {
            return "drone_unavailable";
        }
        float range = Math.min(session.range, rangeFor(player));
        Vec3 predicted = drone.position().add(drone.getDeltaMovement());
        if (session.bodyPosition.distanceToSqr(drone.position()) > range * range
                || session.bodyPosition.distanceToSqr(predicted) > range * range
                || !loaded(session.level, drone.position()) || !loaded(session.level, predicted)
                || !tracked(player, drone.position()) || !tracked(player, predicted)) {
            return "range";
        }
        return session.ticksSinceInput >= INPUT_TIMEOUT_TICKS ? "input_timeout" : null;
    }

    private static boolean loaded(ServerLevel level, Vec3 position) {
        return level.getChunkSource().hasChunk(Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
    }

    private static boolean tracked(ServerPlayer player, Vec3 position) {
        // Headless mock players have no connection/streaming view.
        return player.connection == null || player.getChunkTrackingView().contains(
                Mth.floor(position.x) >> 4, Mth.floor(position.z) >> 4);
    }

    private static boolean heldBinding(ServerPlayer player, UUID droneId) {
        return matches(player, player.getMainHandItem(), droneId) || matches(player, player.getOffhandItem(), droneId);
    }

    private static boolean matches(ServerPlayer player, ItemStack stack, UUID droneId) {
        return ControllerItem.getBoundDrone(stack).filter(droneId::equals).isPresent()
                && ControllerItem.getBoundOwner(stack).filter(player.getUUID()::equals).isPresent();
    }

    private static boolean reject(ServerPlayer player, String reason) {
        message(player, Component.translatable("message.shahed.controller." + reason));
        return false;
    }

    private static void message(ServerPlayer player, Component message) {
        if (player.connection != null) {
            player.sendOverlayMessage(message);
        }
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void telemetry(Session session) {
        DroneEntity drone = session.drone;
        float signal = (float) Math.clamp(1.0 - session.bodyPosition.distanceTo(drone.position()) / session.range, 0.0, 1.0);
        send(session.player, new TelemetryPayload(session.id, drone.getUUID(), drone.getDeltaMovement().length() * 20.0,
                drone.getY(), signal, drone.getFlightTicks()));
    }

    private static final class Session {
        private final UUID id = UUID.randomUUID();
        private final ServerPlayer player;
        private final DroneEntity drone;
        private final ServerLevel level;
        private final Vec3 bodyPosition;
        private final float range;
        private final float health;
        private DroneInput pending = DroneInput.IDLE;
        private int sequence = -1;
        private int ticksSinceInput;
        private int packetsThisTick;
        private int telemetryTicks;
        private int previousHurtTime;

        private Session(ServerPlayer player, DroneEntity drone, float range) {
            this.player = player;
            this.drone = drone;
            this.level = player.level();
            this.bodyPosition = player.position();
            this.range = range;
            this.health = player.getHealth();
            this.previousHurtTime = player.hurtTime;
        }
    }
}
