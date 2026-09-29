package dev.jco.carcasses;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CarcassDefinition {
    private InteractionMode input=InteractionMode.LEFT;public CarcassDefinition harvestInput(String value){edit();input=InteractionMode.parse(value);return this;}public InteractionMode harvestInput(){return input;}
    public final String id;
    private String mob, tool, adapter;
    private int actions=1, actionTicks=0, damage=0, consume=0, cookingTicks=0;
    private final List<String> heat=new ArrayList<>();
    public record CookedDrop(Item source, Item result, double multiplier) {}
    private final List<CookedDrop> cookedDrops=new ArrayList<>();
    private double cookedLootMultiplier=1;
    private boolean litOnly=true, frozen;
    private Consumer<ProcessingContext> finish;
    private Consumer<ProcessingContext> harvest;
    private double impact=1,heatRange=3,heatNear=.5,heatMax=2,heatCurve=1,hudRange=6;
    private int cooldown=10,passiveTicks=0,settleTicks=40;private double settleTolerance=.04;
    private final List<Accelerator> accelerators=new ArrayList<>();private String mountItem,mountHeat;private double mountHeight=2;
    public CarcassDefinition passiveResolution(int ticks){edit();positive(ticks);passiveTicks=ticks;return this;}public int passiveTicks(){return passiveTicks;}
    public CarcassDefinition settleTicks(int ticks){edit();positive(ticks);settleTicks=ticks;return this;}public int settleTicks(){return settleTicks;}
    public CarcassDefinition settleTolerance(double blocks){edit();if(!Double.isFinite(blocks)||blocks<.001||blocks>.5)throw new IllegalArgumentException("Settle tolerance .001..0.5");settleTolerance=blocks;return this;}public double settleTolerance(){return settleTolerance;}
    public CarcassDefinition acceleratorTool(String selector,int actions,int ticks,Consumer<Accelerator> configure){edit();if(accelerators.size()>=16)throw new IllegalArgumentException("Max 16 accelerators");var a=new Accelerator(selector,actions,ticks,1,0);configure.accept(a);a.freeze();accelerators.add(a);return this;}public List<Accelerator> accelerators(){return Collections.unmodifiableList(accelerators);}
    public CarcassDefinition cookingMount(String attachment,String source,double height){edit();key(attachment);key(source);if(!Double.isFinite(height)||height<.5||height>5)throw new IllegalArgumentException("Mount height .5..5");mountItem=attachment;mountHeat=source;mountHeight=height;return this;}public double mountHeight(){return mountHeight;}
    public boolean mountItem(ItemStack stack){return mountItem!=null&&new StackMatcher(mountItem).matches(stack);}public boolean mountSource(BlockState state){return mountHeat!=null&&(mountHeat.startsWith("#")?state.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse(mountHeat.substring(1)))):state.is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(mountHeat))));}

    private final Feedback cookingFeedback=new Feedback().sound("",1,1).particles("minecraft:small_flame",2,.3,.01);
    private final Feedback cookedFeedback=new Feedback().sound("minecraft:entity.experience_orb.pickup",.6F,1.2F).particles("",0,0,0);
    private final Feedback harvestFeedback=new Feedback().sound("minecraft:entity.sheep.shear",.6F,.8F).particles("simpleblood:blood",12,.28,.09);
    private final Feedback removalFeedback=new Feedback().sound("",1,1).particles("minecraft:poof",10,.3,.02);
    public CarcassDefinition(String id) { this.id=ResourceLocation.parse(id).toString(); }
    private void edit() { if(frozen) throw new IllegalStateException("Definition is frozen"); }
    private static String key(String id) { ResourceLocation.parse(id.startsWith("#")?id.substring(1):id); return id; }
    public CarcassDefinition mob(String id) {
        edit(); key(id);
        if(!id.startsWith("#") && !BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(id))) throw new IllegalArgumentException("Unknown mob: "+id);
        mob=id; return this;
    }
    public CarcassDefinition tool(String id, int actions) {
        edit(); key(id); positive(actions);
        if(!id.startsWith("#") && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))) throw new IllegalArgumentException("Unknown item: "+id);
        tool=id; this.actions=actions; return this;
    }
    public CarcassDefinition actionTicks(int ticks) { edit(); nonnegative(ticks); actionTicks=ticks; return this; }
    public CarcassDefinition durability(int damage) { edit(); nonnegative(damage); this.damage=damage; return this; }
    public CarcassDefinition consume(int count) { edit(); nonnegative(count); if(count>64) throw new IllegalArgumentException("consume <= 64"); consume=count; return this; }
    public CarcassDefinition heat(String blockOrTag) {
        edit(); key(blockOrTag);
        if(!blockOrTag.startsWith("#") && !BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(blockOrTag))) throw new IllegalArgumentException("Unknown heat block: "+blockOrTag);
        heat.add(blockOrTag); return this;
    }
    public CarcassDefinition litOnly(boolean value) { edit(); litOnly=value; return this; }
    public CarcassDefinition cookingTicks(int ticks) { edit(); positive(ticks); cookingTicks=ticks; return this; }
    /** Multiplies captured vanilla drops on cooked harvest, without rerolling the loot table. */
    public CarcassDefinition cookedLootMultiplier(double value) {edit();multiplier(value);cookedLootMultiplier=value;return this;}
    public CarcassDefinition cookedDrop(String source,String result,double multiplier) {
        edit();multiplier(multiplier);
        var from=ResourceLocation.parse(source);var to=ResourceLocation.parse(result);
        if(!BuiltInRegistries.ITEM.containsKey(from)||!BuiltInRegistries.ITEM.containsKey(to))throw new IllegalArgumentException("Unknown cooked drop item: "+source+" -> "+result);
        if(cookedDrops.stream().anyMatch(drop->drop.source()==BuiltInRegistries.ITEM.get(from)))throw new IllegalArgumentException("Duplicate cooked drop: "+source);
        cookedDrops.add(new CookedDrop(BuiltInRegistries.ITEM.get(from),BuiltInRegistries.ITEM.get(to),multiplier));return this;
    }
    public List<CookedDrop> cookedDrops(){return List.copyOf(cookedDrops);}
    public List<ItemStack> cookedLoot(List<ItemStack> original){
        var result=new ArrayList<ItemStack>();
        for(var stack:original){
            if(stack.isEmpty())continue;
            var conversion=cookedDrops.stream().filter(drop->stack.is(drop.source())).findFirst().orElse(null);
            double factor=conversion==null?cookedLootMultiplier:conversion.multiplier();
            long total=(long)Math.ceil(stack.getCount()*factor);
            if(total>100000)throw new IllegalArgumentException("Cooked loot exceeds safety limit");
            while(total>0){int count=(int)Math.min(total,stack.getMaxStackSize());
                result.add(conversion==null?stack.copyWithCount(count):stack.transmuteCopy(conversion.result(),count));total-=count;}
        }
        return result;
    }
    public CarcassDefinition onFinish(Consumer<ProcessingContext> callback) { edit(); finish=Objects.requireNonNull(callback); return this; }
    public CarcassDefinition onHarvest(Consumer<ProcessingContext> callback) {edit();harvest=Objects.requireNonNull(callback);return this;}
    public CarcassDefinition harvestCount(int count) {edit();positive(count);actions=count;return this;}
    public CarcassDefinition impactMultiplier(double value) {edit();if(!Double.isFinite(value)||value<0||value>4)throw new IllegalArgumentException("impactMultiplier must be 0..4");impact=value;return this;}
    public CarcassDefinition cookingFeedback(Consumer<Feedback> configure) {edit();configure.accept(cookingFeedback);return this;}
    public CarcassDefinition cookedFeedback(Consumer<Feedback> configure) {edit();configure.accept(cookedFeedback);return this;}
    public CarcassDefinition harvestFeedback(Consumer<Feedback> configure) {edit();configure.accept(harvestFeedback);return this;}
    public CarcassDefinition removalFeedback(Consumer<Feedback> configure) {edit();configure.accept(removalFeedback);return this;}
    public Feedback cookingFeedback() {return cookingFeedback;}
    public Feedback cookedFeedback() {return cookedFeedback;}
    public Feedback harvestFeedback() {return harvestFeedback;}
    public Feedback removalFeedback() {return removalFeedback;}
    public Consumer<ProcessingContext> harvest() {return harvest;}
    public double impact() {return impact;}
    public CarcassDefinition actionCooldown(int ticks){edit();nonnegative(ticks);cooldown=ticks;return this;}
    public int cooldown(){return cooldown;}
    public CarcassDefinition hudRange(double range){edit();if(!Double.isFinite(range)||range<1||range>32)throw new IllegalArgumentException("hudRange 1..32");hudRange=range;return this;}
    public double hudRange(){return hudRange;}
    public CarcassDefinition heatDistance(double near,double range,double maximum,double curve){edit();if(!Double.isFinite(near+range+maximum+curve)||near<0||range<=near||range>8||maximum<=0||maximum>20||curve<=0||curve>8)throw new IllegalArgumentException("Invalid heat curve");heatNear=near;heatRange=range;heatMax=maximum;heatCurve=curve;return this;}
    public double heatRange(){return heatRange;}
    public double heatRate(double distance){return distance>=heatRange?0:heatMax*Math.pow(1-Math.clamp((distance-heatNear)/(heatRange-heatNear),0,1),heatCurve);}
    public CarcassDefinition adapter(String id) { edit(); adapter=Objects.requireNonNull(id); return this; }
    public String adapter() { return adapter; }
    public void freeze() {
        if(mob==null || (cookingTicks>0 && (heat.isEmpty() || tool==null))) throw new IllegalArgumentException(id+": mob required; cooking needs heat and harvest tool");
        if(!accelerators.isEmpty()&&passiveTicks==0)throw new IllegalArgumentException("Accelerators require passiveResolution");cookingFeedback.freeze();cookedFeedback.freeze();harvestFeedback.freeze();removalFeedback.freeze();
        frozen=true;
    }
    public boolean matches(EntityType<?> type) {
        return mob.startsWith("#") ? type.is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse(mob.substring(1))))
            : BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(ResourceLocation.parse(mob));
    }
    public boolean matchesTool(ItemStack stack) {
        if(tool==null || stack.isEmpty()) return false;
        if(FarmersDelight.useKnife(tool))return FarmersDelight.knife(stack);
        return tool.startsWith("#") ? stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(tool.substring(1))))
            : stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(tool)));
    }
    public boolean heats(BlockState state) {
        if(litOnly && state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return false;
        return heat.stream().anyMatch(s -> s.startsWith("#") ? state.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse(s.substring(1))))
            : state.is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s))));
    }
    public int actions() { return actions; }
    public int actionTicks() { return actionTicks; }
    public int damage() { return damage; }
    public int consume() { return consume; }
    public int cookingTicks() { return cookingTicks; }
    public Consumer<ProcessingContext> finish() { return finish; }
    public String mobSelector() { return mob; }
    public ItemStack displayItem() {
        if(FarmersDelight.useKnife(tool))return harvestFeedback.display(FarmersDelight.displayKnife());
        var fallback=tool==null?ItemStack.EMPTY:"#minecraft:swords".equals(tool)?net.minecraft.world.item.Items.IRON_SWORD.getDefaultInstance():tool.startsWith("#")
            ?BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,ResourceLocation.parse(tool.substring(1)))).flatMap(tag->tag.stream().findFirst()).map(holder->holder.value().getDefaultInstance()).orElse(ItemStack.EMPTY)
            :BuiltInRegistries.ITEM.get(ResourceLocation.parse(tool)).getDefaultInstance();
        return harvestFeedback.display(fallback);
    }
    public String fingerprint() { return mob+"|"+tool+"|"+actions+"|"+actionTicks+"|"+damage+"|"+consume+"|"+heat+"|"+litOnly+"|"+cookingTicks+"|"+adapter+"|"+cookedLootMultiplier+"|"+cookedDrops; }
    private static void multiplier(double value){if(!Double.isFinite(value)||value<0||value>10)throw new IllegalArgumentException("Cooked loot multiplier must be 0..10");}
    private static void nonnegative(int n) { if(n<0 || n>1000000000) throw new IllegalArgumentException("Value must be 0..1000000000"); }
    private static void positive(int n) { nonnegative(n); if(n==0) throw new IllegalArgumentException("Value must be positive"); }
}
