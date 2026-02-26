package fun.pasta.module.impl.move;

import com.google.common.eventbus.Subscribe;
import fun.pasta.Pasta;
import fun.pasta.event.EventKey;
import fun.pasta.event.EventPacket;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BindOption;
import fun.pasta.module.settings.BooleanOption;
import fun.pasta.module.settings.ModeOption;
import fun.pasta.module.settings.SliderOption;
import fun.pasta.util.time.TimerUtil;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

public class Speed extends Module {

    private static final float S20 = 0.3085F;
    private static final float S15 = 0.2948F;
    private static final float S0 = 0.254F;
    private static final float S25 = 0.32125F;

    private static final String ORB_HERACLES_2 = "\u0428\u0430\u0440 \u0413\u0435\u0440\u0430\u043a\u043b\u0430 2";
    private static final String ORB_AID_2 = "\u0428\u0430\u0440 \u0410\u0438\u0434\u0430 2";
    private static final String ORB_CHAMPION = "\u0428\u0430\u0440 CHAMPION";
    private static final String ORB_GOD = "\u0428\u0430\u0440 GOD";
    private static final String ORB_BUNNY = "\u0428\u0430\u0440 BUNNY";
    private static final String CUBE = "\u041a\u0423\u0411\u0418\u041a-\u0420\u0423\u0411\u0418\u041a";
    private static final String TALISMAN_VENOM = "\u0422\u0430\u043b\u0438\u0441\u043c\u0430\u043d \u0412\u0435\u043d\u043e\u043c\u0430";
    private static final String TALISMAN_CARTEL = "\u0422\u0430\u043b\u0438\u0441\u043c\u0430\u043d \u041a\u0430\u0440\u0442\u0435\u043b\u044f";

    private final ModeOption mode = new ModeOption("Mode", "Grim", "Grim", "Collision", "Meta", "MetaHvH");
    private final SliderOption speedGrim = new SliderOption("Speed", 1.09F, 1.0F, 1.2F, 0.01F)
            .setVisible(() -> isMode("Grim"));
    private final SliderOption radiusGrim = new SliderOption("Radius", 2.1F, 1.0F, 3.0F, 0.01F)
            .setVisible(() -> isMode("Grim"));
    private final BooleanOption onlyAura = new BooleanOption("Only With Aura", false)
            .setVisible(() -> isMode("Grim"));

    private final SliderOption speedMeta = new SliderOption("Speed", 0.36F, 0.10F, 1.0F, 0.01F)
            .setVisible(() -> isMode("Meta"));
    private final BooleanOption metaJump = new BooleanOption("Auto Jump", true)
            .setVisible(() -> isMode("Meta"));

    private final SliderOption speedCollision = new SliderOption("Speed", 1.6F, 0.5F, 2.0F, 0.1F)
            .setVisible(() -> isMode("Collision"));

    private final BindOption boostKey = new BindOption("BoostKey", -1)
            .setVisible(() -> isMode("MetaHvH"));
    private final BooleanOption damageBoost = new BooleanOption("Damage Boost", false);
    private final SliderOption boostSpeed = new SliderOption("Boost Speed", 0.7F, 0.1F, 5.0F, 0.1F)
            .setVisible(damageBoost::get);

    private final TimerUtil liquidExitTimer = new TimerUtil();

    private long boostEndTime;
    private long lastDamageTime;
    private boolean isBoosting;
    private boolean boostPressed;
    private boolean wasInLiquid;

    public Speed() {
        super("Speed", Category.MOVE);
        add(mode, speedGrim, radiusGrim, onlyAura, speedMeta, metaJump, speedCollision, boostKey, damageBoost, boostSpeed);
    }

    @Subscribe
    public void onKey(EventKey event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.key == boostKey.key) {
            boostPressed = true;
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!damageBoost.get() || mc.player == null || mc.world == null || !event.isreceive()) {
            return;
        }
        if (!(event.packet instanceof SEntityStatusPacket)) {
            return;
        }

        SEntityStatusPacket packet = (SEntityStatusPacket) event.packet;
        Entity entity = packet.getEntity(mc.world);
        if (entity == mc.player && packet.getOpCode() == 2) {
            lastDamageTime = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (boostPressed) {
            isBoosting = true;
            boostEndTime = System.currentTimeMillis() + 900L;
            boostPressed = false;
        }

        if (isBoosting && System.currentTimeMillis() > boostEndTime) {
            isBoosting = false;
        }

        if (mc.player.hurtTime > 0) {
            lastDamageTime = System.currentTimeMillis();
        }

        float boostMultiplier = resolveBoostMultiplier();

        if ((onlyAura.get() && !hasAuraTarget()) || mc.player.ticksExisted < 20) {
            return;
        }

        if (isMode("Meta")) {
            runMeta(boostMultiplier);
            return;
        }

        if (isMode("Collision")) {
            runCollision(boostMultiplier);
            return;
        }

        if (isMode("Grim")) {
            runGrim(boostMultiplier);
            return;
        }

        if (isMode("MetaHvH")) {
            runMetaHvh(boostMultiplier);
        }
    }

    private void runMeta(float boostMultiplier) {
        if (!mc.player.isInWater() && !mc.player.isOnGround()) {
            setSpeed(speedMeta.getValue().floatValue() * boostMultiplier);
        }

        if (metaJump.get() && isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.player.jump();
        }
    }

    private void runCollision(float boostMultiplier) {
        AxisAlignedBB box = mc.player.getBoundingBox().grow(0.1D);
        int armorStands = mc.world.getEntitiesWithinAABB(ArmorStandEntity.class, box).size();
        int living = mc.world.getEntitiesWithinAABB(LivingEntity.class, box).size();
        boolean canBoost = armorStands > 1 || living > 1;

        if (canBoost && !mc.player.isOnGround()) {
            float base = armorStands > 1 ? 1.0F / (float) armorStands : (speedCollision.getValue().floatValue() / 10.0F);
            mc.player.jumpMovementFactor = base * boostMultiplier;
        }
    }

