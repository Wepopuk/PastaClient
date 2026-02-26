package fun.pasta.util.chat;

import fun.pasta.util.IMinecraft;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class ChatUtil implements IMinecraft {
    public static void addChatMessage(String msg) {
        mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent(TextFormatting.YELLOW + "PastaClient" + TextFormatting.GRAY + " -> " + TextFormatting.RESET + msg));
    }
}
