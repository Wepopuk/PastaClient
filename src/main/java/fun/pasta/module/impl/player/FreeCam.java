package fun.pasta.module.impl.player;

import com.google.common.eventbus.Subscribe;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventMotion;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class FreeCam extends Module {

    private static int NEXT_FAKE_ID = -2_500_000;

    private final SliderOption speed = new SliderOption("Speed", 1.2f, 0.2f, 5.0f, 0.1f);

    private double savedX;
    private double savedY;
    private double savedZ;
    private float savedYaw;
    private float savedPitch;
    private Vector3d savedMotion = Vector3d.ZERO;
    private boolean savedNoClip;
    private boolean savedNoGravity;
    private boolean savedOnGround;
    private int savedFogType = -1;
    private float savedFogStart = 0.8F;

    private double camX;
    private double camY;
    private double camZ;

    private RemoteClientPlayerEntity fakePlayer;
    private int fakeEntityId = Integer.MIN_VALUE;

    public FreeCam() {
        super("FreeCam", Category.PLAYER);
        add(speed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            return;
        }

        savedX = mc.player.getPosX();
        savedY = mc.player.getPosY();
        savedZ = mc.player.getPosZ();
        savedYaw = mc.player.rotationYaw;
        savedPitch = mc.player.rotationPitch;
        savedMotion = mc.player.getMotion();
        savedNoClip = mc.player.noClip;
        savedNoGravity = mc.player.hasNoGravity();
        savedOnGround = mc.player.isOnGround();
        savedFogType = mc.gameSettings.ofFogType;
        savedFogStart = mc.gameSettings.ofFogStart;

        camX = savedX;
        camY = savedY;
        camZ = savedZ;

        spawnFakePlayer();

        // Real player becomes the camera entity and flies locally without collisions.
        mc.setRenderViewEntity(mc.player);
        mc.player.noClip = true;
        mc.player.setNoGravity(true);
        mc.player.setMotion(Vector3d.ZERO);
        mc.player.fallDistance = 0.0f;
        mc.gameSettings.ofFogType = 3;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        if (mc.getRenderViewEntity() != mc.player) {
            mc.setRenderViewEntity(mc.player);
        }

        if (fakePlayer == null || fakePlayer.removed) {
            spawnFakePlayer();
        } else {
            fakePlayer.setPositionAndRotation(savedX, savedY, savedZ, savedYaw, savedPitch);
            fakePlayer.prevPosX = savedX;
            fakePlayer.prevPosY = savedY;
            fakePlayer.prevPosZ = savedZ;
            fakePlayer.rotationYawHead = savedYaw;
            fakePlayer.renderYawOffset = savedYaw;
        }

        mc.player.noClip = true;
        mc.player.setNoGravity(true);
        mc.player.setSprinting(false);
        mc.player.setMotion(Vector3d.ZERO);
        mc.player.fallDistance = 0.0f;
        mc.gameSettings.ofFogType = 3;
        blockVanillaMovementKeys();

        if (mc.player.movementInput != null) {
            mc.player.movementInput.moveForward = 0.0F;
            mc.player.movementInput.moveStrafe = 0.0F;
            mc.player.movementInput.jump = false;
            mc.player.movementInput.sneaking = false;
        }
        mc.player.moveForward = 0.0F;
        mc.player.moveStrafing = 0.0F;
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (!enable || mc.player == null || mc.world == null) {
            return;
        }

        long handle = mc.getMainWindow().getHandle();
        boolean forwardPressed = isPhysicallyPressed(mc.gameSettings.keyBindForward, handle);
        boolean backPressed = isPhysicallyPressed(mc.gameSettings.keyBindBack, handle);
        boolean leftPressed = isPhysicallyPressed(mc.gameSettings.keyBindLeft, handle);
        boolean rightPressed = isPhysicallyPressed(mc.gameSettings.keyBindRight, handle);
        boolean jumpPressed = isPhysicallyPressed(mc.gameSettings.keyBindJump, handle);
        boolean sneakPressed = isPhysicallyPressed(mc.gameSettings.keyBindSneak, handle);
        boolean sprintPressed = isPhysicallyPressed(mc.gameSettings.keyBindSprint, handle);

        double moveSpeed = speed.getValue().doubleValue() * 0.35D;
        if (sprintPressed) {
            moveSpeed *= 1.7D;
        }

        float yaw = mc.player.rotationYaw;
        float pitch = mc.player.rotationPitch;
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        Vector3d lookDir = new Vector3d(
                -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad),
                -MathHelper.sin(pitchRad),
                MathHelper.cos(yawRad) * MathHelper.cos(pitchRad)
        );
        Vector3d rightDir = new Vector3d(
                MathHelper.cos(yawRad),
                0.0D,
                MathHelper.sin(yawRad)
        );

        Vector3d motion = Vector3d.ZERO;
        if (forwardPressed) {
            motion = motion.add(lookDir.scale(moveSpeed));
        }
        if (backPressed) {
            motion = motion.subtract(lookDir.scale(moveSpeed));
        }
        if (leftPressed) {
            motion = motion.subtract(rightDir.scale(moveSpeed));
        }
        if (rightPressed) {
            motion = motion.add(rightDir.scale(moveSpeed));
        }
        if (jumpPressed) {
            motion = motion.add(0.0D, moveSpeed, 0.0D);
        }
        if (sneakPressed) {
            motion = motion.add(0.0D, -moveSpeed, 0.0D);
        }

        double oldX = camX;
        double oldY = camY;
        double oldZ = camZ;
        camX += motion.x;
        camY += motion.y;
        camZ += motion.z;

        mc.player.prevPosX = oldX;
        mc.player.prevPosY = oldY;
        mc.player.prevPosZ = oldZ;
        mc.player.lastTickPosX = oldX;
        mc.player.lastTickPosY = oldY;
        mc.player.lastTickPosZ = oldZ;
        mc.player.setPosition(camX, camY, camZ);
        mc.player.setMotion(Vector3d.ZERO);
        mc.player.fallDistance = 0.0f;

        // Server keeps seeing the old position while local camera is moving.
        event.x = savedX;
        event.y = savedY;
        event.z = savedZ;
        event.yaw = savedYaw;
        event.pitch = savedPitch;
        event.onGround = savedOnGround;
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!enable || !event.isreceive()) {
            return;
        }

        if (event.packet instanceof SPlayerPositionLookPacket) {
            event.cancel();
        }
    }

    @Override
    public void onDisable() {
        try {
            removeFakePlayer();

            if (mc.player != null) {
                mc.setRenderViewEntity(mc.player);

                mc.player.noClip = savedNoClip;
                mc.player.setNoGravity(savedNoGravity);
                mc.player.setMotion(savedMotion);
                mc.player.setPositionAndRotation(savedX, savedY, savedZ, savedYaw, savedPitch);
                mc.player.prevPosX = savedX;
                mc.player.prevPosY = savedY;
                mc.player.prevPosZ = savedZ;
                mc.player.fallDistance = 0.0f;

                if (mc.player.connection != null) {
                    mc.player.connection.sendPacket(new net.minecraft.network.play.client.CPlayerPacket.PositionRotationPacket(
                            savedX, savedY, savedZ, savedYaw, savedPitch, savedOnGround
                    ));
                }
            }
            if (savedFogType != -1) {
                mc.gameSettings.ofFogType = savedFogType;
                mc.gameSettings.ofFogStart = savedFogStart;
            }
        } finally {
            super.onDisable();
        }
    }

    private void spawnFakePlayer() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        removeFakePlayer();

        fakeEntityId = NEXT_FAKE_ID--;
        fakePlayer = new RemoteClientPlayerEntity(mc.world, mc.player.getGameProfile());
        fakePlayer.copyLocationAndAnglesFrom(mc.player);
        fakePlayer.setPositionAndRotation(savedX, savedY, savedZ, savedYaw, savedPitch);
        fakePlayer.prevPosX = savedX;
        fakePlayer.prevPosY = savedY;
        fakePlayer.prevPosZ = savedZ;
        fakePlayer.rotationYawHead = savedYaw;
        fakePlayer.renderYawOffset = savedYaw;
        fakePlayer.inventory.copyInventory(mc.player.inventory);
        fakePlayer.setHealth(mc.player.getHealth());
        fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        fakePlayer.noClip = false;
        fakePlayer.setNoGravity(false);

        mc.world.addEntity(fakeEntityId, fakePlayer);
    }

    private void removeFakePlayer() {
        if (mc.world != null && fakeEntityId != Integer.MIN_VALUE) {
            mc.world.removeEntityFromWorld(fakeEntityId);
        }
        fakePlayer = null;
        fakeEntityId = Integer.MIN_VALUE;
    }

    private void blockVanillaMovementKeys() {
        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSneak,
                mc.gameSettings.keyBindSprint
        };
        for (KeyBinding key : keys) {
            key.setPressed(false);
        }
    }

    private boolean isPhysicallyPressed(KeyBinding keyBinding, long handle) {
        return InputMappings.isKeyDown(handle, keyBinding.getDefault().getKeyCode());
    }
}
