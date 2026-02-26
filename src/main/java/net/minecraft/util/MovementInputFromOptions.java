package net.minecraft.util;

import net.minecraft.client.GameSettings;
import fun.pasta.Pasta;
import fun.pasta.event.EventInput;

public class MovementInputFromOptions extends MovementInput
{
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn)
    {
        this.gameSettings = gameSettingsIn;
    }

    public void tickMovement(boolean p_225607_1_)
    {
        this.forwardKeyDown = this.gameSettings.keyBindForward.isKeyDown();
        this.backKeyDown = this.gameSettings.keyBindBack.isKeyDown();
        this.leftKeyDown = this.gameSettings.keyBindLeft.isKeyDown();
        this.rightKeyDown = this.gameSettings.keyBindRight.isKeyDown();
        this.moveForward = this.forwardKeyDown == this.backKeyDown ? 0.0F : (this.forwardKeyDown ? 1.0F : -1.0F);
        this.moveStrafe = this.leftKeyDown == this.rightKeyDown ? 0.0F : (this.leftKeyDown ? 1.0F : -1.0F);
        this.jump = this.gameSettings.keyBindJump.isKeyDown();
        this.sneaking = this.gameSettings.keyBindSneak.isKeyDown();

        EventInput event = new EventInput();

        event.forward = (moveForward);
        event.strafe = (moveStrafe);
        event.jump = (jump);
        event.sneak = (sneaking);

        Pasta.instance.eventBus.post(event);

        final double sneakMultiplier = 0.3;
        this.moveForward = event.forward;
        this.moveStrafe = event.strafe;
        this.jump = event.jump;
        this.sneaking = event.sneak;

        if (p_225607_1_) {
            this.moveStrafe = (float) (this.moveStrafe * sneakMultiplier);
            this.moveForward = (float) (this.moveForward * sneakMultiplier);
        }
    }
}
