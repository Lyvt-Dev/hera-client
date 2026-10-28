package gg.hera.cosmetics;
import gg.hera.features.Esp;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
/** Eight articulated strips, shared cached mesh, frame-time continuous movement. */
public final class WeavyCapeModel extends EntityModel<PlayerEntityRenderState> {
 private final ModelPart[] strips=new ModelPart[8];
 public WeavyCapeModel(){super(create());ModelPart part=getRootPart();for(int i=0;i<8;i++){part=part.getChild("strip"+i);strips[i]=part;}}
 private static ModelPart create(){var data=new ModelData();var parent=data.getRoot();for(int i=0;i<8;i++)parent=parent.addChild("strip"+i,ModelPartBuilder.create().uv(0,i*2).cuboid(-5,0,-1,10,2,1),ModelTransform.origin(0,i==0?0:2,i==0?2:0));return TexturedModelData.of(data,64,32).createModel();}
 @Override public void setAngles(PlayerEntityRenderState state){resetTransforms();float strength=Esp.options().capeStrength;strips[0].originY=state.isInSneakingPose?3:0;strips[0].originZ=state.isInSneakingPose?3:2;strips[0].yaw=(float)Math.PI;strips[0].pitch=(float)Math.toRadians(6+Math.clamp(state.field_53536+state.field_53537*.5f,0,70));for(int i=1;i<8;i++){float motion=(float)Math.sin(state.age*.13-i*.55+state.id*.4);strips[i].pitch=(.025f+motion*.045f+Math.clamp(state.field_53537,0,60)*.0008f)*strength;strips[i].roll=(float)Math.sin(state.age*.09-i*.4+state.id)*.018f*strength;}}
}
