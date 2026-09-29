package dev.jco.carcasses;

import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import static dev.jco.carcasses.CarcassData.Phase.*;

public final class CarcassRuntime {
    public static final String MARKER="jco_carcasses:carrier";
    private static final ThreadLocal<CarcassData.Entry> RESOLVING=new ThreadLocal<>();
    private static Vec3 position(ServerLevel level,LivingEntity carrier){return dev.jco.carcasses.integration.BodyParts.worldPosition(level,carrier);}
    public static boolean isCarrier(Entity entity) {return entity.getPersistentData().getBoolean(MARKER);}
    public static void death(LivingDeathEvent event) {
        if(isCarrier(event.getEntity())) {event.setCanceled(true);event.getEntity().setHealth(event.getEntity().getMaxHealth());return;}
        if(!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level))return;
        var d=Carcasses.find(mob.getType());if(d==null)return;
        var adapter=Carcasses.adapters().stream().filter(a->(d.adapter()==null||d.adapter().equals(a.id()))&&a.supports(level,mob)).findFirst().orElse(null);
        if(adapter==null)return;
        var data=CarcassData.get(level);if(data.entries.containsKey(mob.getUUID())){event.setCanceled(true);return;}
        var e=new CarcassData.Entry();e.carrier=mob.getUUID();e.definition=d.id;e.adapter=adapter.id();e.fingerprint=d.fingerprint();
        e.original=mob.saveWithoutId(new CompoundTag());e.original.putString("id",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        e.original.putString("jco_damage_type",event.getSource().getMsgId());
        if(event.getSource().getEntity()!=null)e.original.putUUID("jco_attacker",event.getSource().getEntity().getUUID());
        e.passiveRemaining=d.passiveTicks();e.acceleratorCounts=new int[d.accelerators().size()];
        e.impactMultiplier=d.impact();data.entries.put(e.carrier,e);data.setDirty();
        // Cancel before vanilla sets DYING, sends the death animation, or removes the entity.
        event.setCanceled(true);RESOLVING.set(e);
        try {((dev.jco.carcasses.integration.mixin.LootAccess)mob).jco$resolveOriginalDeathLoot(level,event.getSource());}
        catch(RuntimeException error){e.failed=true;LogUtils.getLogger().error("Failed to capture original carcass loot {}",e.carrier,error);}
        finally {RESOLVING.remove();}
        mob.getPersistentData().putBoolean(MARKER,true);mob.setHealth(mob.getMaxHealth());mob.deathTime=0;
        mob.setNoAi(true);mob.setInvulnerable(true);mob.setPersistenceRequired();mob.setSilent(true);mob.clearFire();mob.setNoGravity(true);
        // Keep the hit pose visible while the client extracts Sable's model; Sable hides
        // this same entity when its ragdoll parts are ready, avoiding a blank frame.
        if(adapter.ready(level,mob)){adapter.impactExisting(level,mob,e.impactMultiplier);e.launched=true;}
        else e.launched=adapter.launch(level,mob,e.impactMultiplier);
        if(!e.launched)refund(e);
        sync(level,mob,e,d);data.setDirty();
    }
    public static void drops(LivingDropsEvent event) {
        var resolving=RESOLVING.get();
        if(resolving!=null && resolving.carrier.equals(event.getEntity().getUUID())) {
            resolving.loot=new ArrayList<>(event.getDrops().stream().map(i->i.getItem().copy()).toList());event.setCanceled(true);
        } else if(isCarrier(event.getEntity()))event.setCanceled(true);
    }
    public static void experience(LivingExperienceDropEvent event) {
        var resolving=RESOLVING.get();
        if(resolving!=null && resolving.carrier.equals(event.getEntity().getUUID())){resolving.experience=event.getDroppedExperience();event.setDroppedExperience(0);}
        else if(isCarrier(event.getEntity()))event.setDroppedExperience(0);
    }
    public static void damage(LivingIncomingDamageEvent event) {
        if(!isCarrier(event.getEntity()))return;
        event.setCanceled(true);
        if(event.getSource().getEntity() instanceof ServerPlayer player) attack(player,event.getEntity());
    }
    public static void attackEntity(AttackEntityEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity living && attack(player,living))event.setCanceled(true);
    }
    public static boolean attack(ServerPlayer player,LivingEntity carrier) {
        if(!isCarrier(carrier))return false;
        if(player.isSpectator()||!player.mayBuild()||player.level()!=carrier.level()||player.position().distanceToSqr(dev.jco.carcasses.integration.BodyParts.worldPosition(player.serverLevel(),carrier))>Math.pow(player.entityInteractionRange()+3,2))return true;
        var level=player.serverLevel();var data=CarcassData.get(level);var e=data.entries.get(carrier.getUUID());
        if(e==null||e.committing||e.finishing)return true;
        var current=Carcasses.definitions().get(e.definition);

        if(e.phase==COOKED){if(current!=null&&current.harvestInput().accepts(InteractionMode.LEFT))harvestInteraction(player,carrier,InteractionHand.MAIN_HAND);return true;}
        var definition=Carcasses.definitions().get(e.definition);
        if(definition==null)return true;
        e.lastAction=level.getGameTime();
        definition.harvestFeedback().emit(level,position(level,carrier),player,InteractionHand.MAIN_HAND,player.getMainHandItem(),true);
        e.outputPosition=safeOutput(level,position(level,carrier),player);
        refund(e);data.setDirty();var adapter=Carcasses.adapter(e.adapter);
        if(adapter!=null)finalize(level,carrier,e,adapter,data);
        return true;
    }
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if(!isCarrier(event.getTarget()))return;
        if(event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity carrier)interact(player,carrier,event.getHand());
        event.setCanceled(true);event.setCancellationResult(InteractionResult.CONSUME);
    }
    public static boolean interact(ServerPlayer player,LivingEntity carrier,InteractionHand hand) {
        if(!isCarrier(carrier))return false;
        if(hand!=InteractionHand.MAIN_HAND||player.isSpectator()||!player.mayBuild()||carrier.level()!=player.level()||player.position().distanceToSqr(dev.jco.carcasses.integration.BodyParts.worldPosition(player.serverLevel(),carrier))>Math.pow(player.blockInteractionRange()+2,2))return true;
        var level=player.serverLevel();var data=CarcassData.get(level);var e=data.entries.get(carrier.getUUID());
        if(e!=null&&!e.finishing){var def=Carcasses.definitions().get(e.definition);if(def!=null&&CarcassTransport.interact(player,carrier,hand,e,def))return true;}
        var inputDefinition=e==null?null:Carcasses.definitions().get(e.definition);
        if(player.getMainHandItem().isEmpty()&&!player.isShiftKeyDown())return false;
        if(inputDefinition==null||!inputDefinition.harvestInput().accepts(InteractionMode.RIGHT))return true;
        return harvestInteraction(player,carrier,hand);
    }
    private static boolean harvestInteraction(ServerPlayer player,LivingEntity carrier,InteractionHand hand){
        var level=player.serverLevel();var data=CarcassData.get(level);var e=data.entries.get(carrier.getUUID());
        if(e==null||e.phase!=COOKED||e.finishing||e.failed||e.committing||!e.outputs.isEmpty())return true;
        var d=Carcasses.definitions().get(e.definition);var adapter=Carcasses.adapter(e.adapter);
        if(d==null||adapter==null||!adapter.ready(level,carrier)||e.worker!=null||!elapsed(level.getGameTime(),e.lastAction,d.cooldown())||!validTool(player,d))return true;
        e.lastAction=level.getGameTime();e.hand=hand;
        if(d.actionTicks()>0){e.worker=player.getUUID();e.workTicks=0;}
        else harvest(level,carrier,e,d,player);
        data.setDirty();if(!carrier.isRemoved())sync(level,carrier,e,d);return true;
    }
    private static boolean validTool(ServerPlayer player,CarcassDefinition d) {
        var stack=player.getMainHandItem();
        return d.matchesTool(stack)&&stack.getCount()>=d.consume()&&(d.damage()==0||stack.isDamageableItem()&&stack.getMaxDamage()-stack.getDamageValue()>=d.damage());
    }
    public static List<ItemStack> portion(List<ItemStack> loot,int index,int count) {
        var out=new ArrayList<ItemStack>();
        int ordinal=0;
        for(var stack:loot){int n=0;for(int unit=0;unit<stack.getCount();unit++)if(ordinal++%count==index-1)n++;if(n>0)out.add(stack.copyWithCount(n));}return out;
    }
    private static void harvest(ServerLevel level,LivingEntity carrier,CarcassData.Entry e,CarcassDefinition d,ServerPlayer player) {
        if(player!=null&&!validTool(player,d)||e.committing||e.actions>=d.actions())return;
        e.committing=true;
        try {
            int index=e.actions+1;
            e.outputPosition=safeOutput(level,position(level,carrier),player);
            List<ItemStack> cookedLoot=d.cookedLoot(e.loot);
            List<ItemStack> defaults=d.harvest()!=null||d.finish()==null?portion(cookedLoot,index,d.actions()):index==d.actions()?cookedLoot:List.of();
            var context=new ProcessingContext(level,player,position(level,carrier),true,e.original,e.loot,defaults,index,d.actions());
            if(d.harvest()!=null)d.harvest().accept(context);else if(d.finish()!=null&&index==d.actions())d.finish().accept(context);
            var prepared=new ArrayList<>(context.outputs());
            prepared.addAll(FarmersDelight.bonus(e.loot,index,d.actions()));
            var stack=player==null?ItemStack.EMPTY:player.getMainHandItem();if(player!=null&&!validTool(player,d))throw new IllegalStateException("Harvest callback changed held tool");
            var presentation=stack.copyWithCount(1);
            if(player!=null&&!player.isCreative()){stack.shrink(d.consume());if(d.damage()>0&&!stack.isEmpty())stack.hurtAndBreak(d.damage(),player,EquipmentSlot.MAINHAND);}
            e.actions=index;e.outputs=prepared;e.worker=null;
            if(index>=d.actions()){e.phase=FINALIZING;e.finishing=true;}
            CarcassData.get(level).setDirty();
            flushOutputs(level,carrier,e);
            d.harvestFeedback().emit(level,position(level,carrier),player,e.hand,presentation,true);
        } catch(RuntimeException error) {
            e.failed=true;CarcassData.get(level).setDirty();
            LogUtils.getLogger().error("Carcass harvest {} failed; committed harvests retained. /jco_carcasses retry {}",e.definition,e.carrier,error);
            if(player!=null)player.displayClientMessage(Component.literal("Harvest paused; see server log. Completed harvests are retained."),true);
        } finally {e.committing=false;}
        if(e.finishing){var adapter=Carcasses.adapter(e.adapter);if(adapter!=null)finalize(level,carrier,e,adapter,CarcassData.get(level));}
    }
    public static void tick(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof LivingEntity carrier)||!isCarrier(carrier)||!(carrier.level() instanceof ServerLevel level))return;
        event.setCanceled(true);var data=CarcassData.get(level);var e=data.entries.get(carrier.getUUID());
        if(e==null){carrier.discard();return;}var adapter=Carcasses.adapter(e.adapter);if(adapter==null||e.committing)return;
        if(e.finishing){finalize(level,carrier,e,adapter,data);return;}
        var d=Carcasses.definitions().get(e.definition);if(d==null){refund(e);data.setDirty();finalize(level,carrier,e,adapter,data);return;}
        if(!e.fingerprint.equals(d.fingerprint())){e.worker=null;e.fingerprint=d.fingerprint();data.setDirty();}
        if(!adapter.ready(level,carrier)) {
            e.launchAge++;data.setDirty();
            if(!e.launched && e.launchAge>=3){e.launched=adapter.launch(level,carrier,e.impactMultiplier);if(!e.launched)refund(e);}
            else if(e.launchAge>240&&!adapter.pending(level,carrier)){LogUtils.getLogger().warn("Carcass {} could not obtain a Sable model; finalizing safely",e.carrier);refund(e);}
            return;
        }
        if(!e.outputs.isEmpty()){e.committing=true;try{flushOutputs(level,carrier,e);}finally{e.committing=false;}if(!e.outputs.isEmpty())return;}
        if(e.failed){if(level.getGameTime()%5==0)sync(level,carrier,e,d);return;}
        if(e.phase==COOKED && e.actions>=d.actions()){e.phase=FINALIZING;e.finishing=true;data.setDirty();return;}
        if(e.worker!=null) {
            var worker=level.getServer().getPlayerList().getPlayer(e.worker);
            if(worker==null||worker.level()!=level||worker.isSpectator()||!worker.mayBuild()||worker.position().distanceToSqr(position(level,carrier))>Math.pow(worker.blockInteractionRange()+2,2)||!validTool(worker,d)){e.worker=null;e.workTicks=0;}
            else if(++e.workTicks>=d.actionTicks()){e.worker=null;e.workTicks=0;harvest(level,carrier,e,d,worker);data.setDirty();}
        }
        boolean manipulated=CarcassTransport.tick(level,carrier,e,d);
        Vec3 bodyPosition=dev.jco.carcasses.integration.BodyParts.worldPosition(level,carrier);
        boolean wasHeating=e.activeHeat;e.activeHeat=false;double mounted=CarcassTransport.mountedHeat(level,e,d);e.rate=mounted>=0?mounted:heatRate(level,bodyPosition,d);
        if(!e.finishing && e.phase!=COOKED && d.cookingTicks()>0 && e.rate>0) {
            e.phase=COOKING;e.activeHeat=true;e.preciseCooking+=e.rate;e.cooking=(int)Math.floor(e.preciseCooking);data.setDirty();
            if(e.cooking>=d.cookingTicks()) {
                e.phase=COOKED;e.activeHeat=false;e.cooking=d.cookingTicks();e.preciseCooking=e.cooking;data.setDirty();
                d.cookedFeedback().emit(level,bodyPosition,null,InteractionHand.MAIN_HAND,ItemStack.EMPTY,false);
                sync(level,carrier,e,d);
            } else if(level.getGameTime()%10==0){d.cookingFeedback().emit(level,bodyPosition,null,InteractionHand.MAIN_HAND,ItemStack.EMPTY,false);
                if(level.getGameTime()%20==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,bodyPosition.x,bodyPosition.y+.4,bodyPosition.z,1,.15,.1,.15,.005);
            }
        }
        if(!e.finishing&&!e.failed)uncookedIdle(level,carrier,e,manipulated||e.activeHeat||e.worker!=null);
        if(!e.finishing&&!e.failed)passive(level,carrier,e,d,manipulated||e.activeHeat||e.worker!=null);
        if(!carrier.isRemoved() && (level.getGameTime()%5==0||wasHeating!=e.activeHeat))sync(level,carrier,e,d);
    }
    public static void passive(ServerLevel level,LivingEntity carrier,CarcassData.Entry e,CarcassDefinition d,boolean manipulated){
        if(d.passiveTicks()==0||e.finishing||e.failed||e.committing)return;long now=level.getGameTime();
        if(e.passiveRemaining<0)e.passiveRemaining=d.passiveTicks();
        var bodyPosition=position(level,carrier);boolean moving=e.lastPosition==null||e.lastPosition.distanceTo(bodyPosition)>d.settleTolerance();
        if(moving)e.lastPosition=bodyPosition;
        if(manipulated||moving){if(e.passiveDeadline>=0)e.passiveRemaining=Math.max(0,e.passiveDeadline-now);e.passiveDeadline=-1;e.stillTicks=0;CarcassData.get(level).setDirty();return;}
        if(e.passiveDeadline<0){if(++e.stillTicks>=d.settleTicks()){e.passiveDeadline=now+e.passiveRemaining;CarcassData.get(level).setDirty();}return;}
        if(now<e.passiveDeadline)return;
        if(e.phase==COOKED){harvest(level,carrier,e,d,null);}else{refund(e);var adapter=Carcasses.adapter(e.adapter);if(adapter!=null)finalize(level,carrier,e,adapter,CarcassData.get(level));}
    }
    public static void uncookedIdle(ServerLevel level,LivingEntity carrier,CarcassData.Entry e,boolean manipulated){
        uncookedIdle(level,carrier,e,manipulated,CookingConfig.UNCOOKED_DESPAWN_TICKS.get());
    }
    public static void uncookedIdle(ServerLevel level,LivingEntity carrier,CarcassData.Entry e,boolean manipulated,int limit){
        if(limit==0||e.phase==COOKED||e.finishing||e.failed||e.committing)return;
        var position=position(level,carrier);
        boolean moving=e.uncookedLastPosition==null||e.uncookedLastPosition.distanceToSqr(position)>.01;
        if(moving)e.uncookedLastPosition=position;
        if(manipulated||moving){if(e.uncookedIdleTicks!=0){e.uncookedIdleTicks=0;CarcassData.get(level).setDirty();}return;}
        if(++e.uncookedIdleTicks%20==0)CarcassData.get(level).setDirty();
        if(e.uncookedIdleTicks<limit)return;
        e.outputPosition=safeOutput(level,position,null);
        refund(e);CarcassData.get(level).setDirty();
        var adapter=Carcasses.adapter(e.adapter);
        if(adapter!=null)finalize(level,carrier,e,adapter,CarcassData.get(level));
    }
    private static void accelerate(ServerLevel level,LivingEntity carrier,CarcassData.Entry e,CarcassDefinition d,ServerPlayer player){
        if(e.failed||e.finishing||e.committing||e.passiveDeadline<0||e.activeHeat||!e.transport.isEmpty()||dev.jco.carcasses.integration.BodyParts.grabbed(e.carrier))return;
        if(e.acceleratorCounts.length!=d.accelerators().size())e.acceleratorCounts=new int[d.accelerators().size()];var stack=player.getMainHandItem();
        for(int i=0;i<d.accelerators().size();i++){var a=d.accelerators().get(i);if(e.acceleratorCounts[i]<0||!a.item.matches(stack)||!elapsed(level.getGameTime(),e.lastAction,a.cooldown())||stack.getCount()<a.consume()||a.damage()>0&&(!stack.isDamageableItem()||stack.getMaxDamage()-stack.getDamageValue()<a.damage()))continue;
            var tool=stack.copyWithCount(1);if(!player.isCreative()){stack.shrink(a.consume());if(a.damage()>0&&!stack.isEmpty())stack.hurtAndBreak(a.damage(),player,EquipmentSlot.MAINHAND);}e.lastAction=level.getGameTime();if(++e.acceleratorCounts[i]>=a.actions){e.passiveDeadline=Math.max(level.getGameTime(),e.passiveDeadline-a.reduction);e.acceleratorCounts[i]=a.repeatable()?0:-1;}
            a.feedback().emit(level,position(level,carrier),player,InteractionHand.MAIN_HAND,tool,true);CarcassData.get(level).setDirty();if(e.passiveDeadline<=level.getGameTime()){if(e.phase==COOKED)harvest(level,carrier,e,d,null);else{refund(e);var adapter=Carcasses.adapter(e.adapter);if(adapter!=null)finalize(level,carrier,e,adapter,CarcassData.get(level));}}return;
        }
    }
    public static boolean elapsed(long now,long last,int cooldown){return last==Long.MIN_VALUE||now>last&&now-last>=cooldown;}
    public static double heatRate(ServerLevel level,Vec3 center,CarcassDefinition d) {
        int r=(int)Math.ceil(d.heatRange());double rate=0;var origin=BlockPos.containing(center);
        for(var pos:BlockPos.betweenClosed(origin.offset(-r,-r,-r),origin.offset(r,r,r)))if(level.hasChunkAt(pos)&&d.heats(level.getBlockState(pos)))
            rate=Math.max(rate,d.heatRate(center.distanceTo(Vec3.atCenterOf(pos))));
        return rate;
    }
    private static boolean safe(ServerLevel level,Vec3 p){
        var at=BlockPos.containing(p);
        if(!level.getBlockState(at).getCollisionShape(level,at).isEmpty())return false;
        for(int y=0;y<4;y++){var s=level.getBlockState(at.below(y));if(s.is(net.minecraft.tags.BlockTags.FIRE)||s.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock||!s.getFluidState().isEmpty())return false;if(!s.getCollisionShape(level,at.below(y)).isEmpty())break;}
        return true;
    }
    public static Vec3 safeOutput(ServerLevel level,Vec3 body,ServerPlayer player){
        if(safe(level,body.add(0,.4,0)))return body.add(0,.4,0);
        for(int r=1;r<=4;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){if(Math.abs(x)!=r&&Math.abs(z)!=r)continue;var p=body.add(x,.5,z);if(safe(level,p))return p;}
        return body.add(0,1,0);
    }
    public static void refund(CarcassData.Entry e) {
        if(e.actions==0 && !e.finishing)e.outputs.addAll(e.loot.stream().map(ItemStack::copy).toList());
        e.finishing=true;e.phase=FINALIZING;e.worker=null;e.activeHeat=false;
    }
    private static void flushOutputs(ServerLevel level,LivingEntity carrier,CarcassData.Entry e) {
        while(!e.outputs.isEmpty()) {
            var stack=e.outputs.getFirst();
            if(e.outputPosition==null)e.outputPosition=safeOutput(level,position(level,carrier),null);
            var pos=e.outputPosition;
            if(!stack.isEmpty()){var drop=new ItemEntity(level,pos.x,pos.y,pos.z,stack.copy());drop.setDefaultPickUpDelay();drop.setDeltaMovement((level.random.nextDouble()-.5)*.1,.16,(level.random.nextDouble()-.5)*.1);if(!level.addFreshEntity(drop))return;}
            e.outputs.removeFirst();CarcassData.get(level).setDirty();
        }
    }
    private static void finalize(ServerLevel level,LivingEntity carrier,CarcassData.Entry e,CarcassAdapter adapter,CarcassData data) {
        if(e.committing)return;e.committing=true;
        try {
            if(e.transport.contains("mount")||e.transport.hasUUID("holder")||e.transport.hasUUID("carry"))CarcassTransport.release(level,carrier,e);
            if(e.outputPosition==null)e.outputPosition=safeOutput(level,position(level,carrier),null);
            flushOutputs(level,carrier,e);if(!e.outputs.isEmpty())return;
            if(!e.clientHidden){var hiddenDefinition=Carcasses.definitions().get(e.definition);if(hiddenDefinition!=null)sync(level,carrier,e,hiddenDefinition);e.clientHidden=true;}
            if(!adapter.finish(level,carrier)){var pendingDefinition=Carcasses.definitions().get(e.definition);if(pendingDefinition!=null&&level.getGameTime()%5==0)sync(level,carrier,e,pendingDefinition);return;}
            var d=Carcasses.definitions().get(e.definition);
            if(d!=null)d.removalFeedback().emit(level,position(level,carrier),null,InteractionHand.MAIN_HAND,ItemStack.EMPTY,false);
            if(e.experience>0)ExperienceOrb.award(level,position(level,carrier),e.experience);
            data.entries.remove(e.carrier);data.setDirty();
            var removed=new CarcassPayload(e.carrier,level.dimension().location().toString(),position(level,carrier),e.definition,e.actions,0,e.cooking,0,0,0,FINALIZING.name(),false,carrier.getId(),ItemStack.EMPTY);
            for(var player:level.players())if(player.position().distanceToSqr(removed.position())<4096)PacketDistributor.sendToPlayer(player,removed);
            carrier.discard();
        } finally {e.committing=false;}
    }
    private static void sync(ServerLevel level,LivingEntity carrier,CarcassData.Entry e,CarcassDefinition d) {
        var packet=new CarcassPayload(e.carrier,level.dimension().location().toString(),dev.jco.carcasses.integration.BodyParts.worldPosition(level,carrier),e.definition,e.actions,d.actions(),e.cooking,d.cookingTicks(),e.workTicks,d.actionTicks(),e.phase.name(),e.activeHeat,carrier.getId(),d.displayItem(),e.preciseCooking,e.rate,d.hudRange(),CarcassTransport.snapshot(level,e,d));
        for(var player:level.players())if(player.position().distanceToSqr(packet.position())<4096)PacketDistributor.sendToPlayer(player,packet);
    }
}
