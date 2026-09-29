package dev.jco.carcasses;

import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Validated presentation only; matching and costs are defined separately. */
public final class Feedback {
    private String sound, particles;
    private float volume=.6F,pitch=1;
    private int count=5;
    private double spread=.18,speed=.03;
    private ItemStack display=ItemStack.EMPTY;
    private boolean frozen;
    private void edit() {if(frozen) throw new IllegalStateException("Feedback is frozen");}
    public Feedback displayItem(ItemStack stack) {edit();if(stack.isEmpty()) throw new IllegalArgumentException("Empty display item");display=stack.copyWithCount(1);return this;}
    public Feedback sound(String id,float volume,float pitch) {
        edit(); if(!Float.isFinite(volume)||!Float.isFinite(pitch)||volume<0||volume>4||pitch<=0||pitch>4) throw new IllegalArgumentException("Invalid sound volume/pitch");
        if(!id.isEmpty() && !BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.parse(id))) throw new IllegalArgumentException("Unknown sound: "+id);
        sound=id;this.volume=volume;this.pitch=pitch;return this;
    }
    public Feedback particles(String id,int count,double spread,double speed) {
        edit();if(count<0||count>64||!Double.isFinite(spread)||spread<0||spread>2||!Double.isFinite(speed)||speed<0||speed>1) throw new IllegalArgumentException("Invalid particle settings");
        // Existing KubeJS recipes may still ask for Simple Blood. Fall back safely
        // when the optional mod is absent, including on dedicated servers.
        if(id.equals("simpleblood:blood") && !(BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(id)) instanceof SimpleParticleType))id="minecraft:damage_indicator";
        if(!id.isEmpty() && !(BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(id)) instanceof SimpleParticleType)) throw new IllegalArgumentException("Expected registered simple particle: "+id);
        particles=id;this.count=count;this.spread=spread;this.speed=speed;return this;
    }
    public ItemStack display(ItemStack fallback) {return (display.isEmpty()?fallback:display).copyWithCount(1);}
    public void freeze() {frozen=true;}
    public void emit(ServerLevel level,Vec3 pos,ServerPlayer player,InteractionHand hand,ItemStack item,boolean tool) {
        // The input event already played its swing. Replaying it here looks like
        // a second hit, especially when harvest completes after actionTicks.
        if(sound==null) {
            var type=level.getBlockState(net.minecraft.core.BlockPos.containing(pos)).getSoundType();
            level.playSound(null,pos.x,pos.y,pos.z,tool?type.getHitSound():type.getPlaceSound(),SoundSource.BLOCKS,volume,pitch);
        } else if(!sound.isEmpty()) level.playSound(null,pos.x,pos.y,pos.z,BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(sound)),SoundSource.BLOCKS,volume,pitch);
        ParticleOptions options=particles==null ? (item.isEmpty()?ParticleTypes.POOF:new ItemParticleOption(ParticleTypes.ITEM,item.copyWithCount(1)))
            : particles.isEmpty()?null:(SimpleParticleType)BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(particles));
        if(options!=null && count>0) level.sendParticles(options,pos.x,pos.y+.35,pos.z,count,spread,spread,spread,speed);
    }
}
