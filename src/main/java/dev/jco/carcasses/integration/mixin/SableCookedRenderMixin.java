package dev.jco.carcasses.integration.mixin;

import com.mojang.blaze3d.vertex.*;
import dev.jco.carcasses.client.CarcassOverlay;
import dev.jco.carcasses.client.Darkened;
import dev.leo.sableplayerragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Wraps Sable's actual part mesh buffers, including replay and baked-quad paths. */
@Pseudo
@Mixin(targets="dev.leo.sableplayerragdoll.mob.client.MobRagdollPartBlockEntityRenderer",remap=false)
public abstract class SableCookedRenderMixin {
    @Inject(method="render(Ldev/leo/sableplayerragdoll/mob/block/entity/MobRagdollPartBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("HEAD"),cancellable=true)
    private void jco$noDebugCube(MobRagdollPartBlockEntity part,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){if(part.quads().isEmpty()||part.sourceEntityId()!=null&&CarcassOverlay.vanished(part.sourceEntityId()))ci.cancel();}

    @ModifyVariable(method="render(Ldev/leo/sableplayerragdoll/mob/block/entity/MobRagdollPartBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private MultiBufferSource jco$tint(MultiBufferSource buffer,MobRagdollPartBlockEntity part,float partial,PoseStack pose,MultiBufferSource original,int light,int overlay) {
        float amount=part.sourceEntityId()==null?0:CarcassOverlay.cooking(part.sourceEntityId());if(amount<=0)return buffer;
        return type->new Darkened(buffer.getBuffer(type),amount);
    }
}
