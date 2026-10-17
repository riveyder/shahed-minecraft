package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.ShahedMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/** Temporary remote loading: a bounded terrain bubble and a smaller entity-simulation bubble per pilot. */
public final class DroneChunkTracking implements AutoCloseable {
    // Expiration is a fallback for shutdown/errors; ordinary session cleanup releases tickets immediately.
    public static final TicketType LOADING = Registry.register(BuiltInRegistries.TICKET_TYPE,
            ShahedMod.id("fpv_loading"), new TicketType(40, TicketType.FLAG_LOADING));
    public static final TicketType SIMULATION = Registry.register(BuiltInRegistries.TICKET_TYPE,
            ShahedMod.id("fpv_simulation"), new TicketType(40,
                    TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    private static final int SIMULATION_RADIUS = 4;
    // Vanilla identifies tickets by type + level + chunk, so pilots sharing a center must share a lease.
    private static final Map<Lease, Integer> USERS = new HashMap<>();
    private final ServerLevel level;
    private Lease loading;
    private Lease simulation;

    public DroneChunkTracking(ServerLevel level) {
        this.level = level;
    }

    public static void initialize() {
        // Initialize registered types during mod initialization, before the registry is frozen.
    }

    public void update(ServerPlayer player, ChunkPos center) {
        int viewDistance = Math.clamp(Math.min(player.requestedViewDistance(),
                level.getServer().getPlayerList().getViewDistance()), 2, 32);
        loading = replace(loading, new Lease(level, LOADING, center, viewDistance + 2));
        simulation = replace(simulation, new Lease(level, SIMULATION, center, SIMULATION_RADIUS));
    }

    private static Lease replace(Lease old, Lease next) {
        if (!next.equals(old)) {
            USERS.merge(next, 1, Integer::sum);
            // Acquire the next center before releasing the old one; simulation never loses its ticket.
            next.refresh();
            release(old);
        } else {
            next.refresh();
        }
        return next;
    }

    private static void release(Lease lease) {
        if (lease == null) return;
        Integer count = USERS.get(lease);
        if (count == null) return;
        if (count > 1) {
            USERS.put(lease, count - 1);
        } else {
            USERS.remove(lease);
            lease.level.getChunkSource().removeTicketWithRadius(lease.type, lease.center, lease.radius);
        }
    }

    @Override
    public void close() {
        release(loading);
        release(simulation);
        loading = simulation = null;
    }

    private record Lease(ServerLevel level, TicketType type, ChunkPos center, int radius) {
        private void refresh() {
            level.getChunkSource().addTicketWithRadius(type, center, radius);
        }
    }
}
