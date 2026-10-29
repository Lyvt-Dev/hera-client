package gg.hera.features;

import gg.hera.hud.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import java.util.*;

/** Client-only presentation state. All potentially expensive sampling is opt-in and tick-bound. */
public final class Presentation {
    public static final List<String> IDS=List.of("Hera Companion","Weavy Capes","Chat Heads","Clear Lava","Arrow Trail","Item Model","Block Outlines","Hit Color","Toggle Sprint","Old Animations","No Hurt Cam","Low Fire","No Advancement","Particle Settings","Item Physics","Clear GUI","Way Points","Shulker Preview");
    public static HudConfig hud;
    public static net.minecraft.client.option.KeyBinding sprintKey,waypointKey;
    private static Object world,connection;private static int ticks,combo,lastTarget=-1,lastOwnHurt;private static long lastHit;
    private static boolean sprintLatched,inventorySampled;
    private static final Map<String,Integer> inventory=new LinkedHashMap<>();private static final Deque<Change> changes=new ArrayDeque<>();
    private record Change(String name,int count,long until){}
    private static long previousTime=-1,previousNanos,serverDay=-1;private static double tps=Double.NaN;
    public static Text action=Text.empty(),title=Text.empty(),subtitle=Text.empty();public static int actionTicks,titleTicks;
    public static int fadeIn=10,stay=70,fadeOut=20;
    private Presentation(){}
    public static boolean enabled(String id){var map=Esp.options().presentation;return map!=null&&Boolean.TRUE.equals(map.get(id));}
    public static boolean hudEnabled(String id){return hud!=null&&hud.modules.stream().anyMatch(m->m.id.equals(id)&&m.enabled);}
    public static void toggle(String id){if(IDS.contains(id))Esp.options().presentation.put(id,!enabled(id));}
    public static String worldKey(MinecraftClient c){if(c.world==null)return "";String server=c.getCurrentServerEntry()!=null?c.getCurrentServerEntry().address:c.getServer()!=null?c.getServer().getSaveProperties().getLevelName():"local";return server+"|"+c.world.getRegistryKey().getValue();}
    public static void tick(MinecraftClient c){
        if(connection!=c.getNetworkHandler()){connection=c.getNetworkHandler();previousTime=-1;serverDay=-1;tps=Double.NaN;previousNanos=0;}
        if(world!=c.world){world=c.world;inventory.clear();inventorySampled=false;changes.clear();combo=0;lastTarget=-1;sprintLatched=false;actionTicks=titleTicks=0;}
        if(c.player==null||c.world==null)return;ticks++;if(actionTicks>0)actionTicks--;if(titleTicks>0)titleTicks--;
        if(combo>0&&(System.nanoTime()-lastHit>3_000_000_000L||c.player.hurtTime>lastOwnHurt)){combo=0;lastTarget=-1;}lastOwnHurt=c.player.hurtTime;
        if(!enabled("Toggle Sprint"))sprintLatched=false;
        if(sprintKey!=null)while(sprintKey.wasPressed())if(enabled("Toggle Sprint")&&c.currentScreen==null)sprintLatched=!sprintLatched;
        if(waypointKey!=null)while(waypointKey.wasPressed())if(enabled("Way Points")&&c.currentScreen==null)c.setScreen(new gg.hera.ui.WaypointScreen(null,Esp.config));
        if(hudEnabled("Item Changes")&&ticks%5==0)sampleInventory(c);else if(!hudEnabled("Item Changes")){inventory.clear();inventorySampled=false;changes.clear();}
        changes.removeIf(ch->ch.until()<System.nanoTime());
        if(enabled("Arrow Trail")&&ticks%2==0){int count=0,visited=0;for(var e:c.world.getEntities()){if(++visited>1024)break;if(e instanceof net.minecraft.entity.projectile.PersistentProjectileEntity arrow&&arrow.getVelocity().lengthSquared()>.005&&e.squaredDistanceTo(c.player)<4096){if(++count>48)break;c.world.addParticleClient(net.minecraft.particle.ParticleTypes.END_ROD,e.getX(),e.getY(),e.getZ(),0,0,0);}}}
    }
    public static int shieldColor(){var p=MinecraftClient.getInstance().player;if(p==null)return 0xFFA9ADB5;var stack=p.getOffHandStack().isOf(net.minecraft.item.Items.SHIELD)?p.getOffHandStack():p.getMainHandStack();if(!stack.isOf(net.minecraft.item.Items.SHIELD))return 0xFFA9ADB5;var o=Esp.options();return p.getItemCooldownManager().isCoolingDown(stack)?o.shieldCooldown:p.isBlocking()?o.shieldBlocking:o.shieldReady;}
    public static boolean sprintRequested(){return enabled("Toggle Sprint")&&sprintLatched&&MinecraftClient.getInstance().currentScreen==null;}
    private static void sampleInventory(MinecraftClient c){var next=new LinkedHashMap<String,Integer>();for(int i=0;i<c.player.getInventory().size();i++){var stack=c.player.getInventory().getStack(i);if(!stack.isEmpty())next.merge(Registries.ITEM.getId(stack.getItem()).toString(),stack.getCount(),Integer::sum);}if(inventorySampled){var ids=new LinkedHashSet<>(inventory.keySet());ids.addAll(next.keySet());for(String id:ids){int delta=next.getOrDefault(id,0)-inventory.getOrDefault(id,0);if(delta!=0){var item=Registries.ITEM.get(net.minecraft.util.Identifier.of(id));changes.addFirst(new Change(item.getName().getString(),delta,System.nanoTime()+5_000_000_000L));while(changes.size()>6)changes.removeLast();}}}inventory.clear();inventory.putAll(next);inventorySampled=true;}
    public static void time(long time,long day){serverDay=day;long now=System.nanoTime();if(previousTime>=0&&time>previousTime&&previousNanos>0){double sample=estimateTps(time-previousTime,now-previousNanos);if(Double.isFinite(sample))tps=Double.isNaN(tps)?sample:tps*.7+sample*.3;}previousTime=time;previousNanos=now;}
    public static double estimateTps(long ticks,long nanos){return ticks<=0||nanos<=0?Double.NaN:Math.min(20,ticks*1_000_000_000d/nanos);}
    public static void damage(int target,int attacker){var c=MinecraftClient.getInstance();if(c.player==null||c.world==null||!hudEnabled("Combo Counter"))return;if(target==c.player.getId()){combo=0;lastTarget=-1;}else if(attacker==c.player.getId()&&c.world.getEntityById(target) instanceof net.minecraft.entity.player.PlayerEntity){if(target!=lastTarget||System.nanoTime()-lastHit>3_000_000_000L)combo=0;lastTarget=target;lastHit=System.nanoTime();combo++;}}
    public static List<String> lines(String id,MinecraftClient c){var player=c.player;return switch(id){
        case "Combo Counter"->List.of(combo+" Combo");
        case "Day Counter"->List.of("Tag "+(Math.floorDiv(serverDay>=0?serverDay:c.world.getTimeOfDay(),24000)+1));
        case "TPS"->List.of(Double.isFinite(tps)&&System.nanoTime()-previousNanos<5_000_000_000L?String.format(Locale.ROOT,"≈ %.1f TPS",tps):"TPS —");
        case "Resource Pack Display"->{var packs=c.getResourcePackManager().getEnabledProfiles().stream().map(p->p.getDisplayName().getString()).limit(6).toList();yield packs.isEmpty()?List.of("Keine Ressourcenpakete"):packs;}
        case "Saturation HUD"->List.of("Hunger "+player.getHungerManager().getFoodLevel()+" / 20",String.format(Locale.ROOT,"Sättigung %.1f",player.getHungerManager().getSaturationLevel()));
        case "Helditem"->{var stack=player.getMainHandStack();var list=new ArrayList<String>();list.add(stack.isEmpty()?"Leere Hand":stack.getName().getString());var ench=stack.get(DataComponentTypes.ENCHANTMENTS);if(ench!=null)for(var e:ench.getEnchantmentEntries()){if(list.size()>7)break;list.add(net.minecraft.enchantment.Enchantment.getName(e.getKey(),e.getIntValue()).getString());}yield list;}
        case "Item Counter"->{var list=new ArrayList<String>();for(String item:Esp.options().countedItems){if(list.size()>=8)break;var value=Registries.ITEM.get(net.minecraft.util.Identifier.of(item));int count=0;for(int i=0;i<player.getInventory().size();i++){var s=player.getInventory().getStack(i);if(s.isOf(value))count+=s.getCount();}list.add(value.getName().getString()+" × "+count);}yield list.isEmpty()?List.of("Keine Items ausgewählt"):list;}
        case "Item Changes"->changes.isEmpty()?List.of("Keine Item-Änderungen"):changes.stream().map(ch->(ch.count()>0?"+":"")+ch.count()+" "+ch.name()).toList();
        case "Shield Status"->{boolean has=player.getMainHandStack().isOf(net.minecraft.item.Items.SHIELD)||player.getOffHandStack().isOf(net.minecraft.item.Items.SHIELD);var shield=player.getMainHandStack().isOf(net.minecraft.item.Items.SHIELD)?player.getMainHandStack():player.getOffHandStack();yield List.of(!has?"Kein Schild":player.getItemCooldownManager().isCoolingDown(shield)?"Schild: gesperrt":player.isBlocking()?"Schild: blockiert":"Schild: bereit");}
        case "Jump Reset"->List.of(player.hurtTime>0&&player.isOnGround()?"JETZT SPRINGEN":player.hurtTime>0?"In der Luft":"Warte auf Treffer");
        case "Scoreboard"->{var board=c.world.getScoreboard();var teamOfPlayer=board.getScoreHolderTeam(player.getNameForScoreboard());var colored=teamOfPlayer==null?null:net.minecraft.scoreboard.ScoreboardDisplaySlot.fromFormatting(teamOfPlayer.getColor());var objective=colored==null?null:board.getObjectiveForSlot(colored);if(objective==null)objective=board.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);if(objective==null)yield List.of();var list=new ArrayList<String>();list.add(objective.getDisplayName().getString());board.getScoreboardEntries(objective).stream().filter(e->!e.hidden()).sorted(Comparator.comparingInt(net.minecraft.scoreboard.ScoreboardEntry::value).reversed().thenComparing(net.minecraft.scoreboard.ScoreboardEntry::owner)).limit(15).forEach(e->{var team=board.getScoreHolderTeam(e.owner());list.add(net.minecraft.scoreboard.Team.decorateName(team,e.name()).getString()+(Esp.options().scoreNumbers?"  "+e.value():""));});yield list;}
        case "Action Bar"->actionTicks>0?List.of(action.getString()):List.of();
        case "Titles"->titleTicks>0?List.of(title.getString(),subtitle.getString()):List.of();
        default->List.of(id);
    };}
}
