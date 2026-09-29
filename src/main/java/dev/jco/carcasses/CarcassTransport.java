package dev.jco.carcasses;
import java.util.*;import dev.jco.carcasses.integration.BodyParts;import dev.ryanhcode.sable.api.physics.constraint.*;import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.nbt.CompoundTag;import net.minecraft.server.level.*;import net.minecraft.world.*;import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.item.*;import net.minecraft.world.phys.Vec3;import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;import org.joml.Vector3d;import org.joml.Quaterniond;
public final class CarcassTransport {
 private record Selection(UUID body,long expires){}private static final Map<UUID,Selection> selection=new HashMap<>();
 private record Pin(GenericConstraintHandle handle,Vector3d position){}private static final Map<UUID,Pin> pins=new HashMap<>();
 public static boolean interact(ServerPlayer p,LivingEntity carrier,InteractionHand hand,CarcassData.Entry e,CarcassDefinition d){
  var item=p.getItemInHand(hand);var t=e.transport;var level=p.serverLevel();
  if(item.isEmpty()&&p.isShiftKeyDown()&&(t.contains("mount")||t.hasUUID("holder")||t.hasUUID("carry"))){release(level,carrier,e);p.swing(hand,true);return true;}
  if(d.mountItem(item)){if(t.hasUUID("carry")||t.hasUUID("holder")||BodyParts.grabbed(e.carrier)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Release the leash/grab before mounting."),true);return true;}selection.put(p.getUUID(),new Selection(e.carrier,level.getGameTime()+200));p.displayClientMessage(net.minecraft.network.chat.Component.literal("Now use this attachment on a heat source."),true);p.swing(hand,true);return true;}
  if(item.is(Items.LEAD)||item.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse("jco:leashes")))){if(t.contains("mount")||t.hasUUID("carry")||t.hasUUID("holder")||BodyParts.grabbed(e.carrier))return true;t.putUUID("holder",p.getUUID());t.put("leashItem",item.copyWithCount(1).save(level.registryAccess()));item.shrink(1);e.stillTicks=0;CarcassData.get(level).setDirty();p.swing(hand,true);return true;}
  // Empty-hand dragging is owned by Sable's native grab system.
  return false;
 }
 public static void attachBlock(PlayerInteractEvent.RightClickBlock event){
  if(!(event.getEntity() instanceof ServerPlayer p)||event.getHand()!=InteractionHand.MAIN_HAND)return;var chosen=selection.get(p.getUUID());if(chosen==null)return;var level=p.serverLevel();if(chosen.expires<level.getGameTime()){selection.remove(p.getUUID());return;}
  var e=CarcassData.get(level).entries.get(chosen.body);if(e==null||!(level.getEntity(chosen.body) instanceof LivingEntity body))return;var d=Carcasses.definitions().get(e.definition);var item=p.getMainHandItem();if(d==null||!d.mountItem(item)||!d.mountSource(level.getBlockState(event.getPos())))return;
  event.setCanceled(true);event.setCancellationResult(InteractionResult.CONSUME);
  if(e.finishing||e.committing||e.transport.hasUUID("holder")||e.transport.contains("mount")||BodyParts.grabbed(e.carrier)||!p.mayBuild()||p.isSpectator()||!level.mayInteract(p,event.getPos())||p.distanceToSqr(body)>64||p.distanceToSqr(Vec3.atCenterOf(event.getPos()))>Math.pow(p.blockInteractionRange()+1,2)||body.position().distanceTo(Vec3.atCenterOf(event.getPos()))>8)return;
  var mount=new CompoundTag();mount.putLong("pos",event.getPos().asLong());mount.putString("block",BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock()).toString());mount.putDouble("height",d.mountHeight());
  if(!ensurePin(level,body,e,mount)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Body physics is not ready; try again."),true);return;}
  mount.put("item",item.copyWithCount(1).save(level.registryAccess()));e.transport.put("mount",mount);item.shrink(1);selection.remove(p.getUUID());CarcassData.get(level).setDirty();p.swing(event.getHand(),true);level.playSound(null,event.getPos(),net.minecraft.sounds.SoundEvents.CHAIN_PLACE,net.minecraft.sounds.SoundSource.PLAYERS,.6F,1);
 }
 private static final Map<UUID,PhysicsConstraintHandle> tethers=new HashMap<>();
 private static boolean ensureTether(ServerLevel level,CarcassData.Entry e,Vec3 target){
  var previous=tethers.remove(e.carrier);if(previous!=null&&previous.isValid())previous.remove();
  var root=BodyParts.root(level,e.carrier);var system=SubLevelPhysicsSystem.get(level);if(root==null||system==null)return false;
  var local=Vec3.atCenterOf(root.getPlot().getCenterBlock());var handle=system.getPipeline().addConstraint(null,root,new FreeConstraintConfiguration(new Vector3d(target.x,target.y,target.z),new Vector3d(local.x,local.y,local.z),new Quaterniond()));if(handle==null)return false;
  for(var axis:ConstraintJointAxis.LINEAR)handle.setMotor(axis,0,500,50,true,500);system.getPipeline().wakeUp(root);tethers.put(e.carrier,handle);return true;
 }
 private static boolean ensurePin(ServerLevel level,LivingEntity body,CarcassData.Entry e,CompoundTag mount){
  var pin=pins.get(e.carrier);if(pin!=null&&pin.handle.isValid())return true;
  var root=BodyParts.root(level,e.carrier);var system=SubLevelPhysicsSystem.get(level);if(root==null||system==null)return false;
  try{var center=root.getPlot().getCenterBlock();var local=new Vector3d(center.getX()+.5,center.getY()+.5,center.getZ()+.5);var current=new Vector3d(body.getX(),body.getY(),body.getZ());
   var config=new GenericConstraintConfiguration(local,current,new Quaterniond(),new Quaterniond(),Set.of(ConstraintJointAxis.LINEAR_X,ConstraintJointAxis.LINEAR_Y,ConstraintJointAxis.LINEAR_Z));
   var handle=system.getPipeline().addConstraint(root,null,config);for(var axis:Set.of(ConstraintJointAxis.ANGULAR_X,ConstraintJointAxis.ANGULAR_Z))handle.setMotor(axis,0,0,2,false,15);pins.put(e.carrier,new Pin(handle,current));return true;
  }catch(RuntimeException ex){com.mojang.logging.LogUtils.getLogger().warn("Mount constraint unavailable",ex);return false;}
 }
 public static boolean tick(ServerLevel level,LivingEntity body,CarcassData.Entry e,CarcassDefinition d){
  var t=e.transport;if(t.hasUUID("holder")&&BodyParts.grabbed(e.carrier)){release(level,body,e);return true;}if(t.contains("mount")){var mount=t.getCompound("mount");var pos=BlockPos.of(mount.getLong("pos"));if(!level.hasChunkAt(pos))return true;
   if(BodyParts.grabbed(e.carrier)||!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals(mount.getString("block"))||!d.mountSource(level.getBlockState(pos))){release(level,body,e);return true;}
   if(ensurePin(level,body,e,mount)){var pin=pins.get(e.carrier);var target=new Vector3d(pos.getX()+.5,pos.getY()+mount.getDouble("height"),pos.getZ()+.5);var movement=target.sub(pin.position,new Vector3d());if(movement.length()>.1)movement.normalize(.1);pin.position.add(movement);pin.handle.setFrame2(pin.position,new Quaterniond());}return true;
  }
  if(t.hasUUID("holder")){var holder=level.getEntity(t.getUUID("holder"));if(holder==null||!holder.isAlive()||holder.level()!=level){release(level,body,e);return true;}
   var root=BodyParts.root(level,e.carrier);if(root==null)return true;
   var center=root.logicalPose().transformPosition(Vec3.atCenterOf(root.getPlot().getCenterBlock()));double distance=center.distanceTo(holder.position());
   if(distance>10){release(level,body,e);return true;}
   if(distance>2){var goal=holder.position().add(0,.7,0);var direction=center.subtract(goal).normalize().scale(1.8);ensureTether(level,e,goal.add(direction));}else removePin(e.carrier);
   return true; // Leashed transport never expires, even when a player briefly stops.
  }
  if(t.hasUUID("carry")){var holder=level.getPlayerByUUID(t.getUUID("carry"));if(holder==null||!holder.isAlive()||holder.isSpectator()||!holder.getMainHandItem().isEmpty()||holder.distanceToSqr(body)>64){release(level,body,e);return true;}var target=holder.getEyePosition().add(holder.getLookAngle().scale(1.5)).add(0,-.5,0);ensureTether(level,e,target);return true;}
  return BodyParts.grabbed(e.carrier);
 }
 public static double mountedHeat(ServerLevel level,CarcassData.Entry e,CarcassDefinition d){if(!e.transport.contains("mount"))return -1;var pos=BlockPos.of(e.transport.getCompound("mount").getLong("pos"));return level.hasChunkAt(pos)&&d.heats(level.getBlockState(pos))?d.heatRate(d.mountHeight()-.5):0;}
 public static void release(ServerLevel level,LivingEntity body,CarcassData.Entry e){
  removePin(e.carrier);var t=e.transport;var items=new ArrayList<ItemStack>();if(t.contains("mount"))items.add(ItemStack.parseOptional(level.registryAccess(),t.getCompound("mount").getCompound("item")));if(t.contains("leashItem"))items.add(ItemStack.parseOptional(level.registryAccess(),t.getCompound("leashItem")));
  t.remove("carry");t.remove("mount");t.remove("holder");t.remove("leashItem");e.outputs.addAll(items.stream().filter(i->!i.isEmpty()).toList());e.outputPosition=CarcassRuntime.safeOutput(level,body.position(),null);e.stillTicks=0;CarcassData.get(level).setDirty();
 }
 public static void clear(){for(var id:new ArrayList<>(tethers.keySet()))removePin(id);for(var id:new ArrayList<>(pins.keySet()))removePin(id);selection.clear();}
 public static void removePin(UUID id){var tether=tethers.remove(id);if(tether!=null&&tether.isValid())tether.remove();var pin=pins.remove(id);if(pin!=null&&pin.handle.isValid())pin.handle.remove();}
 public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event){if(CarcassRuntime.isCarrier(event.getEntity())){removePin(event.getEntity().getUUID());if(event.getLevel() instanceof ServerLevel level){var e=CarcassData.get(level).entries.get(event.getEntity().getUUID());if(e!=null&&e.passiveDeadline>=0){e.passiveRemaining=Math.max(0,e.passiveDeadline-level.getGameTime());e.passiveDeadline=-1;e.stillTicks=0;CarcassData.get(level).setDirty();}}}}
 public static CompoundTag snapshot(ServerLevel level,CarcassData.Entry e,CarcassDefinition d){var n=new CompoundTag();n.putString("harvestInput",d.harvestInput().name());n.putLong("passive",e.passiveDeadline<0?-1:Math.max(0,e.passiveDeadline-level.getGameTime()));n.putInt("passiveTotal",d.passiveTicks());n.putBoolean("settled",e.passiveDeadline>=0);var rows=new net.minecraft.nbt.ListTag();for(int i=0;i<d.accelerators().size();i++){var a=d.accelerators().get(i);if(i<e.acceleratorCounts.length&&e.acceleratorCounts[i]<0)continue;var row=new CompoundTag();row.putString("selector",a.item.id());row.putInt("have",i<e.acceleratorCounts.length?e.acceleratorCounts[i]:0);row.putInt("need",a.actions);row.putInt("reduction",a.reduction);rows.add(row);}n.put("accelerators",rows);var parts=new net.minecraft.nbt.ListTag();var saved=dev.leo.sableplayerragdoll.mob.MobRagdollSavedData.get(level).getEntry(e.carrier);if(saved!=null)for(var id:saved.partIds().values())parts.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));n.put("parts",parts);
  if(e.transport.hasUUID("carry")){var owner=level.getEntity(e.transport.getUUID("carry"));if(owner!=null)n.putInt("carry",owner.getId());}
  if(e.transport.hasUUID("holder")){var holder=level.getEntity(e.transport.getUUID("holder"));if(holder!=null)n.putInt("holder",holder.getId());}
  if(e.transport.contains("mount")){var mount=e.transport.getCompound("mount");var pos=BlockPos.of(mount.getLong("pos"));n.putDouble("anchorX",pos.getX()+.5);n.putDouble("anchorY",pos.getY()+mount.getDouble("height")+1);n.putDouble("anchorZ",pos.getZ()+.5);}
  return n;
 }
}
