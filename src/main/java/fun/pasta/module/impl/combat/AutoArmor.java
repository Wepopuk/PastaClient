package fun.pasta.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import fun.pasta.event.EventUpdate;
import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.util.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class AutoArmor extends Module {
    private final StopWatch stopWatch = new StopWatch();

    public AutoArmor() {
        super("AutoArmor", Category.COMBAT);
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        PlayerInventory inventory = mc.player.inventory;
        int[] bestSlots = new int[4];
        int[] bestValues = new int[4];

        Arrays.fill(bestSlots, -1);
        Arrays.fill(bestValues, 0);

        for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
            ItemStack currentArmor = inventory.armorItemInSlot(armorSlot);

            if (isValidItem(currentArmor) && currentArmor.getItem() instanceof ArmorItem) {
                ArmorItem armorItem = (ArmorItem) currentArmor.getItem();
                bestValues[armorSlot] = calculateArmorValue(armorItem, currentArmor);
            }
        }

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);

            if (!isValidItem(stack) || !(stack.getItem() instanceof ArmorItem)) continue;

            ArmorItem armorItem = (ArmorItem) stack.getItem();
            int armorType = armorItem.slot.getIndex();
            int value = calculateArmorValue(armorItem, stack);

            if (value > bestValues[armorType]) {
                bestSlots[armorType] = slot;
                bestValues[armorType] = value;
            }
        }

        ArrayList<Integer> armorSlots = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(armorSlots);

        for (int armorType : armorSlots) {
            int bestSlot = bestSlots[armorType];

            if (bestSlot == -1) continue;

            if (bestSlot < 9) {
                bestSlot += 36;
            }

            if (!stopWatch.isreached(100)) break;

            ItemStack currentArmor = inventory.armorItemInSlot(armorType);

            if (isValidItem(currentArmor)) {
                mc.playerController.windowClick(0, 8 - armorType, 0, ClickType.QUICK_MOVE, mc.player);
            }

            mc.playerController.windowClick(0, bestSlot, 0, ClickType.QUICK_MOVE, mc.player);
            stopWatch.reset();
            break;
        }
    }

    private boolean isValidItem(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    private int calculateArmorValue(ArmorItem armor, ItemStack stack) {
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack);
        IArmorMaterial material = armor.getArmorMaterial();
        int reduction = material.getDamageReductionAmount(armor.getEquipmentSlot());
        int baseValue = armor.getDamageReduceAmount() * 20;
        int enchantValue = protection * 12;
        int toughnessValue = (int) (armor.toughness * 2);
        int materialValue = reduction * 5;

        return (baseValue + enchantValue + toughnessValue + materialValue) >> 3;
    }
}