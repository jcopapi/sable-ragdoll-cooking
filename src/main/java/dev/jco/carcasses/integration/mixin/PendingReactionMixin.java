package dev.jco.carcasses.integration.mixin;

import dev.jco.carcasses.integration.ReactionBridge;
import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;
import dev.leo.sableplayerragdoll.mob.api.MobRagdollLaunchOptions;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=MobRagdollAssembly.class,remap=false)
public abstract class PendingReactionMixin {
    @Shadow @Final private static Map<UUID,?> PENDING_LAUNCHES;
    @Inject(method="requestLaunch",at=@At("HEAD"))
    private static void jco$replace(ServerLevel level,LivingEntity entity,Vec3 linear,Vec3 angular,MobRagdollLaunchOptions options,CallbackInfoReturnable<Boolean> ci){
        if(ReactionBridge.forced())PENDING_LAUNCHES.remove(entity.getUUID());
    }
}
