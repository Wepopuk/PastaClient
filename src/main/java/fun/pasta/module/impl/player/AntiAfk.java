package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.time.TimerUtil;

import java.util.Random;

public class AntiAfk extends Module {

    private final BooleanOption withMove = new BooleanOption("Move", true);
    private final SliderOption idleSeconds = new SliderOption("Idle Chat Delay", 15.0F, 5.0F, 60.0F, 1.0F)
            .setVisible(() -> !withMove.get());

    private final TimerUtil idleTimer = new TimerUtil();
    private final TimerUtil actionTimer = new TimerUtil();
    private final Random random = new Random();

    private int movementDurationTicks;
    private int movementTick;
    private int nextMovementDelayMs = 500;
    private boolean movingPeriod;

    public AntiAfk() {
        super("AntiAfk", Category.PLAYER);
        add(withMove, idleSeconds);
        idleTimer.reset();
        actionTimer.reset();
        nextMovementDelayMs = randomDelay(400, 900);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        if (withMove.get()) {
            handleMoveMode();
            return;
        }

        if (isMoving()) {
            idleTimer.reset();
            return;
        }

        if (idleTimer.hasReached((long) (idleSeconds.getValue().floatValue() * 1000.0F))) {
            mc.player.sendChatMessage("/" + randomWord(5));
            idleTimer.reset();
        }
    }

    private void handleMoveMode() {
        releaseAllKeys();

        if (!movingPeriod) {
            if (actionTimer.hasReached(nextMovementDelayMs)) {
                movingPeriod = true;
                movementDurationTicks = randomDelay(2, 6);
                movementTick = 0;
                actionTimer.reset();
            }
            return;
        }

        if (movementTick < movementDurationTicks) {
            executeRandomMovement();
            movementTick++;
            return;
        }

        movingPeriod = false;
        nextMovementDelayMs = randomDelay(100, 240);
        actionTimer.reset();
        releaseAllKeys();
    }

    private void executeRandomMovement() {
        if (random.nextBoolean()) mc.gameSettings.keyBindForward.setPressed(true);
        if (random.nextInt(4) == 0) mc.gameSettings.keyBindBack.setPressed(true);
        if (random.nextBoolean()) mc.gameSettings.keyBindLeft.setPressed(true);
        if (random.nextBoolean()) mc.gameSettings.keyBindRight.setPressed(true);
        if (random.nextInt(3) == 0 && mc.player.isOnGround()) mc.gameSettings.keyBindJump.setPressed(true);
    }

    private void releaseAllKeys() {
        mc.gameSettings.keyBindForward.setPressed(false);
        mc.gameSettings.keyBindBack.setPressed(false);
        mc.gameSettings.keyBindLeft.setPressed(false);
        mc.gameSettings.keyBindRight.setPressed(false);
        mc.gameSettings.keyBindJump.setPressed(false);
    }

    private boolean isMoving() {
        return Math.abs(mc.player.moveForward) > 1.0E-4F || Math.abs(mc.player.moveStrafing) > 1.0E-4F;
    }

    private int randomDelay(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    private String randomWord(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            out.append(chars.charAt(random.nextInt(chars.length())));
        }
        return out.toString();
    }

    @Override
    public void onDisable() {
        releaseAllKeys();
        super.onDisable();
    }
}