    private void runGrim(float boostMultiplier) {
        if (!isMoving()) {
            return;
        }

        float radius = radiusGrim.getValue().floatValue();
        for (AbstractClientPlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player) {
                continue;
            }
            if (mc.player.getDistance(entity) <= radius) {
                float speed = speedGrim.getValue().floatValue() * boostMultiplier;
                Vector3d motion = mc.player.getMotion();
                mc.player.setMotion(motion.x * speed, motion.y, motion.z * speed);
                return;
            }
        }
    }

    private void runMetaHvh(float boostMultiplier) {
        boolean inLiquid = mc.player.isInWater()
                || mc.player.isInLava()
                || mc.player.areEyesInFluid(FluidTags.WATER)
                || mc.player.areEyesInFluid(FluidTags.LAVA);

        if (inLiquid) {
            wasInLiquid = true;
            return;
        }

        if (wasInLiquid) {
            wasInLiquid = false;
            liquidExitTimer.reset();
        }

        if (!liquidExitTimer.hasReached(250L)) {
            return;
        }

        if (mc.player.abilities.isFlying || mc.player.abilities.isCreativeMode || mc.player.isElytraFlying()) {
            return;
        }

        ItemStack offHandItem = mc.player.getHeldItemOffhand();
        String itemName = offHandItem.getDisplayName().getString();
        EffectInstance speedEffect = mc.player.getActivePotionEffect(Effects.SPEED);
        EffectInstance slowEffect = mc.player.getActivePotionEffect(Effects.SLOWNESS);

        float appliedSpeed = resolveMetaHvhBaseSpeed(itemName);
        boolean orbItem = hasOrbBoost(itemName);
        boolean venom = itemName.contains(TALISMAN_VENOM);
        boolean cartel = itemName.contains(TALISMAN_CARTEL);

        if (speedEffect != null) {
            int amp = speedEffect.getAmplifier();
            if (amp == 2) {
                if (orbItem) {
                    appliedSpeed = S20 * 1.63F;
                } else if (venom) {
                    appliedSpeed = S25 * 1.62F;
                } else if (cartel) {
                    appliedSpeed = S15 * 1.62F;
                } else {
                    appliedSpeed = S0 * 1.62F;
                }
            } else if (amp == 1) {
                if (orbItem) {
                    appliedSpeed = S20 * 1.423F;
                } else if (venom) {
                    appliedSpeed = S25 * 1.423F;
                } else if (cartel) {
                    appliedSpeed = S15 * 1.423F;
                } else {
                    appliedSpeed = S0 * 1.423F;
                }
            }
        }

        if (slowEffect != null) {
            appliedSpeed *= 0.835F;
        }

        if (!mc.player.isOnGround()) {
            appliedSpeed *= 1.435F;
        }

        if (isBoosting) {
            appliedSpeed *= 1.12F;
        }

        appliedSpeed *= boostMultiplier;
        setSpeed(appliedSpeed);
    }

    private boolean hasAuraTarget() {
        if (Pasta.instance == null || Pasta.instance.manager == null || Pasta.instance.manager.killAura == null) {
            return false;
        }
        return Pasta.instance.manager.killAura.target != null;
    }

    private float resolveBoostMultiplier() {
        float multiplier = 5.0F;
        if (!damageBoost.get()) {
            return multiplier;
        }

        long now = System.currentTimeMillis();
        if (now - lastDamageTime <= 1100L) {
            return 5.0F + boostSpeed.getValue().floatValue() / 25.0F;
        }
        return multiplier;
    }

    private float resolveMetaHvhBaseSpeed(String itemName) {
        if (hasOrbBoost(itemName)) {
            return S20;
        }
        if (itemName.contains(TALISMAN_VENOM)) {
            return S25;
        }
        if (itemName.contains(TALISMAN_CARTEL)) {
            return S15;
        }
        return S0;
    }

    private boolean hasOrbBoost(String itemName) {
        return itemName.contains(ORB_HERACLES_2)
                || itemName.contains(ORB_CHAMPION)
                || itemName.contains(ORB_AID_2)
                || itemName.contains(ORB_GOD)
                || itemName.contains(CUBE)
                || itemName.contains(ORB_BUNNY);
    }

    private void setSpeed(float speed) {
        if (!isMoving()) {
            return;
        }

        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        float yaw = mc.player.rotationYaw;

        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        if (forward != 0.0F) {
            if (strafe > 0.0F) {
                yaw += forward > 0.0F ? -45.0F : 45.0F;
            } else if (strafe < 0.0F) {
                yaw += forward > 0.0F ? 45.0F : -45.0F;
            }
            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
        }

        double rad = Math.toRadians(yaw + 90.0F);
        double x = forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad);
        double z = forward * speed * Math.sin(rad) - strafe * speed * Math.cos(rad);
        mc.player.setMotion(x, mc.player.getMotion().y, z);
    }

    private boolean isMoving() {
        return Math.abs(mc.player.moveForward) > 1.0E-4F || Math.abs(mc.player.moveStrafing) > 1.0E-4F;
    }

    private boolean isMode(String name) {
        return mode.get().equalsIgnoreCase(name);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.timer.timerSpeed = 1.0F;
        }
        isBoosting = false;
        boostPressed = false;
        wasInLiquid = false;
        boostEndTime = 0L;
        lastDamageTime = 0L;
        liquidExitTimer.reset();
        super.onDisable();
    }
}
