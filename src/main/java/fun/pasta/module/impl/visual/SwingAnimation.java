package fun.pasta.module.impl.visual;

import com.mojang.blaze3d.matrix.MatrixStack;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.anim.Easings;
import fun.pasta.util.anim.OldAnimation;
import fun.pasta.util.time.StopWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class SwingAnimation extends Module {

    public final ModeOption mode = new ModeOption("Mode", "Swipe",
            "Swipe", "Swipe 2", "Shift", "Break", "Lunge", "Spear", "Down", "Jelly", "Shake");
    public final SliderOption turn = new SliderOption("Turn", 0.0F, -7.5F, 35.0F, 0.1F);
    public final SliderOption speed = new SliderOption("Speed", 7.0F, 1.0F, 10.0F, 0.5F);
    public final BooleanOption strengthInclineSetting = new BooleanOption("Incline Enable", false);
    public final SliderOption strengthIncline = new SliderOption("Strength Incline", 20.0F, 20.0F, 75.0F, 0.1F);
    public final BooleanOption item360 = new BooleanOption("Item 360", false);
    public final SliderOption scale = new SliderOption("Scale", 1.0F, 0.1F, 2.0F, 0.1F);

    private final OldAnimation fapAnim = new OldAnimation();
    private final StopWatch timerSwinging = new StopWatch();
    private float speedMulPrev = 1.0F;
    private float phaseOffset = 0.0F;
    private boolean wasSwinging = false;
    private float lastSwingProgress = 0.0F;
    private float lastAdjustedSwing = 0.0F;

    public SwingAnimation() {
        super("SwingAnimation", Category.VISUAL);
        add(mode, turn, speed, strengthInclineSetting, strengthIncline, item360, scale);

        turn.setVisible(() -> {
            String m = mode.get();
            return !m.equals("Swipe") && !m.equals("Swipe 2")
                    && !m.equals("Jelly") && !m.equals("Shake")
                    && !m.equals("Down") && !m.equals("Spear")
                    && !m.equals("Lunge");
        });
        strengthIncline.setVisible(strengthInclineSetting::get);
    }

    public boolean apply(MatrixStack stack, HandSide handSide, float swingProgress, float partialTicks,
                         ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        float speedMul = speed.getValue().floatValue() / 7.0F;
        boolean swinging = swingProgress > 0.0F;
        boolean newSwing = swinging && (!wasSwinging || swingProgress < lastSwingProgress);

        if (newSwing) {
            phaseOffset = 0.0F;
            speedMulPrev = speedMul;
            lastAdjustedSwing = 0.0F;
        } else if (swinging && speedMul != speedMulPrev) {
            float current = MathHelper.clamp(swingProgress * speedMulPrev + phaseOffset, 0.0F, 1.0F);
            phaseOffset = current - swingProgress * speedMul;
            speedMulPrev = speedMul;
        } else if (!swinging) {
            phaseOffset = 0.0F;
            speedMulPrev = speedMul;
            lastAdjustedSwing = 0.0F;
        }

        wasSwinging = swinging;
        lastSwingProgress = swingProgress;

        float adjustedSwing = MathHelper.clamp(swingProgress * speedMul + phaseOffset, 0.0F, 1.0F);
        if (swinging && adjustedSwing < lastAdjustedSwing) {
            adjustedSwing = lastAdjustedSwing;
        } else if (swinging) {
            lastAdjustedSwing = adjustedSwing;
        }

        int handOffset = handSide == HandSide.RIGHT ? 1 : -1;
        float side = handOffset;

        float yBase = "Swipe".equals(mode.get()) ? -0.32F : -0.52F;
        stack.translate(side * 0.56F, yBase, -0.72F);

        animationProcess(stack, handOffset, adjustedSwing, partialTicks);

        if (item360.get()) {
            long currentTime = System.currentTimeMillis();
            float angle = ((currentTime % 3600) / 10.0F) * side;
            stack.rotate(Vector3f.XP.rotationDegrees(angle));
        }

        return true;
    }

    private float getFapUpdatedAnimation(boolean swinging) {
        if (!swinging) {
            timerSwinging.reset();
        }
        fapAnim.update();
        int delayWave = 215;
        double point1 = (timerSwinging.getTime() % delayWave >= delayWave / 2.0) ? 0.0 : 1.0;
        fapAnim.run(swinging ? point1 : 0.0, 0.2, Easings.QUINT_OUT);
        return fapAnim.get();
    }

    private void rotateYXZ(MatrixStack stack, float yawDeg, float pitchDeg, float rollDeg) {
        stack.rotate(Vector3f.YP.rotationDegrees(yawDeg));
        stack.rotate(Vector3f.XP.rotationDegrees(pitchDeg));
        stack.rotate(Vector3f.ZP.rotationDegrees(rollDeg));
    }

    public void animationProcess(MatrixStack stack, int handOffset, float swingProgress, float partialTicks) {
        float anim = MathHelper.sin(swingProgress * (float) Math.PI);
        String currentMode = mode.get();

        switch (currentMode) {
            case "Swipe 2":
                stack.translate(0.55F, -0.5F, -(0.7F + (anim * 0.002F)));
                stack.scale(1, 1, anim + 1);
                stack.rotate(Vector3f.XP.rotationDegrees(-90F * anim * (strengthIncline.getValue().floatValue() * 0.015F)));
                break;

            case "Shift":
                stack.translate(handOffset * 0.75F, -0.35F, -1.0F);
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * 90F));
                stack.rotate(Vector3f.ZP.rotationDegrees(handOffset * -60F));
                stack.rotate(Vector3f.XP.rotationDegrees(
                        -90F - strengthIncline.getValue().floatValue() * anim + turn.getValue().floatValue()));
                break;

            case "Break":
                stack.translate(handOffset * 0.75F, -0.35F, -1.0F);
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * 90F));
                stack.rotate(Vector3f.ZP.rotationDegrees(handOffset * -30F));
                stack.rotate(Vector3f.XP.rotationDegrees(
                        -90F - strengthIncline.getValue().floatValue() * anim + turn.getValue().floatValue()));
                break;

            case "Lunge":
                float f = MathHelper.sin(swingProgress * (float) Math.PI);
                float f2 = strengthIncline.getValue().floatValue() * 2.5F;
                float f4 = f2 / 10.0F / 10.0F * 40.0F;
                stack.translate(handOffset * 1.35F, -0.55F, -1.3F);
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * 45.0F));
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * f * -(f4 / 2.0F)));
                stack.rotate(Vector3f.ZP.rotationDegrees(handOffset * f * -(f4 / 2.0F)));
                stack.rotate(Vector3f.XP.rotationDegrees(f * -f4));
                stack.translate((handOffset * -0.5F), 0.2F, -0.3F);
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * 30F));
                stack.rotate(Vector3f.XP.rotationDegrees(-80F));
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * 60F));
                break;

            case "Spear":
                stack.translate(handOffset * 0.75F, -0.45F, -(1.0F + anim * 0.2F));
                stack.scale(1F, 1F, (anim * (strengthIncline.getValue().floatValue() * 0.03F)) + 1F);
                stack.rotate(Vector3f.XP.rotationDegrees(-90F));
                break;

            case "Down":
                stack.translate(handOffset * (0.75F - anim * 0.5F), -(0.35F - anim * 0.25F), -1.0F);
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * 65F));
                stack.rotate(Vector3f.ZP.rotationDegrees(handOffset));
                stack.rotate(Vector3f.XP.rotationDegrees(-110F - 20F * 4F * anim));
                break;

            case "Jelly":
                stack.translate(handOffset * 0.75F, -0.45F, -(1.0F + anim * 0.2F));
                stack.rotate(Vector3f.YP.rotationDegrees(handOffset * 35F));
                stack.scale(1F, 1F,
                        (anim * (swingProgress * 0.65F * (strengthIncline.getValue().floatValue() * 0.0175F))) + 1F);
                stack.rotate(Vector3f.XP.rotationDegrees(-90F));
                break;

            case "Shake":
                boolean swinging = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSwingInProgress;
                float fap = getFapUpdatedAnimation(swinging);
                float fapAnimVal = (1.0F - fap / 75.0F * strengthIncline.getValue().floatValue() * 4F);
                stack.translate(handOffset * 0.76F, -0.35F, -1.0F);
                rotateYXZ(stack, handOffset * (110 + fapAnimVal * 15F), -110F, 0F);
                rotateYXZ(stack, handOffset * 75F, 35F + fapAnimVal * 15F, 0F);
                break;

            case "Swipe":
                stack.scale(scale.getValue().floatValue(), scale.getValue().floatValue(), scale.getValue().floatValue());
                float swipeIncline = strengthInclineSetting.get()
                        ? (strengthIncline.getValue().floatValue() * 0.015F) : 1F;
                stack.translate(0, 0, anim * 0.1F);
                stack.rotate(Vector3f.XP.rotationDegrees(-90F * anim * swipeIncline));
                stack.rotate(Vector3f.ZP.rotationDegrees(handOffset * 40 * anim * swipeIncline));
                break;
        }
    }
}
