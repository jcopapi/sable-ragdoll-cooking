package dev.jco.carcasses.integration.mixin;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.block.entity.CampfireBlockEntity;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireFuelMixin {
 @Inject(method="cookTick",at=@At("HEAD"),cancellable=true)
 private static void jco$fuel(Level level,BlockPos pos,BlockState state,CampfireBlockEntity be,CallbackInfo ci){if(!dev.jco.carcasses.CampfireFuel.tick(level,pos,state,be))ci.cancel();}
}
