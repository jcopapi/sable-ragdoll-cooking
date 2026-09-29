package dev.jco.carcasses.integration;

import dev.leo.ragdollreactions.physics.MobDamageReactionHandler;
import dev.leo.ragdollreactions.physics.ReactionMobLauncher;
import dev.leo.sableplayerragdoll.mob.MobRagdollAssembly;
import dev.leo.sableplayerragdoll.mob.MobRagdollSavedData;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Routes fatal carcass hits through Ragdoll Reactions' installed damage/launch physics. */
public final class ReactionBridge {
    private static final ThreadLocal<Boolean> FORCE=ThreadLocal.withInitial(()->false);
    private static final Map<LivingEntity,Float> DAMAGE=new WeakHashMap<>();
    private ReactionBridge(){}
    public static boolean forced(){return FORCE.get();}
    public static void damage(LivingDamageEvent.Pre event){DAMAGE.put(event.getEntity(),event.getNewDamage());}
    public static boolean react(ServerLevel level,LivingEntity entity,DamageSource source,double multiplier){
        if(source==null)source=level.damageSources().generic();
        if(source.getSourcePosition()==null)source=new DamageSource(source.typeHolder(),source.getDirectEntity(),source.getEntity(),entity.position().add(0,-1,0));
        float amount=Math.max(1,DAMAGE.getOrDefault(entity,4F));
        FORCE.set(true);
        try {MobDamageReactionHandler.onMobDamaged(entity,source,(float)(amount*multiplier));}
        finally {FORCE.remove();ReactionMobLauncher.onMobReleased(entity,level);}
        var saved=MobRagdollSavedData.get(level).getEntry(entity.getUUID());
        if(saved!=null&&saved.durationTicks()!=Integer.MAX_VALUE){
            MobRagdollSavedData.get(level).addEntry(entity.getUUID(),saved.spawnedAtTick(),Integer.MAX_VALUE,saved.preRagdollPos(),saved.entityType(),entity.saveWithoutId(new CompoundTag()),saved.partInfos(),saved.partIds());
        }
        boolean launched=MobRagdollAssembly.isPendingOrConverted(entity.getUUID());
        if(launched)DAMAGE.remove(entity);
        return launched;
    }
}
