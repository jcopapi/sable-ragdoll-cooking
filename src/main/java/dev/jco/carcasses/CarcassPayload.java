package dev.jco.carcasses;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record CarcassPayload(UUID id,String dimension,Vec3 position,String definition,int actions,int total,int cooking,int cookTotal,int work,int workTotal,String phase,boolean heating,int entityId,net.minecraft.world.item.ItemStack display,double precise,double rate,double hudRange,net.minecraft.nbt.CompoundTag extra) implements CustomPacketPayload {
    public CarcassPayload(UUID id,String dimension,Vec3 position,String definition,int actions,int total,int cooking,int cookTotal,int work,int workTotal,String phase,boolean heating,int entityId,net.minecraft.world.item.ItemStack display,double precise,double rate,double hudRange){this(id,dimension,position,definition,actions,total,cooking,cookTotal,work,workTotal,phase,heating,entityId,display,precise,rate,hudRange,new net.minecraft.nbt.CompoundTag());}
    public CarcassPayload(UUID id,String dimension,Vec3 position,String definition,int actions,int total,int cooking,int cookTotal,int work,int workTotal) {this(id,dimension,position,definition,actions,total,cooking,cookTotal,work,workTotal,"RAW",false,-1,net.minecraft.world.item.ItemStack.EMPTY);}
    public CarcassPayload(UUID id,String dimension,Vec3 position,String definition,int actions,int total,int cooking,int cookTotal,int work,int workTotal,String phase,boolean heating,int entityId,net.minecraft.world.item.ItemStack display){this(id,dimension,position,definition,actions,total,cooking,cookTotal,work,workTotal,phase,heating,entityId,display,cooking,heating?1:0,6);}
    public static final Type<CarcassPayload> TYPE=new Type<>(ResourceLocation.parse("jco_carcasses:progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CarcassPayload> CODEC=new StreamCodec<>() {
        public CarcassPayload decode(RegistryFriendlyByteBuf b) {
            return new CarcassPayload(b.readUUID(),b.readUtf(256),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readUtf(256),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(32),b.readBoolean(),b.readVarInt(),net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(b),b.readDouble(),b.readDouble(),b.readDouble(),b.readNbt());
        }
        public void encode(RegistryFriendlyByteBuf b,CarcassPayload p) {
            b.writeUUID(p.id); b.writeUtf(p.dimension,256); b.writeDouble(p.position.x); b.writeDouble(p.position.y); b.writeDouble(p.position.z); b.writeUtf(p.definition,256);
            b.writeVarInt(p.actions); b.writeVarInt(p.total); b.writeVarInt(p.cooking); b.writeVarInt(p.cookTotal); b.writeVarInt(p.work); b.writeVarInt(p.workTotal);
            b.writeUtf(p.phase,32);b.writeBoolean(p.heating);b.writeVarInt(p.entityId);net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(b,p.display);b.writeDouble(p.precise);b.writeDouble(p.rate);b.writeDouble(p.hudRange);b.writeNbt(p.extra);
        }
    };
    public Type<CarcassPayload> type() { return TYPE; }
}
