package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.settings.PointOfView;
import fun.pasta.event.EventKey;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.util.aura.Rotation;
import fun.pasta.util.aura.RotationService;

public class FreeLock extends Module {

    private final BooleanOption thirdPerson = new BooleanOption("Включать 3 лицо", true);

    private boolean enabled;
    private float savedYaw;
    private float savedPitch;
    private float lockYaw;
    private float lockPitch;

    public FreeLock() {
        super("FreeLock", Category.PLAYER);
        add(thirdPerson);
    }

    @Subscribe
    public void onKey(EventKey event) {
    }

    private void lockCamera() {
        savedYaw = mc.player.rotationYaw;
        savedPitch = mc.player.rotationPitch;

        lockYaw = savedYaw;
        lockPitch = savedPitch;

        if (thirdPerson.get()) {
            mc.gameSettings.setPointOfView(PointOfView.THIRD_PERSON_BACK);
        }
    }

    private void restoreCamera() {
        if (thirdPerson.get()) {
            mc.gameSettings.setPointOfView(PointOfView.FIRST_PERSON);
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (enabled) {
            Rotation rotation = new Rotation(lockYaw, lockPitch);
            RotationService.update(rotation, 360, 360, 0, 5);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        enabled = false;
        restoreCamera();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        enabled = true;
        lockCamera();
        savedYaw = mc.player.rotationYaw;
        savedPitch = mc.player.rotationPitch;
    }
}