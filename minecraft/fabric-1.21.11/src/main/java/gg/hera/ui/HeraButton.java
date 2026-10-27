package gg.hera.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import org.lwjgl.glfw.GLFW;

public class HeraButton extends ClickableWidget {
    private final boolean accent;
    private final Runnable action;
    public HeraButton(int x,int y,int w,int h,String label,boolean accent,Runnable action) {
        super(x,y,w,h,HeraTheme.text(label));this.accent=accent;this.action=action;
    }
    @Override public void onClick(Click click,boolean doubled){action.run();}
    @Override public boolean keyPressed(KeyInput input){if(active&&isFocused()&&(input.key()==GLFW.GLFW_KEY_ENTER||input.key()==GLFW.GLFW_KEY_SPACE)){action.run();return true;}return false;}
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder){appendDefaultNarrations(builder);}
    @Override protected void renderWidget(DrawContext c,int mx,int my,float delta) {
        boolean hover=isHovered()||isFocused();
        HeraTheme.round(c,getX(),getY(),width,height,6,accent?(hover?0xFFFFBC8D:0xFFF0A46E):(hover?0xFF343D4D:HeraTheme.RAISED));
        if(isFocused()) { c.fill(getX()+7,getY()+height-2,getX()+width-7,getY()+height-1,HeraTheme.ACCENT); }
        var renderer=MinecraftClient.getInstance().textRenderer;
        HeraTheme.hudText(c,getMessage(),getX()+(width-HeraTheme.measure(getMessage(),false))/2,getY()+(height-10)/2,active?(accent?0xFF261A12:HeraTheme.TEXT):HeraTheme.MUTED,false,false);
    }
}
