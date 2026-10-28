package gg.hera.ui;
import gg.hera.features.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
public final class WaypointScreen extends Screen {
 private final Screen parent;private final FeatureConfig config;private TextFieldWidget name;private String error="";private int page,x,y,w,h,rows;
 public WaypointScreen(Screen parent,FeatureConfig config){super(HeraTheme.text("Wegpunkte"));this.parent=parent;this.config=config;}
 @Override protected void init(){w=Math.min(440,width-20);h=Math.min(340,height-20);x=(width-w)/2;y=(height-h)/2;rows=Math.max(1,(h-130)/29);name=new TextFieldWidget(textRenderer,x+14,y+40,w-28,20,HeraTheme.text("Wegpunktname"));name.setMaxLength(32);name.setPlaceholder(HeraTheme.text("Name für aktuelle Position"));rebuild();}
 private void rebuild(){clearChildren();addDrawableChild(name);addDrawableChild(new HeraButton(x+14,y+66,w-28,24,"Aktuelle Position speichern",true,()->{if(client.player==null||client.world==null){error="Zuerst eine Welt betreten.";return;}if(name.getText().isBlank()){error="Bitte einen Namen eingeben.";return;}if(config.options.waypoints.size()>=32){error="Maximal 32 Wegpunkte.";return;}var p=new ModuleOptions.Waypoint();p.name=name.getText().strip();p.world=Presentation.worldKey(client);p.x=client.player.getX();p.y=client.player.getY();p.z=client.player.getZ();config.options.waypoints.add(p);error="";rebuild();}));var points=config.options.waypoints;page=Math.clamp(page,0,Math.max(0,(points.size()-1)/rows));for(int i=0;i<rows&&page*rows+i<points.size();i++){var p=points.get(page*rows+i);addDrawableChild(new HeraButton(x+14,y+99+i*29,w-28,25,p.name+" · "+Math.round(p.x)+" / "+Math.round(p.y)+" / "+Math.round(p.z)+" · Entfernen",false,()->{points.remove(p);rebuild();}));}addDrawableChild(new HeraButton(x+14,y+h-28,28,20,"‹",false,()->{page--;rebuild();}));addDrawableChild(new HeraButton(x+46,y+h-28,28,20,"›",false,()->{page++;rebuild();}));addDrawableChild(new HeraButton(x+w-94,y+h-28,80,20,"Speichern",true,this::close));}
 @Override public void render(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0xCC101216);HeraTheme.panel(c,x,y,w,h);HeraTheme.label(c,error.isEmpty()?"Wegpunkte · getrennt nach Server und Dimension":error,x+14,y+15,HeraTheme.TEXT);super.render(c,mx,my,d);}
 @Override public void close(){try{config.save();client.setScreen(parent);}catch(Exception e){error="Speichern fehlgeschlagen";}}
}
