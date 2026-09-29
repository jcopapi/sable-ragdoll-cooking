package dev.jco.carcasses;
import java.util.UUID;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.minecraft.resources.ResourceLocation;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.InteractionHand;import net.minecraft.world.phys.*;import net.minecraft.world.level.ClipContext;
public record CarryPayload(UUID id,boolean release) implements CustomPacketPayload {
 public static final Type<CarryPayload> TYPE=new Type<>(ResourceLocation.parse("jco_carcasses:carry"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CarryPayload> CODEC=new StreamCodec<>(){public CarryPayload decode(RegistryFriendlyByteBuf b){return new CarryPayload(b.readUUID(),b.readBoolean());}public void encode(RegistryFriendlyByteBuf b,CarryPayload p){b.writeUUID(p.id);b.writeBoolean(p.release);}};
 public Type<CarryPayload> type(){return TYPE;}
 public static void handle(ServerPlayer p,CarryPayload packet){var level=p.serverLevel();var e=CarcassData.get(level).entries.get(packet.id);if(e==null||!(level.getEntity(packet.id) instanceof LivingEntity body))return;
  if(packet.release){if(e.transport.hasUUID("carry")&&e.transport.getUUID("carry").equals(p.getUUID()))CarcassTransport.release(level,body,e);return;}
  if(p.isSpectator()||!p.mayBuild()||e.finishing||e.committing)return;var start=p.getEyePosition();var end=start.add(p.getLookAngle().scale(p.blockInteractionRange()+1));var box=body.getBoundingBox().inflate(.8);var hit=box.clip(start,end);if(hit.isEmpty()&&!box.contains(start))return;var target=hit.orElse(start);var obstruction=level.clip(new ClipContext(start,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(obstruction.getType()!=HitResult.Type.MISS&&obstruction.getLocation().distanceToSqr(start)+.04<target.distanceToSqr(start))return;CarcassRuntime.interact(p,body,InteractionHand.MAIN_HAND);
 }
}
