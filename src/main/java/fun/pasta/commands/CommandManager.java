package fun.pasta.commands;

import fun.pasta.commands.impl.ConfigCommand;
import fun.pasta.commands.impl.FriendCommand;
import fun.pasta.commands.impl.GpsCommand;
import fun.pasta.commands.impl.HelpCommand;
import fun.pasta.commands.impl.H0td04ekCommand;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {

    public static final CommandManager INSTANCE = new CommandManager();

    private final Map<String, Command> commands = new HashMap<>();

    public CommandManager() {
        init();
    }

    private void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public void executeCommand(String name, String[] args) {
        Command command = commands.get(name.toLowerCase());
        if (command != null) {
            command.execute(args);
        } else {
            // можно вывести сообщение об ошибке
        }
    }

    public void init() {
        registerCommand(new HelpCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new FriendCommand());
        registerCommand(new GpsCommand());
        registerCommand(new H0td04ekCommand());
    }
}
