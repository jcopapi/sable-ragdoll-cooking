package dev.jco.carcasses.integration.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LootAccess {
    @Invoker("dropAllDeathLoot") void jco$resolveOriginalDeathLoot(ServerLevel level,DamageSource source);
}
