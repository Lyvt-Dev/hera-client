package gg.hera.mixin;
import gg.hera.features.Presentation;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class HudPresentationMixin {
 @Shadow private Text overlayMessage;@Shadow private int overlayRemaining;@Shadow private Text title;@Shadow private Text subtitle;@Shadow private int titleRemainTicks;
 @Inject(method="renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",at=@At("HEAD"),cancellable=true)
 private void hera$score(DrawContext c,RenderTickCounter t,CallbackInfo ci){if(Presentation.hudEnabled("Scoreboard"))ci.cancel();}
 @Inject(method="renderOverlayMessage",at=@At("HEAD"),cancellable=true)
 private void hera$action(DrawContext c,RenderTickCounter t,CallbackInfo ci){if(Presentation.hudEnabled("Action Bar")){Presentation.action=overlayMessage==null?Text.empty():overlayMessage;Presentation.actionTicks=overlayRemaining;ci.cancel();}}
 @Inject(method="renderTitleAndSubtitle",at=@At("HEAD"),cancellable=true)
 private void hera$title(DrawContext c,RenderTickCounter t,CallbackInfo ci){if(Presentation.hudEnabled("Titles")){Presentation.title=title==null?Text.empty():title;Presentation.subtitle=subtitle==null?Text.empty():subtitle;Presentation.titleTicks=titleRemainTicks;ci.cancel();}}
 @Inject(method="renderStatusEffectOverlay",at=@At("HEAD"),cancellable=true)
 private void hera$effects(DrawContext c,RenderTickCounter t,CallbackInfo ci){if(Presentation.hudEnabled("Potion Status"))ci.cancel();}
 @Inject(method="renderHeldItemTooltip",at=@At("HEAD"),cancellable=true)
 private void hera$held(DrawContext c,CallbackInfo ci){if(Presentation.hudEnabled("Helditem"))ci.cancel();}
}
