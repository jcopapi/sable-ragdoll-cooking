package dev.jco.carcasses.integration;
import dev.jco.carcasses.*;import dev.jco.carcasses.integration.mixin.SableAccess;import dev.leo.sableplayerragdoll.mob.*;import dev.leo.sableplayerragdoll.mob.block.entity.MobRagdollPartBlockEntity;import net.minecraft.core.BlockPos;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.entity.LivingEntity;import java.util.UUID;
public final class BodyParts {
 public static LivingEntity resolve(ServerLevel level,BlockPos pos,MobRagdollPartBlockEntity part){
  if(part!=null&&part.sourceEntityId()!=null&&level.getEntity(part.sourceEntityId()) instanceof LivingEntity living&&CarcassRuntime.isCarrier(living))return living;
  var known=SableAccess.source(level,pos);if(known!=null&&CarcassRuntime.isCarrier(known))return known;
  var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);if(container==null||!container.inBounds(pos))return null;var plot=container.getPlot(new net.minecraft.world.level.ChunkPos(pos));if(plot==null)return null;var sub=plot.getSubLevel();if(sub==null)return null;
  for(var id:CarcassData.get(level).entries.keySet()){var saved=MobRagdollSavedData.get(level).getEntry(id);if(saved!=null&&saved.partIds().containsValue(sub.getUniqueId())&&level.getEntity(id) instanceof LivingEntity living)return living;}
  return null;
 }
 public static boolean grabbed(UUID id){return SableAccess.grabs().getOrDefault(id,0)>0;}
 public static net.minecraft.world.phys.Vec3 worldPosition(ServerLevel level,LivingEntity carrier){
  var root=root(level,carrier.getUUID());
  if(root==null)return carrier.position();
  var position=root.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atCenterOf(root.getPlot().getCenterBlock()));
  return Double.isFinite(position.x)&&Double.isFinite(position.y)&&Double.isFinite(position.z)?position:carrier.position();
 }
 public static dev.ryanhcode.sable.sublevel.ServerSubLevel root(ServerLevel level,UUID id){var saved=MobRagdollSavedData.get(level).getEntry(id);if(saved==null)return null;var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);if(container==null)return null;
  var keys=new java.util.ArrayList<>(saved.partIds().keySet());keys.sort(java.util.Comparator.comparingInt(k->saved.partInfos().containsKey(k)&&saved.partInfos().get(k).role()==dev.leo.sableplayerragdoll.mob.block.MobPartRole.TORSO?0:1));for(var key:keys)if(container.getSubLevel(saved.partIds().get(key)) instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel sub)return sub;return null;
 }
}
