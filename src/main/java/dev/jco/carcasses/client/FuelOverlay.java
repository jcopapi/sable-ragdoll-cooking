package dev.jco.carcasses.client;
import dev.jco.carcasses.FuelPayload;import net.minecraft.client.Minecraft;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.phys.*;import net.neoforged.api.distmarker.Dist;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="jco_carcasses",value=Dist.CLIENT)
public final class FuelOverlay {
 private static FuelPayload recent;private static long time;public static void accept(FuelPayload p){recent=p;time=System.nanoTime();}
 @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event){event.registerAboveAll(ResourceLocation.parse("jco_carcasses:fuel"),(gui,delta)->{var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.screen!=null||recent==null||System.nanoTime()-time>750_000_000L||!recent.dimension().equals(mc.level.dimension().location().toString())||!(mc.hitResult instanceof BlockHitResult b)||b.getType()!=HitResult.Type.BLOCK||!b.getBlockPos().equals(recent.pos()))return;double left=Math.max(0,recent.remaining()-(recent.lit()?(System.nanoTime()-time)/1E9*20:0));WorldLabels.drawFuel(gui,Vec3.atCenterOf(recent.pos()).add(0,.6,0),(float)(left/Math.max(1,recent.total())),recent.fuel());});}
}
