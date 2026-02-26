package fun.pasta.commands.impl;

import fun.pasta.commands.Command;
import fun.pasta.config.ConfigManager;
import fun.pasta.util.chat.ChatUtil;

public class ConfigCommand extends Command {

    private final ConfigManager manager = new ConfigManager();

    public ConfigCommand() {
        super("config");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.addChatMessage(".config save <name>");
            ChatUtil.addChatMessage(".config load <name>");
            ChatUtil.addChatMessage(".config list");
            ChatUtil.addChatMessage(".config remove <name>");
            return;
        }

        switch (args[0].toLowerCase()) {

            case "save":
                manager.get(args[1]).save();
                ChatUtil.addChatMessage("Конфиг сохранён: " + args[1]);
                break;

            case "load":
                if (!manager.exists(args[1])) {
                    ChatUtil.addChatMessage("Конфиг не найден");
                    return;
                }
                manager.get(args[1]).load();
                ChatUtil.addChatMessage("Конфиг загружен: " + args[1]);
                break;

            case "list":
                ChatUtil.addChatMessage("Конфиги:");
                for (String s : manager.list()) {
                    ChatUtil.addChatMessage(" - " + s);
                }
                break;

            case "remove":
                manager.delete(args[1]);
                ChatUtil.addChatMessage("Конфиг удалён: " + args[1]);
                break;
        }
    }
}
