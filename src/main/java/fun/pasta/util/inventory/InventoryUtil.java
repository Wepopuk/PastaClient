package fun.pasta.util.inventory;

import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import fun.pasta.util.IMinecraft;



public class InventoryUtil implements IMinecraft {

    public static int findBestSlotInHotBar() {
        int emptySlot = findEmptySlot();
        if (emptySlot != -1) {
            return emptySlot;
        } else {
            return findNonSwordSlot();
        }
    }

    private static int findEmptySlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).isEmpty() && mc.player.inventory.currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    private static int findNonSwordSlot() {
        for (int i = 0; i < 9; i++) {
            if (!(mc.player.inventory.getStackInSlot(i).getItem() instanceof SwordItem) && !(mc.player.inventory.getStackInSlot(i).getItem() instanceof ElytraItem) && mc.player.inventory.currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    public static void moveItem(int from, int to, boolean air) {
        if (from == to)
            return;
        pickupItem(from, 0);
        pickupItem(to, 0);
        if (air)
            pickupItem(from, 0);
    }

    public static void pickupItem(int slot, int button) {
        mc.playerController.windowClick(0, slot, button, ClickType.PICKUP, mc.player);
    }

    public static int findItem(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findHotbarItem(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findChestplateSlot() {
        Item[] chestplates = new Item[]{
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.LEATHER_CHESTPLATE
        };
        for (Item chestplate : chestplates) {
            int slot = findItem(chestplate);
            if (slot != -1) {
                return slot;
            }
        }
        return -1;
    }

    public static int toContainerSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }

    public static boolean swapToChestSlot(int inventorySlot) {
        if (mc.player == null || mc.playerController == null) return false;
        int slot = toContainerSlot(inventorySlot);
        int chestSlot = 6;
        mc.playerController.windowClick(0, chestSlot, 0, ClickType.QUICK_MOVE, mc.player);
        mc.playerController.windowClick(0, slot, 0, ClickType.QUICK_MOVE, mc.player);
        return true;
    }

    public static boolean useItem(Item item) {
        if (mc.player == null || mc.world == null || mc.playerController == null) return false;

        if (mc.player.getHeldItemOffhand().getItem() == item) {
            mc.playerController.processRightClick(mc.player, mc.world, Hand.OFF_HAND);
            return true;
        }

        int hotbarSlot = findHotbarItem(item);
        int oldSlot = mc.player.inventory.currentItem;

        if (hotbarSlot != -1) {
            mc.player.inventory.currentItem = hotbarSlot;
            mc.playerController.syncCurrentPlayItem();
            mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
            mc.player.inventory.currentItem = oldSlot;
            mc.playerController.syncCurrentPlayItem();
            return true;
        }

        int invSlot = findItem(item);
        if (invSlot == -1) {
            return false;
        }

        int targetHotbar = findBestSlotInHotBar();
        if (targetHotbar == -1) {
            return false;
        }

        mc.playerController.windowClick(0, toContainerSlot(invSlot), targetHotbar, ClickType.SWAP, mc.player);
        mc.player.inventory.currentItem = targetHotbar;
        mc.playerController.syncCurrentPlayItem();
        mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
        mc.player.inventory.currentItem = oldSlot;
        mc.playerController.syncCurrentPlayItem();
        mc.playerController.windowClick(0, toContainerSlot(invSlot), targetHotbar, ClickType.SWAP, mc.player);
        return true;
    }
}
