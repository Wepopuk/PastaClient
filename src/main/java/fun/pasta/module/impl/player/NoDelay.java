package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.Field;

public class NoDelay extends Module {

    private Field jumpTicksField;
    private boolean triedResolveField;

    public NoDelay() {
        super("NoDelay", Category.PLAYER);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        if (!mc.gameSettings.keyBindJump.isKeyDown()) {
            return;
        }

        if (mc.player.isPassenger()) {
            return;
        }

        if (ensureJumpTicksField()) {
            try {
                jumpTicksField.setInt(mc.player, 0);
            } catch (IllegalAccessException ignored) {
            }
            return;
        }

        // Fallback for environments where private field reflection is blocked.
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    private boolean ensureJumpTicksField() {
        if (jumpTicksField != null) {
            return true;
        }
        if (triedResolveField) {
            return false;
        }
        triedResolveField = true;

        try {
            jumpTicksField = LivingEntity.class.getDeclaredField("jumpTicks");
            jumpTicksField.setAccessible(true);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
