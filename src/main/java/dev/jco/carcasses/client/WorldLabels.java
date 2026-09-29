package dev.jco.carcasses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Small native GuiGraphics labels projected over carcasses and campfires. */
@EventBusSubscriber(modid="jco_carcasses",value=Dist.CLIENT)
public final class WorldLabels {
    private static Matrix4f projection,view;
    private static Vec3 camera;
    @SubscribeEvent public static void capture(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        projection=new Matrix4f(e.getProjectionMatrix());view=new Matrix4f(e.getModelViewMatrix());camera=e.getCamera().getPosition();
    }
    public static void draw(GuiGraphics gui,Vec3 world,float alpha,float progress,float pulse,String input,ItemStack tool,int remaining) {
        draw(gui,world,alpha,progress,input,tool,remaining,false);
    }
    public static void drawFuel(GuiGraphics gui,Vec3 world,float progress,ItemStack fuel) {
        draw(gui,world,1,progress,"",fuel,0,true);
    }
    private static void draw(GuiGraphics gui,Vec3 world,float alpha,float progress,String input,ItemStack tool,int remaining,boolean fuelBar) {
        var mc=Minecraft.getInstance();if(projection==null||view==null||camera==null||mc.level==null||mc.player==null||mc.options.hideGui)return;
        double distance=world.distanceTo(camera);if(distance>20)return;
        var ray=mc.level.clip(new ClipContext(camera,world,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
        if(ray.getType()!=net.minecraft.world.phys.HitResult.Type.MISS && ray.getLocation().distanceTo(camera)+.1<distance)return;
        Vec3 offset=world.subtract(camera);
        var clip=new Vector4f((float)offset.x,(float)offset.y,(float)offset.z,1).mul(view).mul(projection);
        if(clip.w<=0 || clip.z/clip.w>1 || clip.z/clip.w< -1)return;
        float x=(clip.x/clip.w+1)*gui.guiWidth()/2F,y=(1-clip.y/clip.w)*gui.guiHeight()/2F;
        if(x<0||x>gui.guiWidth()||y<0||y>gui.guiHeight())return;
        float scale=(float)Math.clamp(6/Math.max(6,distance),.45,1);
        gui.pose().pushPose();
        try {
            gui.pose().translate(x,y,0);gui.pose().scale(scale,scale,1);
            if(progress>=0){
                var icon=fuelBar?tool:net.minecraft.world.item.Items.CAMPFIRE.getDefaultInstance();
                if(!icon.isEmpty()){gui.setColor(1,1,1,alpha);gui.renderItem(icon,-8,-17);gui.setColor(1,1,1,1);}
                if(fuelBar)gui.fill(-26,1,26,6,((int)(alpha*255)<<24)|0x77828A);
                gui.fill(-25,2,25,5,((int)(alpha*150)<<24)|0x303A43);
                gui.fill(-25,2,-25+Math.round(50*Math.clamp(progress,0,1)),5,((int)(alpha*255)<<24)|0xD2B477);
            }else if(!tool.isEmpty()){
                gui.setColor(1,1,1,alpha);int w=InputIcons.draw(gui,input,-25,-6);gui.renderItem(tool,-25+w,-8);gui.setColor(1,1,1,1);
                gui.drawString(mc.font,"\u00d7"+remaining,-7+w,-4,((int)(alpha*255)<<24)|0xFFFFFF);
            }

        } finally {gui.pose().popPose();}
    }
}
