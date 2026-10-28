package gg.hera.features;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import java.util.*;
/** Retains the known sender only for the exact received message, never guesses names. */
public final class ChatHeads {
 private static final Map<Text,GameProfile> pending=new IdentityHashMap<>();
 private ChatHeads(){}
 public static void remember(Text text,GameProfile profile){if(!Presentation.enabled("Chat Heads"))return;if(pending.size()>=32)pending.clear();pending.put(text,profile);}
 public static Text decorate(Text text){var profile=pending.remove(text);if(profile==null||!Presentation.enabled("Chat Heads"))return text;return Text.object(new net.minecraft.text.object.PlayerTextObjectContents(net.minecraft.component.type.ProfileComponent.ofStatic(profile),true)).append(" ").append(text);}
}
