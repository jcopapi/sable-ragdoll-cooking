package dev.jco.carcasses.integration.mixin;
import java.util.*;import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;import net.minecraft.core.BlockPos;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.entity.LivingEntity;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.*;
@Mixin(value=MobRagdollAssembly.class,remap=false)public interface SableAccess {
 @Invoker("sourceEntityForPart")static LivingEntity source(ServerLevel level,BlockPos pos){throw new AssertionError();}
 @Accessor("GRAB_COUNTS")static Map<UUID,Integer> grabs(){throw new AssertionError();}
}
