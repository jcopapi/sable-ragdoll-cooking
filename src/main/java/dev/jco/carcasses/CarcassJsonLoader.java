package dev.jco.carcasses;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;

/** Definitions in data/<namespace>/sable_ragdoll_cooking/*.json. */
public final class CarcassJsonLoader extends SimpleJsonResourceReloadListener {
    public CarcassJsonLoader(){super(new Gson(),"sable_ragdoll_cooking");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager resources,ProfilerFiller profiler){
        var definitions=new LinkedHashMap<String,CarcassDefinition>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try{var definition=parse(entry.getKey(),entry.getValue());if(definition!=null)definitions.put(definition.id,definition);}
            catch(RuntimeException error){LogUtils.getLogger().error("Invalid Sable Ragdoll: Cooking recipe {}",entry.getKey(),error);}
        });
        Carcasses.replaceDatapack(definitions);
        LogUtils.getLogger().info("Loaded {} Sable Ragdoll: Cooking recipes",definitions.size());
    }
    public static CarcassDefinition parse(ResourceLocation id,JsonElement json){
        if(!json.isJsonObject())throw new IllegalArgumentException("Expected JSON object");
        var root=json.getAsJsonObject();
        if(!bool(root,"enabled",true))return null;
        for(var mod:array(root,"required_mods"))if(!ModList.get().isLoaded(mod.getAsString()))return null;
        var d=new CarcassDefinition(id.toString()).mob(string(root,"mob"));
        if(root.has("adapter"))d.adapter(string(root,"adapter"));
        if(root.has("tool"))d.tool(string(root,"tool"),integer(root,"harvest_count",1));
        else if(root.has("harvest_count"))d.harvestCount(integer(root,"harvest_count",1));
        if(root.has("harvest_input"))d.harvestInput(string(root,"harvest_input"));
        if(root.has("action_ticks"))d.actionTicks(integer(root,"action_ticks",0));
        if(root.has("durability"))d.durability(integer(root,"durability",0));
        if(root.has("consume"))d.consume(integer(root,"consume",0));
        if(root.has("action_cooldown"))d.actionCooldown(integer(root,"action_cooldown",10));
        if(root.has("cooking_ticks"))d.cookingTicks(integer(root,"cooking_ticks",0));
        for(var heat:array(root,"heat"))d.heat(heat.getAsString());
        if(root.has("lit_only"))d.litOnly(bool(root,"lit_only",true));
        if(root.has("heat_distance")){var n=object(root.get("heat_distance"));d.heatDistance(number(n,"near",.5),number(n,"range",3),number(n,"maximum",2),number(n,"curve",1));}
        if(root.has("hud_range"))d.hudRange(number(root,"hud_range",6));
        if(root.has("impact_multiplier"))d.impactMultiplier(number(root,"impact_multiplier",1));
        if(root.has("passive_resolution"))d.passiveResolution(integer(root,"passive_resolution",0));
        if(root.has("settle_ticks"))d.settleTicks(integer(root,"settle_ticks",40));
        if(root.has("settle_tolerance"))d.settleTolerance(number(root,"settle_tolerance",.04));
        if(root.has("cooking_mount")){var n=object(root.get("cooking_mount"));d.cookingMount(string(n,"item"),string(n,"heat"),number(n,"height",2));}
        if(root.has("cooked_loot_multiplier"))d.cookedLootMultiplier(number(root,"cooked_loot_multiplier",1));
        for(var raw:array(root,"cooked_drops")){var n=object(raw);d.cookedDrop(string(n,"source"),string(n,"result"),number(n,"multiplier",1));}
        for(var raw:array(root,"accelerators")){var n=object(raw);d.acceleratorTool(string(n,"item"),integer(n,"actions",1),integer(n,"reduction",1),a->{
            if(n.has("durability"))a.durability(integer(n,"durability",0));if(n.has("consume"))a.consume(integer(n,"consume",0));
            if(n.has("cooldown"))a.actionCooldown(integer(n,"cooldown",10));if(n.has("repeatable"))a.repeatable(bool(n,"repeatable",true));
            if(n.has("feedback"))configure(a.feedback(),object(n.get("feedback")));
        });}
        if(root.has("cooking_feedback"))configure(d.cookingFeedback(),object(root.get("cooking_feedback")));
        if(root.has("cooked_feedback"))configure(d.cookedFeedback(),object(root.get("cooked_feedback")));
        if(root.has("harvest_feedback"))configure(d.harvestFeedback(),object(root.get("harvest_feedback")));
        if(root.has("removal_feedback"))configure(d.removalFeedback(),object(root.get("removal_feedback")));
        d.freeze();return d;
    }
    private static void configure(Feedback feedback,JsonObject n){
        if(n.has("display_item")){var id=ResourceLocation.parse(string(n,"display_item"));if(!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException("Unknown display item: "+id);feedback.displayItem(BuiltInRegistries.ITEM.get(id).getDefaultInstance());}
        if(n.has("sound")){var s=object(n.get("sound"));feedback.sound(string(s,"id"),(float)number(s,"volume",.6),(float)number(s,"pitch",1));}
        if(n.has("particles")){var p=object(n.get("particles"));feedback.particles(string(p,"id"),integer(p,"count",5),number(p,"spread",.18),number(p,"speed",.03));}
    }
    private static JsonObject object(JsonElement value){if(value==null||!value.isJsonObject())throw new IllegalArgumentException("Expected JSON object");return value.getAsJsonObject();}
    private static JsonArray array(JsonObject root,String name){if(!root.has(name))return new JsonArray();if(!root.get(name).isJsonArray())throw new IllegalArgumentException("Expected array: "+name);return root.getAsJsonArray(name);}
    private static String string(JsonObject root,String name){if(!root.has(name)||!root.get(name).isJsonPrimitive())throw new IllegalArgumentException("Expected string: "+name);return root.get(name).getAsString();}
    private static int integer(JsonObject root,String name,int fallback){return root.has(name)?root.get(name).getAsInt():fallback;}
    private static double number(JsonObject root,String name,double fallback){return root.has(name)?root.get(name).getAsDouble():fallback;}
    private static boolean bool(JsonObject root,String name,boolean fallback){return root.has(name)?root.get(name).getAsBoolean():fallback;}
}
