package dev.jco.carcasses;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public final class CarcassData extends SavedData {
    public enum Phase {RAW,COOKING,COOKED,FINALIZING}
    public static final class Entry {
        public String definition, adapter, fingerprint;
        public UUID carrier, worker;
        public CompoundTag original=new CompoundTag();
        public List<ItemStack> loot=new ArrayList<>(), outputs=new ArrayList<>();
        public int actions, cooking, launchAge, workTicks;
        public boolean launched, finishing, failed;
        public long lastAction=Long.MIN_VALUE,passiveDeadline=-1,passiveRemaining=-1;public int stillTicks,uncookedIdleTicks;public int[] acceleratorCounts=new int[0];public net.minecraft.world.phys.Vec3 lastPosition,uncookedLastPosition;public CompoundTag transport=new CompoundTag();
        public Phase phase=Phase.RAW;
        public double impactMultiplier=1;
        public int experience;
        public double preciseCooking,rate;
        public net.minecraft.world.phys.Vec3 outputPosition;
        public boolean activeHeat,committing,clientHidden;
        public net.minecraft.world.InteractionHand hand=net.minecraft.world.InteractionHand.MAIN_HAND;
    }
    public final Map<UUID,Entry> entries=new HashMap<>();
    public static CarcassData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(CarcassData::new,CarcassData::load),"jco_carcasses");
    }
    public static CarcassData load(CompoundTag tag, HolderLookup.Provider lookup) {
        var data=new CarcassData();
        for(var raw:tag.getList("entries",Tag.TAG_COMPOUND)) {
            var n=(CompoundTag)raw; var e=new Entry();
            e.carrier=n.getUUID("carrier"); e.definition=n.getString("definition"); e.adapter=n.getString("adapter"); e.fingerprint=n.getString("fingerprint");
            e.original=n.getCompound("original"); e.actions=n.getInt("actions"); e.cooking=n.getInt("cooking");
            e.launchAge=n.getInt("launchAge"); e.launched=n.getBoolean("launched"); e.finishing=n.getBoolean("finishing"); e.failed=n.getBoolean("failed");
            try {e.phase=Phase.valueOf(n.getString("phase"));}catch(IllegalArgumentException ex){e.phase=e.finishing?Phase.FINALIZING:Phase.RAW;e.actions=0;}
            e.passiveDeadline=n.contains("passiveDeadline")?n.getLong("passiveDeadline"):-1;e.passiveRemaining=n.contains("passiveRemaining")?n.getLong("passiveRemaining"):-1;e.stillTicks=n.getInt("stillTicks");e.uncookedIdleTicks=n.getInt("uncookedIdleTicks");e.acceleratorCounts=n.getIntArray("accelerators");e.transport=n.getCompound("transport");e.transport.remove("carry");
            e.lastAction=n.contains("lastAction")?n.getLong("lastAction"):Long.MIN_VALUE;e.preciseCooking=n.contains("preciseCooking")?n.getDouble("preciseCooking"):e.cooking;
            if(n.contains("uncookedX"))e.uncookedLastPosition=new net.minecraft.world.phys.Vec3(n.getDouble("uncookedX"),n.getDouble("uncookedY"),n.getDouble("uncookedZ"));
            if(n.contains("outputX"))e.outputPosition=new net.minecraft.world.phys.Vec3(n.getDouble("outputX"),n.getDouble("outputY"),n.getDouble("outputZ"));
            e.impactMultiplier=n.contains("impactMultiplier")?n.getDouble("impactMultiplier"):1;e.experience=n.getInt("xp");
            for(var item:n.getList("loot",Tag.TAG_COMPOUND)) e.loot.add(ItemStack.parseOptional(lookup,(CompoundTag)item));
            for(var item:n.getList("outputs",Tag.TAG_COMPOUND)) e.outputs.add(ItemStack.parseOptional(lookup,(CompoundTag)item));
            data.entries.put(e.carrier,e);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        var list=new ListTag();
        entries.values().forEach(e -> {
            var n=new CompoundTag(); n.putUUID("carrier",e.carrier); n.putString("definition",e.definition); n.putString("adapter",e.adapter); n.putString("fingerprint",e.fingerprint);
            n.put("original",e.original.copy()); n.putInt("actions",e.actions); n.putInt("cooking",e.cooking); n.putInt("launchAge",e.launchAge);
            n.putBoolean("launched",e.launched); n.putBoolean("finishing",e.finishing); n.putBoolean("failed",e.failed);
            n.putLong("passiveDeadline",e.passiveDeadline);n.putLong("passiveRemaining",e.passiveRemaining);n.putInt("stillTicks",e.stillTicks);n.putInt("uncookedIdleTicks",e.uncookedIdleTicks);n.putIntArray("accelerators",e.acceleratorCounts);n.put("transport",e.transport.copy());n.putDouble("preciseCooking",e.preciseCooking);n.putLong("lastAction",e.lastAction);if(e.outputPosition!=null){n.putDouble("outputX",e.outputPosition.x);n.putDouble("outputY",e.outputPosition.y);n.putDouble("outputZ",e.outputPosition.z);}
            if(e.uncookedLastPosition!=null){n.putDouble("uncookedX",e.uncookedLastPosition.x);n.putDouble("uncookedY",e.uncookedLastPosition.y);n.putDouble("uncookedZ",e.uncookedLastPosition.z);}
            n.putString("phase",e.phase.name());n.putInt("xp",e.experience);n.putDouble("impactMultiplier",e.impactMultiplier);
            var loot=new ListTag(); e.loot.forEach(i->{ if(!i.isEmpty()) loot.add(i.save(lookup)); }); n.put("loot",loot);
            var outputs=new ListTag(); e.outputs.forEach(i->{ if(!i.isEmpty()) outputs.add(i.save(lookup)); }); n.put("outputs",outputs); list.add(n);
        });
        tag.put("entries",list); return tag;
    }
}
