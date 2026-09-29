package dev.jco.carcasses.integration.mixin;
import dev.jco.carcasses.CarcassRuntime;
import dev.jco.carcasses.CookingConfig;
import dev.jco.carcasses.integration.ReactionBridge;
import dev.leo.ragdollreactions.physics.MobDamageReactionHandler;
import dev.leo.ragdollreactions.physics.ReactionMobLauncher;
import dev.leo.ragdollreactions.config.ReactionSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=MobDamageReactionHandler.class,remap=false)
public abstract class ReactionDamageMixin {
 @Inject(method="onMobDamaged",at=@At("HEAD"),cancellable=true)
 private static void jco$carcassOwnsLaunch(LivingEntity entity,DamageSource source,float damage,CallbackInfo ci){
  if(!ReactionBridge.forced()&&(CarcassRuntime.isCarrier(entity)||CookingConfig.MOB_RAGDOLLS_ONLY_ON_DEATH.get()&&entity.getHealth()>0))ci.cancel();
 }
 @Redirect(method="onMobDamaged",at=@At(value="INVOKE",target="Lnet/minecraft/util/RandomSource;nextDouble()D"))
 private static double jco$chance(RandomSource random){return ReactionBridge.forced()?0:random.nextDouble();}
 @Redirect(method="onMobDamaged",at=@At(value="INVOKE",target="Ldev/leo/ragdollreactions/physics/MobDamageReactionHandler;requiredDamageForRemainingHealth(Lnet/minecraft/world/entity/LivingEntity;F)D"))
 private static double jco$threshold(LivingEntity entity,float damage){return ReactionBridge.forced()?0:(entity.getHealth()+damage)*ReactionSettings.mobs().damage().healthFraction();}
 @Redirect(method="onMobDamaged",at=@At(value="INVOKE",target="Ldev/leo/ragdollreactions/physics/ReactionMobLauncher;canTarget(Lnet/minecraft/world/entity/LivingEntity;J)Z"))
 private static boolean jco$target(LivingEntity entity,long time){return ReactionBridge.forced()||ReactionMobLauncher.canTarget(entity,time);}
 @Redirect(method="onMobDamaged",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
 private static boolean jco$type(DamageSource source,TagKey<DamageType> tag){return !ReactionBridge.forced()&&source.is(tag);}
 @Redirect(method="onMobDamaged",at=@At(value="INVOKE",target="Ldev/leo/ragdollreactions/config/ReactionSettings;enabled()Z"))
 private static boolean jco$enabled(){return ReactionBridge.forced()||ReactionSettings.enabled();}
 @Redirect(method="onMobDamaged",at=@At(value="INVOKE",target="Ldev/leo/ragdollreactions/config/ReactionSettings$Mobs;enabled()Z"))
 private static boolean jco$mobs(ReactionSettings.Mobs settings){return ReactionBridge.forced()||settings.enabled();}
 @Redirect(method="onMobDamaged",at=@At(value="INVOKE",target="Ldev/leo/ragdollreactions/config/ReactionSettings$MobDamage;enabled()Z"))
 private static boolean jco$hits(ReactionSettings.MobDamage settings){return ReactionBridge.forced()||settings.enabled();}
}
