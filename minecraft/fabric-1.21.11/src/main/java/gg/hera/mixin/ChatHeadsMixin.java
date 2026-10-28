package gg.hera.mixin;
import gg.hera.features.ChatHeads;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ChatHud.class)
public abstract class ChatHeadsMixin {
 @ModifyVariable(method="addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",at=@At("HEAD"),argsOnly=true)
 private Text hera$head(Text message){return ChatHeads.decorate(message);}
}
