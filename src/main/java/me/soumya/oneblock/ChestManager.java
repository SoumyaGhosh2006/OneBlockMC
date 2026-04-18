package me.soumya.oneblock;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ChestManager {

    // Chest spawns ON TOP of the oneblock (y+1) so it doesn't replace it
    private static Location getChestLocation() {
        return new Location(Bukkit.getWorld("world"), 0, 66, 0);
    }

    /**
     * Spawns a chest on top of the oneblock and puts 1 End Portal Frame inside.
     * Called every 500 blocks broken.
     */
    public static void spawnFrameChest(int totalBroken) {
        Location loc = getChestLocation();
        if (loc.getWorld() == null) return;

        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        // Slight delay to let the block state settle
        Bukkit.getScheduler().runTaskLater(OneBlockPlugin.getInstance(), () -> {
            if (!(block.getState() instanceof Chest)) return;

            Chest chest = (Chest) block.getState();
            chest.setCustomName("§6§lEnd Portal Frame §7(§e" + (totalBroken / 500) + "§7/§e12§7)");
            chest.update();

            Inventory inv = chest.getInventory();

            // Build a named End Portal Frame so it's obvious
            ItemStack frame = new ItemStack(Material.END_PORTAL_FRAME);
            ItemMeta meta = frame.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b✦ End Portal Frame");
                meta.setLore(Arrays.asList(
                        "§7Milestone: §e" + totalBroken + " §7blocks broken",
                        "§7Frames collected: §e" + (totalBroken / 500) + "§7/§e12",
                        "§7Keep going! You're " + getPercentage(totalBroken) + " done!"
                ));
                frame.setItemMeta(meta);
            }

            inv.addItem(frame);

        }, 2L);

        // Broadcast + sound
        int framesEarned = totalBroken / 500;
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l» §e§lMILESTONE REACHED! §6§l«");
        Bukkit.broadcastMessage("§7  Blocks broken: §e" + totalBroken);
        Bukkit.broadcastMessage("§7  End Portal Frames: §b" + framesEarned + " §7/ §b12");
        Bukkit.broadcastMessage("§7  A chest has appeared above the OneBlock!");
        Bukkit.broadcastMessage("");

        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.9f)
        );
    }

    private static String getPercentage(int totalBroken) {
        // 12 frames needed, one per 500 = 6000 blocks for all frames
        int pct = Math.min(100, (totalBroken * 100) / 6000);
        return pct + "%";
    }
}