package fun.pasta.commands.impl;

import fun.pasta.Pasta;
import fun.pasta.commands.Command;
import fun.pasta.util.chat.ChatUtil;
import net.minecraft.util.text.TextFormatting;

public class FriendCommand extends Command {

    public FriendCommand() {
        super("friend");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            sendHelp();
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "add":
                if (args.length < 2) {
                    sendHelp();
                    return;
                }
                if (Pasta.instance.friendManager.isFriend(args[1])) {
                    ChatUtil.addChatMessage("Player is already in friends: " + args[1]);
                    return;
                }
                Pasta.instance.friendManager.addFriend(args[1]);
                ChatUtil.addChatMessage("Added friend: " + args[1]);
                break;
            case "remove":
            case "del":
            case "delete":
                if (args.length < 2) {
                    sendHelp();
                    return;
                }
                if (!Pasta.instance.friendManager.isFriend(args[1])) {
                    ChatUtil.addChatMessage("Player is not in friends: " + args[1]);
                    return;
                }
                Pasta.instance.friendManager.removeFriend(args[1]);
                ChatUtil.addChatMessage("Removed friend: " + args[1]);
                break;
            case "list":
                if (Pasta.instance.friendManager.getFriends().isEmpty()) {
                    ChatUtil.addChatMessage("Friends list is empty.");
                    return;
                }
                ChatUtil.addChatMessage("Friends:");
                for (String name : Pasta.instance.friendManager.getFriends()) {
                    ChatUtil.addChatMessage(TextFormatting.GRAY + "> " + TextFormatting.WHITE + name);
                }
                break;
            case "clear":
                Pasta.instance.friendManager.clear();
                ChatUtil.addChatMessage("Friends list cleared.");
                break;
            default:
                sendHelp();
                break;
        }
    }

    private void sendHelp() {
        ChatUtil.addChatMessage(".friend add <name>");
        ChatUtil.addChatMessage(".friend remove <name>");
        ChatUtil.addChatMessage(".friend list");
        ChatUtil.addChatMessage(".friend clear");
    }
}
