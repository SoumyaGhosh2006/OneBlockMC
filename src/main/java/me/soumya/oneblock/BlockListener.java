package me.soumya.oneblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockListener implements Listener {

    // Persists across breaks
    private static int blocksBroken = 0;
    
    // Queue system: next N blocks to spawn
    private static final Queue<Material> blockQueue = new LinkedList<>();
    private static final Random random = new Random();

    // Oneblock is fixed at 0, 65, 0 in world "world"
    private static final int OB_X = 0;
    private static final int OB_Y = 65;
    private static final int OB_Z = 0;

    // ==============================
    // CORE EVENT
    // ==============================

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Only care about the exact oneblock
        if (!block.getWorld().getName().equals("world")) return;
        if (block.getX() != OB_X || block.getY() != OB_Y || block.getZ() != OB_Z) return;

        event.setDropItems(false); // We handle drops manually

        Player player = event.getPlayer();
        blocksBroken++;

        Phase currentPhase = Phase.getCurrent(blocksBroken);
        Phase previousPhase = Phase.getCurrent(blocksBroken - 1);

        // Announce phase change
        if (currentPhase != previousPhase) {
            announcePhaseChange(currentPhase);
            blockQueue.clear(); // Reset queue on phase change
        }

        // Handle the drop (20% chance mob, 80% item)
        if (random.nextDouble() < getMobChance(currentPhase) && blocksBroken > 50) {
            spawnPhaseMob(block.getLocation(), currentPhase);
        } else {
            givePhaseItem(player, currentPhase);
        }

        // Milestone: every 500 blocks → spawn a chest with an End Portal Frame
        if (blocksBroken % 500 == 0) {
            ChestManager.spawnFrameChest(blocksBroken);
        }

        // Regenerate the block 1 tick later
        // Block face = NEXT item in queue (preview system)
        Bukkit.getScheduler().runTaskLater(
                OneBlockPlugin.getInstance(),
                () -> {
                    Material nextBlock = getNextBlock(currentPhase);
                    block.setType(nextBlock);
                },
                1L
        );

        // HUD: show progress to player
        int remaining = 500 - (blocksBroken % 500);
        player.sendMessage("§7[§6OneBlock§7] §e#" + blocksBroken
                + " §7| " + currentPhase.getDisplayName()
                + " §7| Frame in: §e" + remaining);
    }

    // ==============================
    // BLOCK QUEUE SYSTEM
    // ==============================

    /**
     * Gets the next block to spawn.
     * Maintains a queue so the block face always shows what's coming next.
     */
    private Material getNextBlock(Phase phase) {
        // Refill queue if empty
        if (blockQueue.isEmpty()) {
            fillBlockQueue(phase);
        }
        
        Material next = blockQueue.poll();
        if (next == null) {
            next = Material.GRASS_BLOCK; // fallback
        }
        
        // Always keep queue filled
        if (blockQueue.size() < 3) {
            fillBlockQueue(phase);
        }
        
        return next;
    }

    /**
     * Fills the block queue with 10-20 random blocks from the current phase.
     */
    private void fillBlockQueue(Phase phase) {
        List<Material> phaseBlocks = getPhaseBlocks(phase);
        int count = 10 + random.nextInt(11); // 10-20 blocks
        
        for (int i = 0; i < count; i++) {
            blockQueue.add(phaseBlocks.get(random.nextInt(phaseBlocks.size())));
        }
    }

    // ==============================
    // MOB SPAWNING
    // ==============================

    private double getMobChance(Phase phase) {
        return switch (phase) {
            case PLAINS       -> 0.12;
            case UNDERGROUND  -> 0.20;
            case SNOW         -> 0.18;
            case OCEAN        -> 0.15;
            case JUNGLE       -> 0.22;
            case SWAMP        -> 0.24;
            case DESERT       -> 0.20;
            case RED_DESERT   -> 0.22;
            case DESOLATE     -> 0.28;
        };
    }

    private void spawnPhaseMob(Location loc, Phase phase) {
        Map<EntityType, Integer> pool = new LinkedHashMap<>();

        switch (phase) {
            case PLAINS -> {
                pool.put(EntityType.COW,     30);
                pool.put(EntityType.SHEEP,   25);
                pool.put(EntityType.PIG,     25);
                pool.put(EntityType.CHICKEN, 20);
                pool.put(EntityType.RABBIT,  15);
            }

            case UNDERGROUND -> {
                pool.put(EntityType.ZOMBIE,   25);
                pool.put(EntityType.SKELETON, 20);
                pool.put(EntityType.SPIDER,   18);
                pool.put(EntityType.CREEPER,  15);
                pool.put(EntityType.CAVE_SPIDER, 10);
                pool.put(EntityType.BAT,      12);
            }

            case SNOW -> {
                pool.put(EntityType.POLAR_BEAR, 15);
                pool.put(EntityType.RABBIT,  20);
                pool.put(EntityType.FOX,     18);
                pool.put(EntityType.STRAY,   15); // snowy skeleton variant
                pool.put(EntityType.WOLF,    12);
                pool.put(Entity Type.SKELETON, 10);
            }

            case OCEAN -> {
                pool.put(EntityType.COD,      20);
                pool.put(EntityType.SALMON,   18);
                pool.put(EntityType.SQUID,    15);
                pool.put(EntityType.DOLPHIN,  12);
                pool.put(EntityType.TURTLE,   10);
                pool.put(EntityType.DROWNED,  15);
                pool.put(EntityType.GUARDIAN,  8);
                pool.put(EntityType.TROPICAL_FISH, 12);
                pool.put(EntityType.GLOW_SQUID, 8);
            }

            case JUNGLE -> {
                pool.put(EntityType.PARROT,   18);
                pool.put(EntityType.OCELOT,   15);
                pool.put(EntityType.PANDA,    12);
                pool.put(EntityType.BEE,      15);
                pool.put(EntityType.SPIDER,   12);
                pool.put(EntityType.ZOMBIE,   10);
                pool.put(EntityType.CHICKEN,  8);
            }

            case SWAMP -> {
                pool.put(EntityType.SLIME,    18);
                pool.put(EntityType.WITCH,    15);
                pool.put(EntityType.FROG,     15);
                pool.put(EntityType.ZOMBIE,   12);
                pool.put(EntityType.SKELETON, 10);
                pool.put(EntityType.SPIDER,   10);
            }

            case DESERT -> {
                pool.put(EntityType.HUSK,     20); // desert zombie
                pool.put(EntityType.SKELETON, 15);
                pool.put(EntityType.RABBIT,   15);
                pool.put(EntityType.PILLAGER, 12);
                pool.put(EntityType.CAMEL,    10);
                pool.put(EntityType.LLAMA,    8);
            }

            case RED_DESERT -> {
                pool.put(EntityType.HUSK,     18);
                pool.put(EntityType.SKELETON, 15);
                pool.put(EntityType.PILLAGER, 15);
                pool.put(EntityType.RAVAGER,  8);
                pool.put(EntityType.WITCH,    10);
                pool.put(EntityType.VINDICATOR, 8);
            }

            case DESOLATE -> {
                pool.put(EntityType.ZOMBIE,     15);
                pool.put(EntityType.SKELETON,   15);
                pool.put(EntityType.CREEPER,    12);
                pool.put(EntityType.BREEZE,     10); // 1.21
                pool.put(EntityType.BOGGED,     10); // 1.21
                pool.put(EntityType.ENDERMAN,   5);  // rare
                pool.put(EntityType.WITCH,      8);
                pool.put(EntityType.PILLAGER,   8);
                pool.put(EntityType.RAVAGER,    5);
            }
        }

        if (pool.isEmpty()) return;

        EntityType selected = getWeightedRandom(pool);
        loc.getWorld().spawnEntity(loc.clone().add(0.5, 1, 0.5), selected);
    }

    // ==============================
    // ITEM DROPS
    // ==============================

    private void givePhaseItem(Player player, Phase phase) {
        List<Material> items = getPhaseBlocks(phase);
        Material selected = items.get(random.nextInt(items.size()));
        
        Map<Integer, ItemStack> leftover =
                player.getInventory().addItem(new ItemStack(selected));

        // Drop overflow at player's feet
        leftover.values().forEach(item ->
                player.getWorld().dropItem(player.getLocation(), item)
        );
    }

    /**
     * Returns the weighted block pool for each phase.
     * Mimics the datapack's loot distribution.
     */
    private List<Material> getPhaseBlocks(Phase phase) {
        List<Material> blocks = new ArrayList<>();

        switch (phase) {
            case PLAINS -> {
                addWeighted(blocks, Material.GRASS_BLOCK, 30);
                addWeighted(blocks, Material.DIRT, 25);
                addWeighted(blocks, Material.OAK_LOG, 20);
                addWeighted(blocks, Material.OAK_LEAVES, 15);
                addWeighted(blocks, Material.OAK_SAPLING, 12);
                addWeighted(blocks, Material.WHEAT_SEEDS, 15);
                addWeighted(blocks, Material.WHEAT, 10);
                addWeighted(blocks, Material.TALL_GRASS, 10);
                addWeighted(blocks, Material.DANDELION, 8);
                addWeighted(blocks, Material.POPPY, 8);
                addWeighted(blocks, Material.GRAVEL, 10);
                addWeighted(blocks, Material.SAND, 8);
                addWeighted(blocks, Material.CLAY, 6);
            }

            case UNDERGROUND -> {
                addWeighted(blocks, Material.STONE, 30);
                addWeighted(blocks, Material.COBBLESTONE, 25);
                addWeighted(blocks, Material.ANDESITE, 15);
                addWeighted(blocks, Material.DIORITE, 15);
                addWeighted(blocks, Material.GRANITE, 15);
                addWeighted(blocks, Material.GRAVEL, 12);
                addWeighted(blocks, Material.COAL_ORE, 18);
                addWeighted(blocks, Material.IRON_ORE, 12);
                addWeighted(blocks, Material.COPPER_ORE, 10);
                addWeighted(blocks, Material.GOLD_ORE, 5);
                addWeighted(blocks, Material.REDSTONE_ORE, 6);
                addWeighted(blocks, Material.TUFF, 8);
                addWeighted(blocks, Material.CALCITE, 6);
                addWeighted(blocks, Material.COBWEB, 4);
            }

            case SNOW -> {
                addWeighted(blocks, Material.SNOW_BLOCK, 25);
                addWeighted(blocks, Material.ICE, 20);
                addWeighted(blocks, Material.PACKED_ICE, 12);
                addWeighted(blocks, Material.BLUE_ICE, 6);
                addWeighted(blocks, Material.SPRUCE_LOG, 18);
                addWeighted(blocks, Material.SPRUCE_SAPLING, 10);
                addWeighted(blocks, Material.POWDER_SNOW, 15);
                addWeighted(blocks, Material.STONE, 15);
                addWeighted(blocks, Material.COAL_ORE, 10);
                addWeighted(blocks, Material.IRON_ORE, 8);
            }

            case OCEAN -> {
                addWeighted(blocks, Material.PRISMARINE, 20);
                addWeighted(blocks, Material.PRISMARINE_BRICKS, 12);
                addWeighted(blocks, Material.DARK_PRISMARINE, 8);
                addWeighted(blocks, Material.SEA_LANTERN, 6);
                addWeighted(blocks, Material.KELP, 18);
                addWeighted(blocks, Material.SEAGRASS, 15);
                addWeighted(blocks, Material.SAND, 20);
                addWeighted(blocks, Material.GRAVEL, 15);
                addWeighted(blocks, Material.CLAY, 10);
                addWeighted(blocks, Material.SPONGE, 3);
                addWeighted(blocks, Material.CORAL_BLOCK, 8);
                addWeighted(blocks, Material.TUBE_CORAL, 6);
            }

            case JUNGLE -> {
                addWeighted(blocks, Material.JUNGLE_LOG, 25);
                addWeighted(blocks, Material.JUNGLE_LEAVES, 20);
                addWeighted(blocks, Material.JUNGLE_SAPLING, 12);
                addWeighted(blocks, Material.BAMBOO, 20);
                addWeighted(blocks, Material.MELON, 12);
                addWeighted(blocks, Material.COCOA_BEANS, 10);
                addWeighted(blocks, Material.MOSS_BLOCK, 15);
                addWeighted(blocks, Material.VINE, 12);
                addWeighted(blocks, Material.PODZOL, 10);
                addWeighted(blocks, Material.GLOW_BERRIES, 8);
            }

            case SWAMP -> {
                addWeighted(blocks, Material.MUD, 20);
                addWeighted(blocks, Material.MUD_BRICKS, 12);
                addWeighted(blocks, Material.MANGROVE_LOG, 18);
                addWeighted(blocks, Material.MANGROVE_ROOTS, 12);
                addWeighted(blocks, Material.MANGROVE_SAPLING, 8);
                addWeighted(blocks, Material.LILY_PAD, 10);
                addWeighted(blocks, Material.SLIME_BLOCK, 6);
                addWeighted(blocks, Material.CLAY, 15);
                addWeighted(blocks, Material.MOSS_BLOCK, 12);
                addWeighted(blocks, Material.BROWN_MUSHROOM, 8);
                addWeighted(blocks, Material.RED_MUSHROOM, 8);
            }

            case DESERT -> {
                addWeighted(blocks, Material.SAND, 30);
                addWeighted(blocks, Material.SANDSTONE, 20);
                addWeighted(blocks, Material.SMOOTH_SANDSTONE, 12);
                addWeighted(blocks, Material.CACTUS, 10);
                addWeighted(blocks, Material.DEAD_BUSH, 8);
                addWeighted(blocks, Material.TERRACOTTA, 15);
                addWeighted(blocks, Material.BONE_BLOCK, 8);
                addWeighted(blocks, Material.SUSPICIOUS_SAND, 3);
                addWeighted(blocks, Material.GOLD_ORE, 6);
            }

            case RED_DESERT -> {
                addWeighted(blocks, Material.RED_SAND, 30);
                addWeighted(blocks, Material.RED_SANDSTONE, 20);
                addWeighted(blocks, Material.TERRACOTTA, 18);
                addWeighted(blocks, Material.ORANGE_TERRACOTTA, 12);
                addWeighted(blocks, Material.RED_TERRACOTTA, 12);
                addWeighted(blocks, Material.YELLOW_TERRACOTTA, 10);
                addWeighted(blocks, Material.CACTUS, 8);
                addWeighted(blocks, Material.DEAD_BUSH, 6);
                addWeighted(blocks, Material.GOLD_ORE, 8);
            }

            case DESOLATE -> {
                addWeighted(blocks, Material.DEEPSLATE, 20);
                addWeighted(blocks, Material.COBBLED_DEEPSLATE, 18);
                addWeighted(blocks, Material.TUFF, 15);
                addWeighted(blocks, Material.OBSIDIAN, 10);
                addWeighted(blocks, Material.DEEPSLATE_DIAMOND_ORE, 3);
                addWeighted(blocks, Material.DEEPSLATE_EMERALD_ORE, 2);
                addWeighted(blocks, Material.AMETHYST_BLOCK, 6);
                addWeighted(blocks, Material.REDSTONE_BLOCK, 5);
                addWeighted(blocks, Material.DIAMOND_ORE, 4);
                addWeighted(blocks, Material.EMERALD_ORE, 2);
                addWeighted(blocks, Material.BEACON, 1);
                addWeighted(blocks, Material.ENCHANTING_TABLE, 2);
            }
        }

        return blocks;
    }

    /**
     * Helper to add N copies of a material to a list (for weighted randomness).
     */
    private void addWeighted(List<Material> list, Material mat, int weight) {
        for (int i = 0; i < weight; i++) {
            list.add(mat);
        }
    }

    // ==============================
    // PHASE CHANGE ANNOUNCEMENT
    // ==============================

    private void announcePhaseChange(Phase newPhase) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("  §e§lPHASE UNLOCKED: " + newPhase.getDisplayName());
        Bukkit.broadcastMessage("  §7New blocks and mobs incoming!");
        Bukkit.broadcastMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
        Bukkit.getOnlinePlayers().forEach(p ->
                p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f)
        );
    }

    // ==============================
    // WEIGHTED RANDOM
    // ==============================

    private <T> T getWeightedRandom(Map<T, Integer> map) {
        int total = map.values().stream().mapToInt(Integer::intValue).sum();
        int roll = random.nextInt(total);
        int cumulative = 0;
        for (Map.Entry<T, Integer> entry : map.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) return entry.getKey();
        }
        return map.keySet().iterator().next();
    }
}