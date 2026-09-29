package dev.jco.carcasses.integration.mixin;

import dev.jco.carcasses.CookingConfig;
import dev.jco.carcasses.integration.ReactionBridge;
import dev.jco.carcasses.integration.SableAdapter;
import dev.leo.ragdollreactions.physics.ReactionMobLauncher;
import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;
import dev.leo.sableplayerragdoll.mob.api.MobRagdollLaunchOptions;
import dev.leo.sableplayerragdoll.mob.api.MobRagdollSession;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ReactionMobLauncher.class,remap=false)
public abstract class ReactionLaunchMixin {
    /** All native mob reaction pathways converge here: hit, fall, explosion and impact. */
    @Inject(method={"launch(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;JLorg/joml/Vector3d;)Lorg/joml/Vector3d;","launchSilent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;JLorg/joml/Vector3d;)Lorg/joml/Vector3d;"},at=@At("HEAD"),cancellable=true)
    private static void jco$deathOnly(ServerLevel level,LivingEntity entity,long tick,Vector3d velocity,CallbackInfoReturnable<Vector3d> ci){
        if(CookingConfig.MOB_RAGDOLLS_ONLY_ON_DEATH.get()&&!ReactionBridge.forced()&&entity.getHealth()>0)ci.setReturnValue(null);
    }
    @Redirect(method="launch(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;JLorg/joml/Vector3d;Z)Lorg/joml/Vector3d;",at=@At(value="INVOKE",target="Ldev/leo/sableplayerragdoll/api/RagdollAPI;launchMob(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ldev/leo/sableplayerragdoll/mob/api/MobRagdollLaunchOptions;)Ldev/leo/sableplayerragdoll/mob/api/MobRagdollSession;"))
    private static MobRagdollSession jco$launch(ServerLevel level,LivingEntity entity,Vec3 linear,Vec3 angular,MobRagdollLaunchOptions options){
        if(ReactionBridge.forced()){
            if(MobRagdollAssembly.isConverted(entity.getUUID())){
                SableAdapter.applyMotion(level,entity,linear,angular);
                return new MobRagdollSession(){
                    public LivingEntity entity(){return entity;}
                    public Vec3 currentVelocity(){return MobRagdollAssembly.currentVelocity(entity.getUUID());}
                    public long elapsedTicks(){return 0;}
                    public void release(){}
                };
            }
            options=new MobRagdollLaunchOptions(Integer.MAX_VALUE);
        }
        return RagdollAPI.launchMob(level,entity,linear,angular,options);
    }
}
