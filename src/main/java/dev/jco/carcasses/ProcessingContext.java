package dev.jco.carcasses;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Callbacks edit an output plan; the engine spawns it after each successful harvest commit. */
public final class ProcessingContext {
    private final ServerLevel level;
    private final ServerPlayer player;
    private final Vec3 position;
    private final boolean cooked;
    private final CompoundTag original;
    private final List<ItemStack> loot, outputs;
    private final int index,count;
    ProcessingContext(ServerLevel level, ServerPlayer player, Vec3 position, boolean cooked, CompoundTag original, List<ItemStack> loot) {
        this(level,player,position,cooked,original,loot,loot,0,1);
    }
    ProcessingContext(ServerLevel level, ServerPlayer player, Vec3 position, boolean cooked, CompoundTag original, List<ItemStack> loot,List<ItemStack> defaults,int index,int count) {
        this.level=level; this.player=player; this.position=position; this.cooked=cooked; this.original=original.copy();
        this.loot=loot.stream().map(ItemStack::copy).toList(); outputs=new ArrayList<>(defaults.stream().map(ItemStack::copy).toList());this.index=index;this.count=count;
    }
    public ServerLevel level() { return level; }
    public ServerPlayer player() { return player; }
    public Vec3 position() { return position; }
    public boolean cooked() { return cooked; }
    public int harvestIndex() {return index;}
    public int harvestCount() {return count;}
    public CompoundTag originalMob() { return original.copy(); }
    public List<ItemStack> resolveLoot() { return loot.stream().map(ItemStack::copy).toList(); }
    public List<ItemStack> resolveHarvestLoot(){return CarcassRuntime.portion(loot,index,count);}
    public void clearOutputs() { outputs.clear(); }
    public void addOutput(ItemStack stack) { if(!stack.isEmpty()) outputs.add(stack.copy()); }
    List<ItemStack> outputs() { return outputs.stream().map(ItemStack::copy).toList(); }
}
