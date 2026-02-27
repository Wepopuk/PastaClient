package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;

import java.lang.reflect.Field;

public class FastBreak extends Module {

    private final SliderOption speed = new SliderOption("Boost", 0.7F, 0.3F, 1.0F, 0.1F);

    private Field blockHitDelayField;
    private Field curBlockDamageField;
    private boolean reflectionReady;

    public FastBreak() {
        super("FastBreak", Category.PLAYER);
        add(speed);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null || mc.playerController == null) {
            return;
        }
        if (!initReflection()) {
            return;
        }

        try {
            blockHitDelayField.setInt(mc.playerController, 0);
            if (mc.playerController.getIsHittingBlock()) {
                float current = curBlockDamageField.getFloat(mc.playerController);
                current += speed.getValue().floatValue() * 0.2F;
                if (current > 1.0F) current = 1.0F;
                curBlockDamageField.setFloat(mc.playerController, current);
            }
        } catch (IllegalAccessException ignored) {
        }
    }

    private boolean initReflection() {
        if (reflectionReady) {
            return true;
        }
        try {
            blockHitDelayField = mc.playerController.getClass().getDeclaredField("blockHitDelay");
            blockHitDelayField.setAccessible(true);

            curBlockDamageField = mc.playerController.getClass().getDeclaredField("curBlockDamageMP");
            curBlockDamageField.setAccessible(true);

            reflectionReady = true;
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }
}

