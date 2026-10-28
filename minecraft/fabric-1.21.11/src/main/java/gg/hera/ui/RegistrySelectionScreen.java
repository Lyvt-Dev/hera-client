package gg.hera.ui;
import gg.hera.features.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import java.util.*;
public final class RegistrySelectionScreen extends Screen {
 private final Screen parent;private final FeatureConfig config;private final boolean particles;private TextFieldWidget search;private int page,rows,x,y,w,h;private String query="",error="";
 public RegistrySelectionScreen(Screen parent,FeatureConfig f,boolean particles){super(HeraTheme.text(particles?"Partikel filtern":"Items zählen · max. 8"));this.parent=parent;config=f;this.particles=particles;}
 private Set<String> selected(){return particles?config.options.hiddenParticles:config.options.countedItems;}
 @Override protected void init(){w=Math.min(420,width-20);h=Math.min(360,height-20);x=(width-w)/2;y=(height-h)/2;rows=Math.max(1,(h-104)/25);search=new TextFieldWidget(textRenderer,x+14,y+38,w-28,20,HeraTheme.text("Suchen"));search.setText(query);search.setChangedListener(v->{query=v;page=0;rebuild();});rebuild();}
 private void rebuild(){boolean focused=search.isFocused();clearChildren();addDrawableChild(search);if(focused)setFocused(search);var registry=particles?Registries.PARTICLE_TYPE:Registries.ITEM;var ids=registry.getIds().stream().map(Object::toString).filter(v->v.contains(query.toLowerCase(Locale.ROOT))).sorted().toList();page=Math.clamp(page,0,Math.max(0,(ids.size()-1)/rows));for(int i=0;i<rows&&page*rows+i<ids.size();i++){String id=ids.get(page*rows+i);addDrawableChild(new HeraButton(x+14,y+67+i*25,w-28,22,(selected().contains(id)?"✓ ":"")+id,selected().contains(id),()->{if(!selected().remove(id)&&selected().size()<(particles?128:8))selected().add(id);rebuild();}));}addDrawableChild(new HeraButton(x+14,y+h-28,28,20,"‹",false,()->{page--;rebuild();}));addDrawableChild(new HeraButton(x+46,y+h-28,28,20,"›",false,()->{page++;rebuild();}));addDrawableChild(new HeraButton(x+w-94,y+h-28,80,20,"Speichern",true,this::close));}
 @Override public void render(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0xCC101216);HeraTheme.panel(c,x,y,w,h);HeraTheme.label(c,error.isEmpty()?particles?"Ausgewählte Partikel ausblenden":"Ausgewählte Items im Inventar zählen":error,x+14,y+15,HeraTheme.TEXT);super.render(c,mx,my,d);}
 @Override public void close(){try{config.save();client.setScreen(parent);}catch(Exception e){error="Speichern fehlgeschlagen";}}
}
