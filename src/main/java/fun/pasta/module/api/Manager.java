package fun.pasta.module.api;

import com.google.common.eventbus.Subscribe;
import fun.pasta.module.impl.misc.AutoMessage;
import fun.pasta.module.impl.misc.AutoAccept;
import fun.pasta.module.impl.misc.DiscordActivity;
import fun.pasta.module.impl.misc.HitSound;
import fun.pasta.module.impl.misc.NameProtect;
import fun.pasta.module.impl.misc.SRpSpoofer;
import fun.pasta.module.impl.player.*;
import net.minecraft.client.Minecraft;
import fun.pasta.Pasta;
import fun.pasta.event.EventKey;
import fun.pasta.module.impl.combat.*;
import fun.pasta.module.impl.misc.ItemScroller;
import fun.pasta.module.impl.move.*;
import fun.pasta.module.impl.visual.*;

import java.util.ArrayList;
import java.util.List;


public class Manager {

    public List<Module> modules = new ArrayList<>();

    public KillAura killAura;
    public PearlTarget pearlTarget;
    public AntiBot antiBot;
    public AntiPush antiPush;
    public ItemScroller itemScroller;
    public NoDelay noDelay;
    public Speed speed;
    public Strafe strafe;
    public AirStuck airStuck;
    public Sprint sprint;
    public AutoPotion autoPotion;
    public AutoSwap autoSwap;
    public Module autoExplosion;
    public ViewModel viewModel;
    public SwingAnimation swingAnimation;
    public ESP esp;
    public ItemESP itemESP;
    public DeathEffect deathEffect;
    public Arrows arrows;
    public TargetESP targetESP;
    public Particles particles;
    public ElytraHelper elytraHelper;
    public ElytraFly elytraFly;
    public ElytraJump elytraJump;
    public ElytraBounce elytraBounce;
    public ClickGui clickGui;
    public NameTags nameTags;
    public MultiActions multiActions;
    public AutoRespawn autoRespawn;
    public FreeCam freeCam;
    public AutoMessage autoMessage;
    public AutoAccept autoAccept;
    public DiscordActivity discordActivity;
    public HitSound hitSound;
    public NoFriendDamage noFriendDamage;
    public FullBright fullBright;

    public void initModule() {
        modules.add(killAura = new KillAura());
        modules.add(pearlTarget = new PearlTarget());
        modules.add(antiBot = new AntiBot());
        modules.add(antiPush = new AntiPush());
        modules.add(itemScroller = new ItemScroller());
        modules.add(noDelay = new NoDelay());
        modules.add(speed = new Speed());
        modules.add(strafe = new Strafe());
        modules.add(airStuck = new AirStuck());
        modules.add(sprint = new Sprint());
        modules.add(autoPotion = new AutoPotion());
        modules.add(autoSwap = new AutoSwap());
        registerOptionalAutoExplosion();
        modules.add(viewModel = new ViewModel());
        modules.add(swingAnimation = new SwingAnimation());
        modules.add(new AutoTotem());
        modules.add(new NoRender());
        modules.add(new FreeLock());
        modules.add(freeCam = new FreeCam());
        modules.add(multiActions = new MultiActions());
        modules.add(autoRespawn = new AutoRespawn());
        modules.add(new InventoryMove());
        modules.add(new ClickPearl());
        modules.add(new Velocity());
        modules.add(new AutoArmor());
        modules.add(new Fly());
        modules.add(new Hud());
        modules.add(nameTags = new NameTags());
        modules.add(esp = new ESP());
        modules.add(itemESP = new ItemESP());
        modules.add(deathEffect = new DeathEffect());
        modules.add(arrows = new Arrows());
        modules.add(targetESP = new TargetESP());
        modules.add(particles = new Particles());
        modules.add(new TriggerBot());
        modules.add(elytraFly = new ElytraFly());
        modules.add(elytraJump = new ElytraJump());
        modules.add(elytraBounce = new ElytraBounce());
        modules.add(elytraHelper = new ElytraHelper());
        modules.add(new NameProtect());
        modules.add(clickGui = new ClickGui());
        modules.add(autoMessage = new AutoMessage());
        modules.add(autoAccept = new AutoAccept());
        modules.add(discordActivity = new DiscordActivity());
        modules.add(hitSound = new HitSound());
        modules.add(new SRpSpoofer());
        modules.add(noFriendDamage = new NoFriendDamage());
        modules.add(fullBright = new FullBright());

        Pasta.instance.eventBus.register(this);
    }

    private void registerOptionalAutoExplosion() {
        try {
            Class<?> cls = Class.forName("fun.pasta.module.impl.combat.AutoExplosion");
            Object instance = cls.getDeclaredConstructor().newInstance();
            if (instance instanceof Module) {
                autoExplosion = (Module) instance;
                modules.add(autoExplosion);
            }
        } catch (Throwable ignored) {
            autoExplosion = null;
        }
    }

    @Subscribe
    public void onKeyToggle(EventKey eventKey) {
        if (Minecraft.getInstance().currentScreen != null)
            return;

        for (Module f : modules) {
            if (eventKey.key == f.key) {
                f.toggle();
            }
        }
    }

    public Module getModule(String name) {
        for (Module m : modules) {
            if (m.name.equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.category == category) {
                list.add(m);
            }
        }
        return list;
    }
}
