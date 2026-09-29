package dev.jco.carcasses;

import java.util.*;
import net.minecraft.world.entity.EntityType;

public final class Carcasses {
    private static volatile Map<String,CarcassDefinition> definitions=Map.of();
    private static Map<String,CarcassDefinition> datapack=Map.of(), scripts=Map.of();
    private static final Map<String,CarcassAdapter> adapters=new LinkedHashMap<>();
    public static Map<String,CarcassDefinition> definitions() { return definitions; }
    /** Test/legacy replacement. Runtime reloads use the separate sources below. */
    public static synchronized void replace(Map<String,CarcassDefinition> value) {datapack=Map.of();scripts=Map.of();definitions=Collections.unmodifiableMap(new LinkedHashMap<>(value));}
    public static synchronized void replaceDatapack(Map<String,CarcassDefinition> value){datapack=Collections.unmodifiableMap(new LinkedHashMap<>(value));merge();}
    public static synchronized void replaceScripts(Map<String,CarcassDefinition> value){scripts=Collections.unmodifiableMap(new LinkedHashMap<>(value));merge();}
    public static synchronized void clearSources(){datapack=Map.of();scripts=Map.of();definitions=Map.of();}
    private static void merge(){var next=new LinkedHashMap<String,CarcassDefinition>();
        // Script entries lead selector lookup; matching IDs replace bundled datapack entries.
        next.putAll(scripts);datapack.forEach(next::putIfAbsent);definitions=Collections.unmodifiableMap(next);
    }
    public static CarcassDefinition find(EntityType<?> type) {return definitions.values().stream().filter(d->!d.mobSelector().startsWith("#")&&d.matches(type)).findFirst().orElseGet(()->definitions.values().stream().filter(d->d.matches(type)).findFirst().orElse(null));}
    public static void registerAdapter(CarcassAdapter adapter) {
        if(adapters.putIfAbsent(adapter.id(),adapter)!=null) throw new IllegalArgumentException("Duplicate carcass adapter");
    }
    public static Collection<CarcassAdapter> adapters() { return Collections.unmodifiableCollection(adapters.values()); }
    public static CarcassAdapter adapter(String id) { return adapters.get(id); }
}
