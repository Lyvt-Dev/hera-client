package gg.hera.ui;

import gg.hera.features.FeatureConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import java.util.*;
import java.io.IOException;

public final class ItemSelectionScreen extends Screen {
    private final Screen parent;private final FeatureConfig config;
    private String search="",error="";private boolean selectedOnly;private int page,x,y,w,h,rows;
    private TextFieldWidget field;
    private List<Item> visible=List.of();
    public ItemSelectionScreen(Screen parent,FeatureConfig config){super(HeraTheme.text("Item-Markierungen"));this.parent=parent;this.config=config;}
    @Override protected void init(){
        w=Math.min(440,width-20);h=Math.min(340,height-20);x=(width-w)/2;y=(height-h)/2;rows=Math.max(1,(h-174)/29);
        field=new TextFieldWidget(textRenderer,x+14,y+60,w-28,20,HeraTheme.text("Items suchen"));
        field.setMaxLength(100);field.setText(search);field.setPlaceholder(HeraTheme.text("Item-Name oder minecraft:ID …"));
        field.addFormatter((v,o)->HeraTheme.text(v).asOrderedText());
        field.setChangedListener(v->{search=v;page=0;rebuild();});rebuild();
    }
    private void rebuild(){
        clearChildren();addDrawableChild(field);
        String query=search.toLowerCase(Locale.ROOT).trim();
        visible=Registries.ITEM.stream().filter(i->i!=Items.AIR).filter(i->!selectedOnly||config.markedItems.contains(Registries.ITEM.getId(i).toString()))
            .filter(i->i.getName().getString().toLowerCase(Locale.ROOT).contains(query)||Registries.ITEM.getId(i).toString().contains(query))
            .sorted(Comparator.comparing(i->i.getName().getString())).toList();
        page=Math.clamp(page,0,Math.max(0,(visible.size()-1)/rows));
        addDrawableChild(new HeraButton(x+12,y+32,132,21,"Item-ESP: "+(config.itemEsp?"An":"Aus"),config.itemEsp,()->{config.itemEsp=!config.itemEsp;rebuild();}));
        addDrawableChild(new HeraButton(x+150,y+32,w-162,21,selectedOnly?"Nur Auswahl":"Alle Items",selectedOnly,()->{selectedOnly=!selectedOnly;page=0;rebuild();}));
        addDrawableChild(new HeraButton(x+12,y+h-76,110,20,"Reichweite: "+config.espRange+" m",false,()->{config.espRange=config.espRange>=128?16:config.espRange*2;rebuild();}));
        for(int row=0;row<rows && page*rows+row<visible.size();row++){
            Item item=visible.get(page*rows+row);String id=Registries.ITEM.getId(item).toString();int ry=y+87+row*29;
            addDrawableChild(new HeraButton(x+12,ry,w-24,26,item.getName().getString(),false,()->{
                if(!config.markedItems.remove(id) && config.markedItems.size()<512)config.markedItems.add(id);rebuild();
            }){@Override protected void renderWidget(DrawContext c,int mx,int my,float d){HeraTheme.round(c,getX(),getY(),getWidth(),getHeight(),5,isHovered()?0xFF343D4D:HeraTheme.RAISED);}});
        }
        addDrawableChild(new HeraButton(x+12,y+h-30,26,20,"‹",false,()->{page--;rebuild();}));
        addDrawableChild(new HeraButton(x+42,y+h-30,26,20,"›",false,()->{page++;rebuild();}));
        addDrawableChild(new HeraButton(x+w-90,y+h-30,78,20,"Fertig",true,this::close));
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0x88080B11);HeraTheme.panel(c,x,y,w,h);
        HeraTheme.title(c,"Item-Markierungen",x+14,y+13,1.15f,HeraTheme.TEXT);super.render(c,mx,my,delta);
        for(int row=0;row<rows && page*rows+row<visible.size();row++){
            Item item=visible.get(page*rows+row);String id=Registries.ITEM.getId(item).toString();int ry=y+87+row*29;
            c.drawItem(new ItemStack(item),x+18,ry+5);
            HeraTheme.label(c,HeraTheme.fit(item.getName().getString(),w-100),x+41,ry+4,HeraTheme.TEXT);
            HeraTheme.label(c,HeraTheme.fit(id,w-100),x+41,ry+15,HeraTheme.MUTED);
            HeraTheme.toggle(c,x+w-45,ry+7,config.markedItems.contains(id));
        }
        if(visible.isEmpty())HeraTheme.label(c,"Keine Items gefunden.",x+16,y+96,HeraTheme.MUTED);
        HeraTheme.label(c,config.markedItems.size()+" ausgewählt · "+(page+1)+" / "+(Math.max(0,(visible.size()-1)/rows)+1),x+76,y+h-24,HeraTheme.MUTED);
        HeraTheme.label(c,error.isEmpty()?"Drops & Container markieren · Inhalt bleibt unbekannt.":error,x+12,y+h-46,error.isEmpty()?HeraTheme.MUTED:0xFFFF9DAB);
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){page+=(int)-Math.signum(v);rebuild();return true;}
    @Override public void close(){try{config.save();client.setScreen(parent);}catch(IOException e){error="Speichern fehlgeschlagen.";}}
    @Override public boolean shouldPause(){return false;}
}
