package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.mixin.ChunkMapFpvAccess;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Server-thread-only authority for equipment, ownership, session identity and one flight step per tick. */
public final class DroneSessions {
    public static final int INPUT_IDLE_TICKS = 10;
    private static final Map<UUID, Session> PLAYERS = new HashMap<>();
    private static final Map<UUID, UUID> DRONES = new HashMap<>();

    private DroneSessions() {
    }

    public static void register() {
        DroneChunkTracking.initialize();
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
        return hasFlightSession(player) || io.github.riveyder.shahed.replay.DroneReplays.isViewing(player);
    }

    public static boolean hasFlightSession(ServerPlayer player) { return PLAYERS.containsKey(player.getUUID()); }

    public static void destroyed(DroneEntity drone) {
        for(Session session:new ArrayList<>(PLAYERS.values())) {
            if(session.drone==drone) {
                io.github.riveyder.shahed.replay.DroneReplays.capture(session.player,session.id,drone);
                end(session.player,"destroyed");
            }
        }
    }

    /** Owner opt-in grants a camera, never input authority or ownership. */
    public static boolean spectate(ServerPlayer player,DroneEntity drone) {
        if(!drone.spectatorsAllowed()||drone.isRemoved()||drone.level()!=player.level()
                ||isControlling(player)||!player.isAlive()||player.isSpectator()||player.isPassenger()||player.isSleeping()
                ||!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.FPV_GOGGLES)) return false;
        if(player.connection!=null && (!ServerPlayNetworking.canSend(player,SessionPayload.TYPE)
                || !ServerPlayNetworking.canSend(player,TelemetryPayload.TYPE))) return false;
        Session session=new Session(player,drone,true);
        PLAYERS.put(player.getUUID(),session);
        player.setDeltaMovement(Vec3.ZERO);
        if(player.connection!=null) { player.connection.resetFlyingTicks();session.chunks.update(player,drone.chunkPosition()); }
        refreshTracking(player);
        send(player,new SessionPayload(true,session.id,drone.getUUID(),drone.getId(),"spectator"));
        telemetry(session);
        return true;
    }

    public static Optional<UUID> sessionId(ServerPlayer player) {
        Session session = PLAYERS.get(player.getUUID());
        return session == null ? Optional.empty() : Optional.of(session.id);
    }

    public static Optional<UUID> controlledDrone(ServerPlayer player) {
        Session session = PLAYERS.get(player.getUUID());
        return session == null ? Optional.empty() : Optional.of(session.drone.getUUID());
    }

    /** The viewing origin is separate from the physical body and its vanilla loading tickets. */
    public static Vec3 trackingPosition(ServerPlayer player) {
        Session session = PLAYERS.get(player.getUUID());
        return session == null ? io.github.riveyder.shahed.replay.DroneReplays.position(player) : session.drone.position();
    }

    public static ChunkPos trackingChunk(ServerPlayer player) {
        return ChunkPos.containing(net.minecraft.core.BlockPos.containing(trackingPosition(player)));
    }

    public static boolean connect(ServerPlayer player, DroneEntity drone, ItemStack stack) {
        // The v3 session protocol distinguishes read-only spectators. Reject older clients before
        // immobilizing the body; a missing protocol must never create an invisible live session.
        if (player.connection != null && (!ServerPlayNetworking.canSend(player, SessionPayload.TYPE)
                || !ServerPlayNetworking.canSend(player, TelemetryPayload.TYPE))) {
            return reject(player, "incompatible_client");
        }
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
        Session session = new Session(player, drone,false);
        PLAYERS.put(player.getUUID(), session);
        DRONES.put(drone.getUUID(), player.getUUID());
        player.setDeltaMovement(Vec3.ZERO);
        if (player.connection != null) {
            player.connection.resetFlyingTicks();
        }
        drone.setControlInput(DroneInput.IDLE);
        if (player.connection != null) session.chunks.update(player, drone.chunkPosition());
        refreshTracking(player);
        send(player, new SessionPayload(true, session.id, drone.getUUID(), drone.getId(), ""));
        telemetry(session);
        message(player, Component.translatable("message.shahed.controller.connected", drone.getUUID().toString().substring(0, 8)));
        return true;
    }

    /** Accept latest state, never advance flight here. Replay/foreign-session and non-finite input is rejected. */
    public static boolean accept(ServerPlayer player, InputPayload payload) {
        Session session = PLAYERS.get(player.getUUID());
        if (session == null || session.spectator || !session.id.equals(payload.sessionId())
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
        if(!session.spectator) {
            DRONES.remove(session.drone.getUUID(), player.getUUID());
            session.drone.stopControl();
        }
        session.chunks.close();
        if (!reason.equals("disconnect") && !reason.equals("server_stopping")) refreshTracking(player);
        player.setDeltaMovement(Vec3.ZERO);
        if (player.connection != null) {
            // Resume ordinary gravity without inheriting floating ticks from the anchored body.
            player.connection.resetFlyingTicks();
        }
        send(player, new SessionPayload(false, session.id, session.drone.getUUID(), session.drone.getId(),
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
            // A lost input packet cannot hold thrust forever, or end the viewing session.
            if(!session.spectator) session.drone.setControlInput(session.ticksSinceInput >= INPUT_IDLE_TICKS
                    ? DroneInput.IDLE : session.pending);
            if (session.player.connection != null) {
                session.chunks.update(session.player, session.drone.chunkPosition());
            }
            refreshTracking(session.player);
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
                || (!session.spectator && !heldBinding(player, drone.getUUID()))) {
            return "equipment";
        }
        if(session.spectator && !drone.spectatorsAllowed()) return "spectator_revoked";
        if (drone.isDestroyed()) return "destroyed";
        if (drone.isRemoved() || !drone.isAlive() || drone.level() != session.level
                || (!session.spectator && !drone.getOwnerUuid().filter(player.getUUID()::equals).isPresent())) {
            return "drone_unavailable";
        }
        return null;
    }

    private static void refreshTracking(ServerPlayer player) {
        if (player.connection == null || player.isRemoved()) return;
        // Vanilla normally refreshes terrain only when the body changes section. The drone moves instead.
        var chunks = player.level().getChunkSource();
        ((ChunkMapFpvAccess) chunks.chunkMap).shahed$updateChunkTracking(player);
        chunks.move(player);
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
        send(session.player, new TelemetryPayload(session.id, drone.getUUID(), drone.getDeltaMovement().length() * 20.0,
                drone.getY(), drone.getFlightTicks(),drone.getDeltaMovement().y*20,
                drone.getYRot(),drone.getXRot(),drone.getRoll(),drone.getThrottle()));
    }

    private static final class Session {
        private final UUID id = UUID.randomUUID();
        private final boolean spectator;
        private final ServerPlayer player;
        private final DroneEntity drone;
        private final ServerLevel level;
        private final Vec3 bodyPosition;
        private final DroneChunkTracking chunks;
        private final float health;
        private DroneInput pending = DroneInput.IDLE;
        private int sequence = -1;
        private int ticksSinceInput;
        private int packetsThisTick;
        private int telemetryTicks;
        private int previousHurtTime;

        private Session(ServerPlayer player, DroneEntity drone,boolean spectator) {
            this.spectator=spectator;
            this.player = player;
            this.drone = drone;
            this.level = player.level();
            this.bodyPosition = player.position();
            this.chunks = new DroneChunkTracking(level);
            this.health = player.getHealth();
            this.previousHurtTime = player.hurtTime;
        }
    }
}
