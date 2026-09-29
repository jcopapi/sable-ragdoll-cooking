package dev.jco.carcasses.kubejs;

import dev.jco.carcasses.*;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.*;
import java.util.*;
import java.util.function.Consumer;

public final class CarcassPlugin implements KubeJSPlugin {
    private Map<String,CarcassDefinition> candidate;
    private boolean invalid;private Integer fuelMultiplier;
    private long generation;
    @Override public void beforeScriptsLoaded(ScriptManager m) {
        if(m.scriptType==ScriptType.SERVER) { candidate=new LinkedHashMap<>();fuelMultiplier=null; invalid=false; generation++; }
    }
    @Override public void afterScriptsLoaded(ScriptManager m) {
        if(m.scriptType==ScriptType.SERVER) {
            if(!invalid && m.scriptType.console.errors.isEmpty()){Carcasses.replaceScripts(candidate);CampfireFuel.multiplier=fuelMultiplier==null?-1:fuelMultiplier;}
            else m.scriptType.console.warn("Sable Ragdoll: Cooking: script reload failed; previous definitions retained");
            candidate=null;
        }
    }
    @Override public void registerBindings(BindingRegistry b) {
        if(b.type()==ScriptType.SERVER) b.add("Carcasses",new Api(generation));
    }
    public final class Api {
        private final long current;
        private Api(long current) { this.current=current; }
        public void create(String id, Consumer<CarcassDefinition> configure) {
            if(candidate==null || current!=generation) throw new IllegalStateException("create belongs at top level in server_scripts");
            try {
                var d=new CarcassDefinition(id); configure.accept(d); d.freeze();
                if(candidate.values().stream().anyMatch(v->v.mobSelector().equals(d.mobSelector()))) throw new IllegalArgumentException("Duplicate mob selector");
                if(candidate.putIfAbsent(d.id,d)!=null) throw new IllegalArgumentException("Duplicate carcass id");
            } catch(RuntimeException ex) { invalid=true; throw ex; }
        }
        public void campfireFuel(int furnaceMultiplier){if(candidate==null||current!=generation)throw new IllegalStateException("campfireFuel belongs at top level");if(furnaceMultiplier<0||furnaceMultiplier>1000){invalid=true;throw new IllegalArgumentException("Fuel multiplier 0..1000 (0 disables)");}fuelMultiplier=furnaceMultiplier;}
        public List<String> ids() { return List.copyOf(Carcasses.definitions().keySet()); }
    }
}
