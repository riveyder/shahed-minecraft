package io.github.riveyder.shahed.entity;

import io.github.riveyder.shahed.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

/** Stable save/registry identities and fictional arcade profiles, not real aircraft specifications. */
public enum AircraftKind {
    SHAHED("shahed", "SHAHED", .8, false),
    QUAD("quad", "FPV QUAD", 1.6, true),
    CONTACT("contact_quad", "CONTACT FPV", 1.9, true),
    JET("jet", "JET FPV", 3.6, false);
    public final String id,label;
    public final double maxSpeed;
    public final boolean hover;
    AircraftKind(String id,String label,double maxSpeed,boolean hover){this.id=id;this.label=label;this.maxSpeed=maxSpeed;this.hover=hover;}
    public EntityType<DroneEntity> type(){return switch(this){case SHAHED->ModEntities.DRONE;case QUAD->ModEntities.QUAD;case CONTACT->ModEntities.CONTACT_QUAD;case JET->ModEntities.JET;};}
    public static AircraftKind of(EntityType<?> type){
        if(type==ModEntities.QUAD)return QUAD;if(type==ModEntities.CONTACT_QUAD)return CONTACT;
        if(type==ModEntities.JET)return JET;return SHAHED;
    }
    public static AircraftKind byId(String id){for(var kind:values())if(kind.id.equals(id))return kind;return SHAHED;}
}
