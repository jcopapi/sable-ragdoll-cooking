package dev.jco.carcasses.integration.mixin;
import dev.jco.carcasses.*;
import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;
import dev.leo.sableplayerragdoll.mob.api.MobRagdollEndEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=MobRagdollAssembly.class,remap=false)
public abstract class CarcassHoldMixin {
 @Inject(method="despawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ldev/leo/sableplayerragdoll/mob/api/MobRagdollEndEvent$Reason;)V",at=@At("HEAD"),cancellable=true)
 private static void hold(ServerLevel level,LivingEntity entity,MobRagdollEndEvent.Reason reason,CallbackInfo ci){var e=CarcassData.get(level).entries.get(entity.getUUID());if(e!=null&&!e.finishing&&CarcassRuntime.isCarrier(entity))ci.cancel();}
}
