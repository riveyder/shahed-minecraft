package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.network.SessionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.phys.Vec3;

/** Test-only observation of real server effects and handled client packets; never shipped in the mod. */
public final class CrashEffectsProbe implements AutoCloseable {
    private static final ThreadLocal<CrashEffectsProbe> ACTIVE = new ThreadLocal<>();
    public int explosions;
    public int explosionParticles;
    public int smokeParticles;
    public int blastSounds;
    public Vec3 blastPosition;
    public final List<SessionPayload> sessions = new ArrayList<>();
    public final List<io.github.riveyder.shahed.network.ExplosionEffectPayload> effects=new ArrayList<>();
    public static final List<io.github.riveyder.shahed.network.ExplosionEffectPayload> CLIENT_EFFECTS=new CopyOnWriteArrayList<>();
    public static final List<String> CLIENT_SOUNDS=new CopyOnWriteArrayList<>();
    public static volatile boolean captureClient;
    public static final List<Packet<?>> CLIENT_PACKETS = new CopyOnWriteArrayList<>();

    public CrashEffectsProbe() { ACTIVE.set(this); }
    public static CrashEffectsProbe active() { return ACTIVE.get(); }
    public static void clientPacket(Packet<?> packet) {
        if (captureClient) CLIENT_PACKETS.add(packet);
    }
    @Override public void close() { ACTIVE.remove(); }
}
