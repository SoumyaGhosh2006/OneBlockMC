package me.soumya.oneblock;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChestManager {

    private static final Location CHEST_LOC =
            new Location(Bukkit.getWorld("world"), 2, 65, 0);

    public static void createChest() {
        Block block = CHEST_LOC.getBlock();
        block.setType(Material.CHEST);

        Chest chest = (Chest) block.getState();
        chest.setCustomName("§6End Progress Chest");
        chest.update();
    }

    public static void addFrame() {
        Block block = CHEST_LOC.getBlock();

        if (!(block.getState() instanceof Chest)) {
            createChest();
        }

        Chest chest = (Chest) block.getState();
        Inventory inv = chest.getInventory();

        ItemStack frame = new ItemStack(Material.END_PORTAL_FRAME);

        if (inv.firstEmpty() != -1) {
            inv.addItem(frame);
        } else {
            CHEST_LOC.getWorld().dropItem(CHEST_LOC, frame);
        }
    }
}