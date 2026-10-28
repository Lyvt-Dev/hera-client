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
    private long lastFrame;private float hoverAmount;
    private net.minecraft.text.Text measuredMessage,displayMessage;private int measuredWidth=-1,displayWidth;
    public HeraButton(int x,int y,int w,int h,String label,boolean accent,Runnable action) {
        super(x,y,w,h,HeraTheme.text(label));this.accent=accent;this.action=action;
    }
    @Override public void onClick(Click click,boolean doubled){action.run();}
    @Override public boolean keyPressed(KeyInput input){if(active&&isFocused()&&(input.key()==GLFW.GLFW_KEY_ENTER||input.key()==GLFW.GLFW_KEY_SPACE)){action.run();return true;}return false;}
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder){appendDefaultNarrations(builder);}
    private static int mix(int a,int b,float t){int result=0xFF000000;for(int shift=0;shift<24;shift+=8)result|=Math.round(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;return result;}
    @Override protected void renderWidget(DrawContext c,int mx,int my,float delta) {
        boolean hover=isHovered()||isFocused();
        long now=System.nanoTime();float elapsed=lastFrame==0?1:Math.min(1,(now-lastFrame)/160_000_000f);lastFrame=now;
        hoverAmount=Math.clamp(hoverAmount+(hover&&active?elapsed:-elapsed),0,1);
        int base=accent?HeraTheme.ACCENT:HeraTheme.RAISED,target=accent?0xFFFFB17D:0xFF30363F;
        HeraTheme.round(c,getX(),getY(),width,height,5,mix(base,target,hoverAmount));
        if(isFocused()) { c.fill(getX()+7,getY()+height-2,getX()+width-7,getY()+height-1,HeraTheme.ACCENT); }

        if(measuredMessage!=getMessage()||measuredWidth!=width){measuredMessage=getMessage();measuredWidth=width;displayMessage=HeraTheme.text(HeraTheme.fit(getMessage().getString(),Math.max(0,width-12)));displayWidth=HeraTheme.measure(displayMessage,false);}
        HeraTheme.hudText(c,displayMessage,getX()+(width-displayWidth)/2,getY()+(height-10)/2,active?(accent?0xFF261A12:HeraTheme.TEXT):HeraTheme.MUTED,false,false);
    }
}
