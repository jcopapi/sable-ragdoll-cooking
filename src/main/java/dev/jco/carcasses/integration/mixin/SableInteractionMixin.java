package dev.jco.carcasses.integration.mixin;

import dev.jco.carcasses.CarcassRuntime;
import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;
import dev.leo.sableplayerragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 0.7.5's RagdollInteractEvent is for player bodies, not mob parts. Both overloads need interception. */
@Pseudo
@Mixin(value=MobRagdollAssembly.class,remap=false)
public abstract class SableInteractionMixin {
    @Inject(method="applyKnockup",at=@At("HEAD"),cancellable=true)
    private static void jco$noHarvestLaunch(java.util.UUID id,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();if(server!=null)for(var level:server.getAllLevels())if(dev.jco.carcasses.CarcassData.get(level).entries.containsKey(id)){ci.cancel();return;}}

    @Inject(method="interactWithPart(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",at=@At("HEAD"),cancellable=true)
    private static void jco$position(ServerLevel level,BlockPos pos,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci) {
        var carrier=dev.jco.carcasses.integration.BodyParts.resolve(level,pos,level.getBlockEntity(pos) instanceof MobRagdollPartBlockEntity part?part:null);if(carrier!=null&&player instanceof ServerPlayer sp&&CarcassRuntime.interact(sp,carrier,hand))ci.setReturnValue(InteractionResult.CONSUME);
    }
    @Inject(method="interactWithPart(Lnet/minecraft/server/level/ServerLevel;Ldev/leo/sableplayerragdoll/mob/block/entity/MobRagdollPartBlockEntity;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",at=@At("HEAD"),cancellable=true)
    private static void jco$part(ServerLevel level,MobRagdollPartBlockEntity part,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci) {
        jco$process(level,part,player,hand,ci);
    }
    @Unique private static void jco$process(ServerLevel level,MobRagdollPartBlockEntity part,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci) {
        var carrier=dev.jco.carcasses.integration.BodyParts.resolve(level,part.getBlockPos(),part);
        if(player instanceof ServerPlayer serverPlayer && carrier!=null
            && CarcassRuntime.interact(serverPlayer,carrier,hand)) ci.setReturnValue(InteractionResult.CONSUME);
    }
    @Inject(method="attackPart(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z",at=@At("HEAD"),cancellable=true)
    private static void jco$attackPosition(ServerLevel level,BlockPos pos,Player player,CallbackInfoReturnable<Boolean> ci) {
        var carrier=dev.jco.carcasses.integration.BodyParts.resolve(level,pos,level.getBlockEntity(pos) instanceof MobRagdollPartBlockEntity part?part:null);if(carrier!=null&&player instanceof ServerPlayer sp&&CarcassRuntime.attack(sp,carrier))ci.setReturnValue(true);
    }
    @Inject(method="attackPart(Lnet/minecraft/server/level/ServerLevel;Ldev/leo/sableplayerragdoll/mob/block/entity/MobRagdollPartBlockEntity;Lnet/minecraft/world/entity/player/Player;)Z",at=@At("HEAD"),cancellable=true)
    private static void jco$attackPart(ServerLevel level,MobRagdollPartBlockEntity part,Player player,CallbackInfoReturnable<Boolean> ci) {jco$attack(level,part,player,ci);}
    @Unique private static void jco$attack(ServerLevel level,MobRagdollPartBlockEntity part,Player player,CallbackInfoReturnable<Boolean> ci) {
        var carrier=dev.jco.carcasses.integration.BodyParts.resolve(level,part.getBlockPos(),part);
        if(player instanceof ServerPlayer serverPlayer && carrier!=null
            && CarcassRuntime.attack(serverPlayer,carrier))ci.setReturnValue(true);
    }
}
