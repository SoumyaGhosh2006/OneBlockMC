package me.soumya.oneblock;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {

    private static int blocksBroken = 0;

    private static final Location ONE_BLOCK_LOC =
            new Location(Bukkit.getWorld("world"), 0, 65, 0);

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {

        Block block = e.getBlock();

        // Only allow breaking the main block
        if (!block.getLocation().equals(ONE_BLOCK_LOC)) return;

        e.setDropItems(false);

        blocksBroken++;

        // Give random overworld drop
        giveRandomDrop(e.getPlayer());

        // Replace block instantly
        Bukkit.getScheduler().runTaskLater(
                OneBlockPlugin.getInstance(),
                () -> block.setType(Material.STONE),
                1L
        );

        // Every 500 blocks → give frame
        if (blocksBroken % 500 == 0) {
            ChestManager.addFrame();
            Bukkit.broadcastMessage("§6+1 End Portal Frame added to Team Chest!");
        }
    }

    private void giveRandomDrop(org.bukkit.entity.Player player) {

        Material[] drops = {
                Material.DIRT,
                Material.STONE,
                Material.OAK_LOG,
                Material.IRON_ORE,
                Material.COAL,
                Material.WATER_BUCKET,
                Material.LAVA_BUCKET
        };

        Material random = drops[(int) (Math.random() * drops.length)];

        player.getInventory().addItem(new ItemStack(random));
    }
}