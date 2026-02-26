package fun.pasta.commands.impl;

import fun.pasta.commands.Command;
import fun.pasta.util.chat.ChatUtil;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.addChatMessage("Список команд: \n\n.help - Список команд.\n.config - Конфиги\nбольше нет\nбольше нет");
        }
    }
}

