package dev.jco.carcasses.integration.mixin;

import dev.jco.carcasses.client.CarcassOverlay;
import dev.leo.sableplayerragdoll.neoforge.client.RagdollGrabClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value=RagdollGrabClient.class,remap=false)
public abstract class SableGrabTargetMixin {
    @Inject(method="targetedPart",at=@At("RETURN"),cancellable=true)
    private static void jco$expandedTarget(Minecraft minecraft,CallbackInfoReturnable<BlockPos> result){
        if(result.getReturnValue()==null){var pos=CarcassOverlay.extendedGrabTarget(minecraft);if(pos!=null)result.setReturnValue(pos);}
    }
}
