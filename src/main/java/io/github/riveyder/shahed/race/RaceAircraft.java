package io.github.riveyder.shahed.race;

import java.util.UUID;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.entity.DroneEntity;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;

/** Small adapter boundary: later aircraft only need position, identity and a safe flight check. */
public interface RaceAircraft {
    UUID id();
    Vec3 position();
    boolean valid(ServerPlayer pilot);
    String category();
    void beginRace();
    static RaceAircraft controlled(ServerPlayer pilot) {
        var id=DroneSessions.controlledDrone(pilot);
        if(!DroneSessions.isPiloting(pilot)||id.isEmpty()||!(pilot.level().getEntity(id.get()) instanceof DroneEntity drone))return null;
        return new Shahed(drone);
    }
    record Shahed(DroneEntity drone) implements RaceAircraft {
        public String category(){return drone.kind().id;}
        public void beginRace(){drone.makeRaceSafe();}
        public UUID id() {return drone.getUUID();}
        public Vec3 position() {return drone.position().add(0,drone.getBbHeight()*.5,0);}
        public boolean valid(ServerPlayer pilot) {
            return drone.canRace()&&!drone.isRemoved()&&!drone.isDestroyed()&&drone.level()==pilot.level()
                    &&drone.getOwnerUuid().filter(pilot.getUUID()::equals).isPresent()&&DroneSessions.isPiloting(pilot)
                    &&DroneSessions.controlledDrone(pilot).filter(id()::equals).isPresent();
        }
    }
}
