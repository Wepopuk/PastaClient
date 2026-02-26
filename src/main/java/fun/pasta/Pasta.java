package fun.pasta;

import com.google.common.eventbus.EventBus;
import fun.pasta.altManager.AccountsConfig;
import fun.pasta.commands.CommandManager;
import fun.pasta.event.EventManager;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import fun.pasta.altManager.AltManagerScreen;
import fun.pasta.altManager.LastAccountConfig;
import fun.pasta.drag.DragManager;
import fun.pasta.drag.Dragging;
import fun.pasta.friend.FriendManager;
import fun.pasta.module.api.Module;
import fun.pasta.module.impl.visual.hud.HudManager;
import fun.pasta.ui.clickGui.CSGui;
import fun.pasta.ui.clickGui.DropdownGui;
import fun.pasta.event.EventKey;
import fun.pasta.module.api.Manager;
import fun.pasta.ui.backGround.BackGroundFrontend;
import fun.pasta.ui.backGround.BackGroundManager;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.aura.FreerotationHelper;
import fun.pasta.util.aura.RotationService;
import fun.pasta.config.ConfigManager;

public class Pasta implements IMinecraft {

    public static String NAME = "Pasta";
    public static String VERSION = "ver. 1488";
    public static Pasta instance;
    public static CommandManager commandManager;
    public EventBus eventBus = new EventBus();
    public Manager manager;
    public BackGroundManager backGroundManager;
    public CSGui csGui;
    public DropdownGui dropdownGui;
    public BackGroundFrontend backGroundFrontend;
    public AltManagerScreen altManagerScreen;
    public LastAccountConfig lastAccountConfig;
    public AccountsConfig accountsConfig;
    public HudManager hudManager;
    public FriendManager friendManager;
    public int uid;
    public String login = "h0td04ek";
    public static EventManager eventManager;
    private static boolean shutdownSaveHookRegistered;

    public static EventManager getEventManager() {
        return eventManager;
    }

    public Pasta() {
        instance = this;
        commandManager = new CommandManager();
        friendManager = new FriendManager();
        initManager();
        initScreen();
        initRotation();
        initHud();
        loadDragging();
        initOther();
        commandManager.init();
        accountsConfig = new AccountsConfig();

        eventBus.register(this);
        new ConfigManager().get("default").load();
        registerShutdownSaveHook();
    }

    public static CommandManager getCommandManager() {
        return commandManager;
    }

    private void initOther() {
        try {
            lastAccountConfig = new LastAccountConfig();
            lastAccountConfig.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initManager() {
        manager = new Manager();
        manager.initModule();

        backGroundManager = new BackGroundManager();
        backGroundManager.init();
    }

    private void initScreen() {
        csGui = new CSGui();
        dropdownGui = new DropdownGui();
        backGroundFrontend = new BackGroundFrontend();
        altManagerScreen = new AltManagerScreen();
    }

    private void initRotation() {
        new RotationService();
        new FreerotationHelper();
    }

    /* create and load dragging */
    public Dragging createDrag(Module module, String name, float x, float y) {
        DragManager.draggables.put(name, new Dragging(module, name, x, y));
        return DragManager.draggables.get(name);
    }

    private void loadDragging() {
        DragManager.load();
    }

    private void registerShutdownSaveHook() {
        if (shutdownSaveHookRegistered) {
            return;
        }
        shutdownSaveHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                new ConfigManager().get("default").save();
            } catch (Throwable ignored) {
            }
            try {
                DragManager.save();
            } catch (Throwable ignored) {
            }
        }, "pasta-save-hook"));
    }

    private void initHud() {
        hudManager = new HudManager();
    }

    /* render background image */
    public void drawBackGround() {
        backGroundManager.drawBackGroundInSelect();
    }

    /* init key handler */
    EventKey eventKey = new EventKey();

    public void initKey(int key) {
        eventKey.key = key;
        eventBus.post(eventKey);

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            String mode = manager.clickGui.mode.get();
            if (mode.equalsIgnoreCase("CSGUI")) {
                mc.displayGuiScreen(csGui);
            } else {
                mc.displayGuiScreen(dropdownGui);
            }
        }
    }

}
