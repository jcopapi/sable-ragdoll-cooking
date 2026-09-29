package dev.jco.carcasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Optional content bridge: no Farmer's Delight classes are linked by this mod. */
public final class FarmersDelight {
    private static final ResourceLocation KNIFE=ResourceLocation.parse("farmersdelight:iron_knife");
    private static final TagKey<net.minecraft.world.item.Item> KNIVES=TagKey.create(Registries.ITEM,ResourceLocation.parse("c:tools/knife"));
    private static final Map<String,String> CUTS=Map.ofEntries(
        Map.entry("minecraft:beef","farmersdelight:minced_beef"),
        Map.entry("minecraft:cooked_beef","farmersdelight:minced_beef"),
        Map.entry("minecraft:porkchop","farmersdelight:bacon"),
        Map.entry("minecraft:cooked_porkchop","farmersdelight:bacon"),
        Map.entry("minecraft:chicken","farmersdelight:chicken_cuts"),
        Map.entry("minecraft:cooked_chicken","farmersdelight:chicken_cuts"),
        Map.entry("minecraft:mutton","farmersdelight:mutton_chops"),
        Map.entry("minecraft:cooked_mutton","farmersdelight:mutton_chops"),
        Map.entry("minecraft:cod","farmersdelight:cod_slice"),
        Map.entry("minecraft:cooked_cod","farmersdelight:cod_slice"),
        Map.entry("minecraft:salmon","farmersdelight:salmon_slice"),
        Map.entry("minecraft:cooked_salmon","farmersdelight:salmon_slice"));
    private FarmersDelight(){}
    public static boolean loaded(){return ModList.get().isLoaded("farmersdelight");}
    public static boolean useKnife(String tool){return loaded()&&"#minecraft:swords".equals(tool);}
    public static boolean knife(ItemStack stack){return stack.is(KNIVES);}
    public static ItemStack displayKnife(){return BuiltInRegistries.ITEM.get(KNIFE).getDefaultInstance();}
    public static List<ItemStack> bonus(List<ItemStack> captured,int index,int actions){
        if(!loaded())return List.of();
        var result=new ArrayList<ItemStack>();
        for(var stack:captured){
            var id=CUTS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            if(id==null)continue;
            var target=ResourceLocation.parse(id);
            if(!BuiltInRegistries.ITEM.containsKey(target))continue;
            int amount=Math.max(1,(stack.getCount()+1)/2);
            result.add(new ItemStack(BuiltInRegistries.ITEM.get(target),amount));
        }
        return CarcassRuntime.portion(result,index,actions);
    }
}
