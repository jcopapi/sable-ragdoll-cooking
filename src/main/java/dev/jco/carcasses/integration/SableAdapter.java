package dev.jco.carcasses.integration;

import dev.jco.carcasses.CarcassAdapter;
import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.mob.*;
import dev.leo.sableplayerragdoll.mob.api.MobRagdollLaunchOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

public final class SableAdapter implements CarcassAdapter {
    public String id() { return "sable_player_ragdoll"; }
    public boolean supports(ServerLevel level, Mob original) { return MobRagdollWhitelist.isAllowed(level,original.getType()); }
    public boolean launch(ServerLevel level, LivingEntity carrier) {return launch(level,carrier,1);}
    public boolean launch(ServerLevel level,LivingEntity carrier,double impactMultiplier) {
        return ReactionBridge.react(level,carrier,carrier.getLastDamageSource(),impactMultiplier);
    }
    public void impactExisting(ServerLevel level,LivingEntity carrier,double impactMultiplier) {
        ReactionBridge.react(level,carrier,carrier.getLastDamageSource(),impactMultiplier);
    }
    public static void applyMotion(ServerLevel level,LivingEntity carrier,Vec3 velocity,Vec3 angular) {
        var entry=MobRagdollSavedData.get(level).getEntry(carrier.getUUID());if(entry==null)return;
        var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);if(container==null)return;
        for(var id:entry.partIds().values()) if(container.getSubLevel(id) instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel sub) {
            var handle=dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(sub);
            if(handle!=null&&handle.isValid())handle.addLinearAndAngularVelocity(new org.joml.Vector3d(velocity.x,velocity.y,velocity.z),new org.joml.Vector3d(angular.x,angular.y,angular.z));
        }
    }
    public boolean ready(ServerLevel level, LivingEntity carrier) {
        return MobRagdollAssembly.isConverted(carrier.getUUID()) && MobRagdollSavedData.get(level).getEntry(carrier.getUUID())!=null;
    }
    public boolean pending(ServerLevel level, LivingEntity carrier) { return MobRagdollAssembly.hasPendingLaunch(carrier.getUUID()); }
    public boolean finish(ServerLevel level, LivingEntity carrier) {
        if(pending(level,carrier)) return false;
        RagdollAPI.releaseMob(carrier);
        return !MobRagdollAssembly.isActiveOrSavedRagdollSource(level,carrier.getUUID());
    }
}
