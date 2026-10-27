package gg.hera.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.block.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;


/** Bounded two-crystal attempt, gated by server observations and conservative self damage. */
public final class DTap {
    public static DTap instance;
    private enum Phase {IDLE,HIT,BASE,CRYSTAL_ONE,POP,CRYSTAL_TWO,DONE}
    private record Plan(BlockPos base,BlockHitResult support,double score){}
    private final FeatureConfig config;private Phase phase=Phase.IDLE;
    private PlayerEntity target;private Object world;private Plan plan;private int tick,deadline,nextAction,cooldown,oldSlot,ownedSlot,crystalId=-1;
    private boolean baseConfirmed,hitConfirmed;private final DTapGate gate=new DTapGate();
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final Direction[] SIDES=Direction.values();
    private BlockPos searchAnchor;private int searchCursor;private Plan searchBest;
    public DTap(FeatureConfig config){this.config=config;instance=this;}
    public boolean busy(){return phase!=Phase.IDLE;}
    public void hit(PlayerEntity other){var c=MinecraftClient.getInstance();
        if(!config.options.dTap || busy() || tick<cooldown || c.player==null || c.currentScreen!=null || c.player.isUsingItem() || c.player.currentScreenHandler!=c.player.playerScreenHandler || CameraController.detached() || other==c.player || !validTarget(c,other) || other.hurtTime>0 || c.player.getAttackCooldownProgress(0)<.9f)return;
        if(slot(c,Items.END_CRYSTAL)<0 || count(c,Items.END_CRYSTAL)<2)return;
        target=other;world=c.world;oldSlot=c.player.getInventory().getSelectedSlot();ownedSlot=oldSlot;phase=Phase.HIT;deadline=tick+12;gate.reset();hitConfirmed=false;baseConfirmed=false;crystalId=-1;searchAnchor=null;searchCursor=0;searchBest=null;
    }
    public void blockUpdate(BlockPos pos,BlockState state){if(phase==Phase.BASE && plan!=null && plan.base.equals(pos) && state.isOf(Blocks.OBSIDIAN))baseConfirmed=true;}
    public void damage(int entity,int attacker){var c=MinecraftClient.getInstance();if(phase==Phase.HIT&&target!=null&&target.getId()==entity&&c.player!=null&&c.player.getId()==attacker)hitConfirmed=true;}
    public void entityStatus(Entity entity,byte status){if(phase==Phase.POP && entity!=null)gate.status(entity.getId(),status);}
    public void removed(int id){if(phase==Phase.POP)gate.removed(id);if(id==crystalId && phase==Phase.DONE)finish(MinecraftClient.getInstance(),"Sequenz bestätigt");}
    public void tick(MinecraftClient c){tick++;
        if(!busy())return;
        if(!config.options.dTap || c.player==null || c.interactionManager==null || c.getNetworkHandler()==null || c.world!=world || c.currentScreen!=null || c.player.isUsingItem() || c.player.currentScreenHandler!=c.player.playerScreenHandler || CameraController.detached() || !c.player.isAlive() || c.player.isSpectator() || !validTarget(c,target)){finish(c,"Abgebrochen");return;}
        if(c.player.getInventory().getSelectedSlot()!=ownedSlot){finish(c,"Slotwechsel – abgebrochen");return;}
        if(tick>deadline){finish(c,"Keine Server-Bestätigung");return;}
        if(tick<nextAction)return;
        switch(phase){
            case HIT->{if(!hitConfirmed)return;if(!target.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)&&!target.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)){finish(c,"Ziel hält kein Totem");return;}if(plan==null){plan=findPlan(c);if(plan==null){if(searchCursor>=50)finish(c,"Kein geeigneter Platz");return;}deadline=tick+40;}
                if(c.world.getBlockState(plan.base).isOf(Blocks.OBSIDIAN)||c.world.getBlockState(plan.base).isOf(Blocks.BEDROCK)){baseConfirmed=true;placeCrystal(c,false);}
                else {int obsidian=slot(c,Items.OBSIDIAN);if(obsidian<0){finish(c,"Obsidian fehlt in der Hotbar");return;}var support=placementHit(c,plan.support);if(support==null)return;if(!safe(c,plan.base)){finish(c,"Schadensprüfung");return;}if(!prepareSlot(c,obsidian))return;c.interactionManager.interactBlock(c.player,Hand.MAIN_HAND,support);c.player.swingHand(Hand.MAIN_HAND);phase=Phase.BASE;deadline=tick+40;nextAction=tick+delay();}}
            case BASE->{if(baseConfirmed)placeCrystal(c,false);}
            case CRYSTAL_ONE,CRYSTAL_TWO->{var crystal=crystal(c);if(crystal==null)return;
                if(phase==Phase.CRYSTAL_TWO && target.hurtTime>0)return;
                if(config.options.dTapCautious && (!(c.crosshairTarget instanceof EntityHitResult hit) || hit.getEntity()!=crystal || c.player.getAttackCooldownProgress(0)<1f))return;
                if(!c.player.canAttackEntityIn(crystal.getBoundingBox(),0) || !c.player.canSee(crystal) || !safe(c,plan.base)){finish(c,"Reichweite oder Schadensprüfung");return;}
                crystalId=crystal.getId();boolean second=phase==Phase.CRYSTAL_TWO;if(!second)gate.begin(target.getId(),crystalId);c.interactionManager.attackEntity(c.player,crystal);c.player.swingHand(Hand.MAIN_HAND);phase=second?Phase.DONE:Phase.POP;deadline=tick+40;nextAction=tick+delay();}
            case POP->{if(gate.ready())placeCrystal(c,true);}
            default->{}
        }
    }
    private boolean validTarget(MinecraftClient c,PlayerEntity p){return p!=null && p.isAlive() && !p.isRemoved() && !p.isSpectator() && !p.isCreative() && p.getEntityWorld()==c.world && !c.player.isTeammate(p) && p.squaredDistanceTo(c.player)<=36;}
    private int slot(MinecraftClient c,Item item){for(int i=0;i<9;i++)if(c.player.getInventory().getStack(i).isOf(item))return i;return -1;}
    private int count(MinecraftClient c,Item item){int n=0;for(int i=0;i<9;i++){var stack=c.player.getInventory().getStack(i);if(stack.isOf(item))n+=stack.getCount();}return n;}
    private void select(MinecraftClient c,int slot){ownedSlot=slot;c.player.getInventory().setSelectedSlot(slot);}
    private int delay(){return DTapPolicy.actionDelay(config.options.dTapDelay,config.options.dTapCautious);}
    private boolean prepareSlot(MinecraftClient c,int slot){
        if(c.player.getInventory().getSelectedSlot()==slot)return true;
        select(c,slot);
        if(config.options.dTapCautious){nextAction=tick+delay();return false;}
        return true;
    }
    private BlockHitResult placementHit(MinecraftClient c,BlockHitResult planned){
        if(!config.options.dTapCautious)return planned;
        return c.crosshairTarget instanceof BlockHitResult hit && hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(planned.getBlockPos()) && hit.getSide()==planned.getSide()?hit:null;
    }
    private void placeCrystal(MinecraftClient c,boolean second){
        int slot=slot(c,Items.END_CRYSTAL);if(slot<0){finish(c,"Kristalle fehlen");return;}
        var hit=new BlockHitResult(plan.base.toCenterPos().add(0,.5,0),Direction.UP,plan.base,false);var aimed=placementHit(c,hit);if(aimed==null)return;
        if(!c.player.canInteractWithBlockAt(plan.base,0)||!space(c,plan.base)||!visible(c,hit)||!safe(c,plan.base)){finish(c,"Platz nicht mehr geeignet");return;}
        if(!prepareSlot(c,slot))return;c.interactionManager.interactBlock(c.player,Hand.MAIN_HAND,aimed);c.player.swingHand(Hand.MAIN_HAND);phase=second?Phase.CRYSTAL_TWO:Phase.CRYSTAL_ONE;deadline=tick+40;nextAction=tick+delay();
    }
    private EndCrystalEntity crystal(MinecraftClient c){return c.world.getEntitiesByClass(EndCrystalEntity.class,new Box(plan.base.up()).expand(.4,1,.4),e->!e.isRemoved() && e.getId()!=crystalId).stream().findFirst().orElse(null);}
    private boolean space(MinecraftClient c,BlockPos base){return c.world.getBlockState(base.up()).isAir() && c.world.getBlockState(base.up(2)).isAir() && c.world.getOtherEntities(null,new Box(base.up()).stretch(0,1,0),e->!e.isRemoved()).isEmpty();}
    private boolean visible(MinecraftClient c,BlockHitResult hit){var ray=c.world.raycast(new RaycastContext(c.player.getEyePos(),hit.getPos(),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,c.player));return ray.getType()==HitResult.Type.BLOCK && ray.getBlockPos().equals(hit.getBlockPos());}
    private Plan findPlan(MinecraftClient c){
        if(searchAnchor==null){searchAnchor=target.getBlockPos();deadline=tick+40;}
        if(!target.getBlockPos().equals(searchAnchor)){searchCursor=50;searchBest=null;return null;}
        var budget=new WorkBudget(4,1_000_000);boolean obsidian=slot(c,Items.OBSIDIAN)>=0;
        while(searchCursor<50&&budget.step()){
            int index=searchCursor++,dx=index/10-2,dz=(index/2)%5-2,dy=index%2-1;
            if(Math.abs(dx)+Math.abs(dz)>3)continue;var base=searchAnchor.add(dx,dy,dz);var state=c.world.getBlockState(base);boolean existing=state.isOf(Blocks.OBSIDIAN)||state.isOf(Blocks.BEDROCK);
            if(!existing && (!state.isAir() || !obsidian || !c.world.getOtherEntities(null,new Box(base),e->!e.isRemoved()).isEmpty()))continue;
            if(!space(c,base) || !c.player.canInteractWithBlockAt(base,0) || !c.player.canAttackEntityIn(new Box(base.up()).expand(.5,0,.5).stretch(0,1,0),0))continue;
            double targetDamage=damage(c,target,base,false),selfDamage=damage(c,c.player,base,true);var o=config.options;
            if(!DTapPolicy.suitable(targetDamage,selfDamage,c.player.getHealth()+c.player.getAbsorptionAmount(),DTapPolicy.requiredDamage(target.getHealth(),target.getAbsorptionAmount(),o.dTapTarget),o.dTapSelf,o.dTapReserve))continue;
            BlockHitResult support=null;
            if(!existing)for(Direction side:SIDES){var neighbor=base.offset(side.getOpposite());if(!c.world.getBlockState(neighbor).isSideSolidFullSquare(c.world,neighbor,side))continue;var point=neighbor.toCenterPos().add(Vec3d.of(side.getVector()).multiply(.5));var hit=new BlockHitResult(point,side,neighbor,false);if(visible(c,hit)){support=hit;break;}}
            if(!existing && support==null)continue;
            double score=targetDamage-selfDamage*.6;if(searchBest==null || score>searchBest.score)searchBest=new Plan(base,support,score);
        }return searchBest;
    }
    private boolean safe(MinecraftClient c,BlockPos base){var o=config.options;double required=DTapPolicy.requiredDamage(target.getHealth(),target.getAbsorptionAmount(),o.dTapTarget);return DTapPolicy.suitable(damage(c,target,base,false),damage(c,c.player,base,true),c.player.getHealth()+c.player.getAbsorptionAmount(),required,o.dTapSelf,o.dTapReserve);}
    private double damage(MinecraftClient c,PlayerEntity entity,BlockPos base,boolean self){
        var center=base.toCenterPos().add(0,.5001,0);double exposure=1;
        if(!self){int clear=0;var box=entity.getBoundingBox();for(int i=0;i<8;i++){var p=new Vec3d((i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY+.1:box.maxY-.1,(i&4)==0?box.minZ:box.maxZ);if(c.world.raycast(new RaycastContext(p,center,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,entity)).getType()==HitResult.Type.MISS)clear++;}exposure=clear/8d;}
        float raw=(float)DTapPolicy.rawDamage(entity.getEntityPos().distanceTo(center),exposure);raw=switch(c.world.getDifficulty()){case PEACEFUL->0;case EASY->Math.min(raw/2+1,raw);case HARD->raw*1.5f;default->raw;};
        float armor=DamageUtil.getDamageLeft(entity,raw,c.world.getDamageSources().explosion(null,null),entity.getArmor(),(float)entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
        int protection=0;for(var slot:ARMOR){var enchantments=entity.getEquippedStack(slot).get(DataComponentTypes.ENCHANTMENTS);if(enchantments!=null)for(var e:enchantments.getEnchantmentEntries()){if(e.getKey().matchesKey(Enchantments.PROTECTION))protection+=e.getIntValue();if(e.getKey().matchesKey(Enchantments.BLAST_PROTECTION))protection+=e.getIntValue()*2;}}
        double result=armor*(1-Math.min(20,protection)/25d);var resistance=entity.getStatusEffect(StatusEffects.RESISTANCE);if(resistance!=null)result*=Math.max(0,1-.2*(resistance.getAmplifier()+1));return result;
    }
    private void finish(MinecraftClient c,String reason){
        if(c.player!=null && c.world==world && c.getNetworkHandler()!=null && c.player.getInventory().getSelectedSlot()==ownedSlot && oldSlot>=0&&oldSlot<9){c.player.getInventory().setSelectedSlot(oldSlot);}
        if(c.player!=null && config.options.dTapFeedback)c.player.sendMessage(net.minecraft.text.Text.literal("D-Tap · "+reason),true);
        phase=Phase.IDLE;cooldown=tick+20;target=null;plan=null;crystalId=-1;
    }
}
