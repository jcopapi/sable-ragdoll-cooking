package dev.jco.carcasses;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;

/** Adapter owns physical representation; processing only depends on this contract. */
public interface CarcassAdapter {
    String id();
    boolean supports(ServerLevel level, Mob original);
    boolean launch(ServerLevel level, LivingEntity carrier);
    default boolean launch(ServerLevel level,LivingEntity carrier,double impactMultiplier) {return launch(level,carrier);}
    default void impactExisting(ServerLevel level,LivingEntity carrier,double impactMultiplier) {}
    boolean ready(ServerLevel level, LivingEntity carrier);
    boolean pending(ServerLevel level, LivingEntity carrier);
    /** False means removal is deferred, e.g. while the physics object is being held. */
    boolean finish(ServerLevel level, LivingEntity carrier);
}
