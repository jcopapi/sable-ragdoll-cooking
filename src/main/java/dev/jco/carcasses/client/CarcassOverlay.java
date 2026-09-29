package dev.jco.carcasses.client;

import dev.jco.carcasses.CarcassPayload;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid="jco_carcasses",value=Dist.CLIENT)
public final class CarcassOverlay {
    private record Recent(CarcassPayload value,long received) {}
    private static final Map<UUID,Recent> recent=new HashMap<>();
    private static final Set<UUID> vanished=new HashSet<>();
    private static final Map<UUID,Motion> motions=new HashMap<>();
    private static final class Motion{long time=System.nanoTime();float alpha,progress,pulse;int actions=-1;long remaining=-1;}

    private static UUID carrying;
    @SubscribeEvent public static void carryInput(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event){
        var mc=Minecraft.getInstance();if(event.isCanceled()||!event.isUseItem()||event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||mc.player==null||mc.level==null||mc.player.isShiftKeyDown())return;
        var held=mc.player.getMainHandItem();if(held.isEmpty()||!held.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse("jco:leashes")))&&!held.is(net.minecraft.world.item.Items.LEAD))return;
        var start=mc.player.getEyePosition();var end=start.add(mc.player.getLookAngle().scale(mc.player.blockInteractionRange()+1));Recent nearest=null;double best=Double.MAX_VALUE;
        for(var r:recent.values()){var p=r.value;if(System.nanoTime()-r.received>1_500_000_000L||!p.dimension().equals(mc.level.dimension().location().toString()))continue;var body=mc.level.getEntity(p.entityId());if(body==null)continue;var box=body.getBoundingBox().inflate(.8);var point=box.clip(start,end);if(point.isEmpty()&&!box.contains(start))continue;var target=point.orElse(start);var obstruction=mc.level.clip(new net.minecraft.world.level.ClipContext(start,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));if(obstruction.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&obstruction.getLocation().distanceToSqr(start)+.04<target.distanceToSqr(start))continue;double distance=target.distanceToSqr(start);if(distance<best){best=distance;nearest=r;}}
        if(nearest==null)return;event.setCanceled(true);event.setSwingHand(false);if(carrying!=null)return;net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.jco.carcasses.CarryPayload(nearest.value.id(),false));if(held.isEmpty())carrying=nearest.value.id();
    }
    @SubscribeEvent public static void releaseCarry(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){var mc=Minecraft.getInstance();if(carrying!=null&&(mc.player==null||mc.screen!=null||!mc.options.keyUse.isDown())){if(mc.getConnection()!=null)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.jco.carcasses.CarryPayload(carrying,true));carrying=null;}}
    /** Expanded target for Sable's own grab packet and physics; never starts a parallel carrying mode. */
    public static net.minecraft.core.BlockPos extendedGrabTarget(Minecraft mc){
        if(mc.player==null||mc.level==null)return null;
        var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(mc.level);if(container==null)return null;
        var start=mc.player.getEyePosition();var end=start.add(mc.player.getLookAngle().scale(mc.player.blockInteractionRange()+1));
        for(var r:recent.values()){
            var p=r.value;if(System.nanoTime()-r.received>1_500_000_000L||!p.dimension().equals(mc.level.dimension().location().toString()))continue;
            var body=mc.level.getEntity(p.entityId());if(body==null)continue;
            var hit=body.getBoundingBox().inflate(1.2).clip(start,end);if(hit.isEmpty()&&!body.getBoundingBox().inflate(1.2).contains(start))continue;
            var target=hit.orElse(start);var obstruction=mc.level.clip(new net.minecraft.world.level.ClipContext(start,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
            if(obstruction.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&obstruction.getLocation().distanceToSqr(start)+.04<target.distanceToSqr(start))continue;
            for(var raw:p.extra().getList("parts",8)){
                java.util.UUID id;try{id=java.util.UUID.fromString(raw.getAsString());}catch(IllegalArgumentException ex){continue;}
                var part=container.getSubLevel(id);if(part==null)continue;var center=part.getPlot().getCenterBlock();
                for(var pos:net.minecraft.core.BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))){
                    if(mc.level.getBlockEntity(pos) instanceof dev.leo.sableplayerragdoll.mob.block.entity.MobRagdollPartBlockEntity be&&p.id().equals(be.sourceEntityId()))return pos.immutable();
                }
            }
        }
        return null;
    }
    public static void accept(CarcassPayload p) {
        var player=Minecraft.getInstance().player;if(player!=null&&p.extra().contains("carry")&&p.extra().getInt("carry")==player.getId())carrying=p.id();
        if(p.phase().equals("FINALIZING")){vanished.add(p.id());recent.remove(p.id());return;}
        recent.put(p.id(),new Recent(p,System.nanoTime()));

    }
    public static boolean vanished(UUID id){return vanished.contains(id);}
    public static float cooking(UUID id) {
        var value=recent.get(id);var level=Minecraft.getInstance().level;
        if(value==null||level==null||!level.dimension().location().toString().equals(value.value.dimension())||value.value.cookTotal()<=0)return 0;
        var p=value.value;double age=Math.clamp((System.nanoTime()-value.received)/1E9,0,.25);
        return (float)Math.clamp((p.precise()+(p.heating()?age*20*p.rate():0))/p.cookTotal(),0,1);
    }
    @SubscribeEvent public static void removed(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e){if(e.getLevel().isClientSide()){recent.remove(e.getEntity().getUUID());motions.remove(e.getEntity().getUUID());}}
    @SubscribeEvent public static void links(net.neoforged.neoforge.client.event.RenderLevelStageEvent event){
        if(event.getStage()!=net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;var mc=Minecraft.getInstance();if(mc.level==null)return;var camera=event.getCamera().getPosition();var buffer=mc.renderBuffers().bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.lines());var pose=event.getPoseStack();
        for(var recentEntry:recent.values()){var p=recentEntry.value;if(!p.dimension().equals(mc.level.dimension().location().toString())||System.nanoTime()-recentEntry.received>1_500_000_000L)continue;net.minecraft.world.phys.Vec3 end=null;var n=p.extra();if(n.contains("holder")){var holder=mc.level.getEntity(n.getInt("holder"));if(holder!=null)end=holder.getEyePosition().add(0,-.6,0);}else if(n.contains("anchorX"))end=new net.minecraft.world.phys.Vec3(n.getDouble("anchorX"),n.getDouble("anchorY"),n.getDouble("anchorZ"));if(end==null)continue;var start=p.position().add(0,.3,0);var dir=end.subtract(start);for(int i=0;i<16;i++){double a=i/16D,b=(i+1)/16D;var from=start.add(dir.scale(a)).add(0,-.2*Math.sin(a*Math.PI),0).subtract(camera);var to=start.add(dir.scale(b)).add(0,-.2*Math.sin(b*Math.PI),0).subtract(camera);var normal=to.subtract(from).normalize();buffer.addVertex(pose.last(),(float)from.x,(float)from.y,(float)from.z).setColor(140,126,105,255).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);buffer.addVertex(pose.last(),(float)to.x,(float)to.y,(float)to.z).setColor(140,126,105,255).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);}}
    }
    @SubscribeEvent public static void register(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.parse("jco_carcasses:world_progress"),(gui,delta)->{
            var mc=Minecraft.getInstance();if(mc.level==null){recent.clear();motions.clear();vanished.clear();return;}
            long now=System.nanoTime();recent.values().removeIf(r->now-r.received>1_500_000_000L||!r.value.dimension().equals(mc.level.dimension().location().toString()));
            motions.keySet().retainAll(recent.keySet());
            if(mc.player==null||mc.options.hideGui||mc.screen!=null){motions.clear();return;}
            for(var r:recent.values()) {
                var p=r.value;
                boolean visible=true;
                if(!p.phase().equals("COOKING")) {
                    boolean target=mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit&&hit.getEntity().getUUID().equals(p.id());
                    if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof dev.leo.sableplayerragdoll.mob.block.entity.MobRagdollPartBlockEntity part){target=p.id().equals(part.sourceEntityId())||part.sourceEntityNetworkId()==p.entityId();if(!target){var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(mc.level);if(container!=null&&container.inBounds(hit.getBlockPos())){var plot=container.getPlot(new net.minecraft.world.level.ChunkPos(hit.getBlockPos()));if(plot!=null&&plot.getSubLevel()!=null){var id=plot.getSubLevel().getUniqueId().toString();target=p.extra().getList("parts",8).stream().anyMatch(v->v.getAsString().equals(id));}}}}
                    var aimedBody=mc.level.getEntity(p.entityId());if(aimedBody!=null){var start=mc.player.getEyePosition();target|=aimedBody.getBoundingBox().inflate(.8).clip(start,start.add(mc.player.getLookAngle().scale(mc.player.blockInteractionRange()+1))).isPresent();}
                    visible=target&&mc.player.position().distanceTo(p.position())<=p.hudRange();
                }
                if(p.phase().equals("FINALIZING")||p.phase().equals("RAW"))continue;
                var carrier=mc.level.getEntity(p.entityId());
                var position=carrier!=null&&carrier.getUUID().equals(p.id())?carrier.getPosition(delta.getGameTimeDeltaPartialTick(false)):p.position();
                var motion=motions.computeIfAbsent(p.id(),id->new Motion());double dt=Math.clamp((now-motion.time)/1E9,0,.1);motion.time=now;
                motion.alpha+=(float)(((visible?1:0)-motion.alpha)*(1-Math.exp(-dt*18)));
                if(motion.actions>=0&&motion.actions!=p.actions())motion.pulse=1;motion.actions=p.actions();motion.pulse=Math.max(0,motion.pulse-(float)dt*3);
                float progress=-1;if(p.phase().equals("COOKING"))progress=cooking(p.id());
                if(progress>=0)motion.progress+=(progress-motion.progress)*(float)(1-Math.exp(-dt*12));
                if(motion.alpha>.02)WorldLabels.draw(gui,position.add(0,1,0),motion.alpha,progress>=0?motion.progress:-1,motion.pulse,p.phase().equals("COOKING")?"":p.phase().equals("RAW")?"LEFT":p.extra().getString("harvestInput"),p.phase().equals("COOKED")?p.display():ItemStack.EMPTY,Math.max(0,p.total()-p.actions()));
            }
        });
    }
}
