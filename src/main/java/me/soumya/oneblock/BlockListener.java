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

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockListener implements Listener {

    // Persists across breaks — counts total blocks broken on the oneblock
    private static int blocksBroken = 0;

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
        }

        // Handle the drop (item or mob)
        handleDrop(player, block.getLocation(), currentPhase);

        // Milestone: every 500 blocks → spawn a chest with an End Portal Frame
        if (blocksBroken % 500 == 0) {
            ChestManager.spawnFrameChest(blocksBroken);
        }

        // Regenerate the block 1 tick later
        // Face = next phase's representative block (preview of what's coming)
        final Phase nextPhase = Phase.getNext(blocksBroken);
        Bukkit.getScheduler().runTaskLater(
                OneBlockPlugin.getInstance(),
                () -> block.setType(getPhaceBlock(nextPhase != null ? nextPhase : currentPhase)),
                1L
        );

        // HUD: show progress to player
        player.sendMessage("§7[OneBlock] §eBlock #" + blocksBroken
                + " §7| Phase: " + currentPhase.getDisplayName()
                + " §7| Next frame in: §e" + (500 - (blocksBroken % 500)) + " §7blocks");
    }

    // ==============================
    // ONEBLOCK FACE (NEXT PHASE PREVIEW)
    // ==============================

    /**
     * Returns the representative block for a given phase.
     * This is what the oneblock looks like while you're in the CURRENT phase
     * (previewing the NEXT phase's theme).
     */
    private Material getPhaceBlock(Phase phase) {
        return switch (phase) {
            case PLAINS           -> Material.GRASS_BLOCK;
            case FOREST           -> Material.OAK_LOG;
            case UNDERGROUND      -> Material.STONE;
            case OCEAN            -> Material.PRISMARINE;
            case JUNGLE_SWAMP     -> Material.JUNGLE_LOG;
            case DESERT           -> Material.SANDSTONE;
            case DEEP_UNDERGROUND -> Material.DEEPSLATE;
            case ENDGAME          -> Material.TUFF; // dark stone, no end/nether
        };
    }

    // ==============================
    // DROP SYSTEM
    // ==============================

    private void handleDrop(Player player, Location loc, Phase phase) {
        // No mobs in the very first 50 blocks — settle in first
        double mobChance = blocksBroken < 50 ? 0.0 : getMobChance(phase);

        if (Math.random() < mobChance) {
            spawnPhaseMob(loc, phase);
        } else {
            givePhaseItem(player, phase);
        }
    }

    /**
     * Mob spawn chance scales with phase — later phases are more dangerous.
     */
    private double getMobChance(Phase phase) {
        return switch (phase) {
            case PLAINS           -> 0.10; // 10% — just passive mobs
            case FOREST           -> 0.15;
            case UNDERGROUND      -> 0.20;
            case OCEAN            -> 0.18;
            case JUNGLE_SWAMP     -> 0.20;
            case DESERT           -> 0.22;
            case DEEP_UNDERGROUND -> 0.25;
            case ENDGAME          -> 0.28;
        };
    }

    // ==============================
    // MOB SPAWNING (NO NETHER / END MOBS)
    // Only Enderman allowed from end, rarely, after block 800
    // ==============================

    private void spawnPhaseMob(Location loc, Phase phase) {
        Map<EntityType, Integer> pool = new LinkedHashMap<>();

        switch (phase) {
            case PLAINS:
                // Only peaceful/passive
                pool.put(EntityType.COW,     30);
                pool.put(EntityType.SHEEP,   25);
                pool.put(EntityType.PIG,     25);
                pool.put(EntityType.CHICKEN, 20);
                break;

            case FOREST:
                pool.put(EntityType.COW,     20);
                pool.put(EntityType.SHEEP,   15);
                pool.put(EntityType.PIG,     15);
                pool.put(EntityType.CHICKEN, 15);
                pool.put(EntityType.WOLF,    10); // neutral
                pool.put(EntityType.BEE,     10);
                pool.put(EntityType.FOX,      8);
                pool.put(EntityType.RABBIT,   7);
                // First hostiles
                pool.put(EntityType.ZOMBIE,   6);
                pool.put(EntityType.SKELETON, 5);
                pool.put(EntityType.SPIDER,   5);
                break;

            case UNDERGROUND:
                pool.put(EntityType.ZOMBIE,   20);
                pool.put(EntityType.SKELETON, 18);
                pool.put(EntityType.SPIDER,   15);
                pool.put(EntityType.CREEPER,  12);
                pool.put(EntityType.CAVE_SPIDER, 8);
                pool.put(EntityType.BAT,      10);
                pool.put(EntityType.COW,       8);
                pool.put(EntityType.SHEEP,     5);
                break;

            case OCEAN:
                pool.put(EntityType.COD,      20);
                pool.put(EntityType.SALMON,   18);
                pool.put(EntityType.SQUID,    15);
                pool.put(EntityType.DOLPHIN,  12);
                pool.put(EntityType.TURTLE,   10);
                pool.put(EntityType.DROWNED,  15); // aquatic hostile
                pool.put(EntityType.GUARDIAN,  8);
                pool.put(EntityType.TROPICAL_FISH, 10);
                break;

            case JUNGLE_SWAMP:
                pool.put(EntityType.PARROT,   15);
                pool.put(EntityType.OCELOT,   12);
                pool.put(EntityType.PANDA,    10);
                pool.put(EntityType.FROG,     12);
                pool.put(EntityType.SLIME,    10);
                pool.put(EntityType.WITCH,    10);
                pool.put(EntityType.ZOMBIE,   12);
                pool.put(EntityType.SPIDER,    8);
                pool.put(EntityType.BEE,      10);
                break;

            case DESERT:
                pool.put(EntityType.CAMEL,    12);
                pool.put(EntityType.RABBIT,   15);
                pool.put(EntityType.LLAMA,    12);
                pool.put(EntityType.ZOMBIE,   15);
                pool.put(EntityType.SKELETON, 15);
                pool.put(EntityType.PILLAGER, 10);
                pool.put(EntityType.WITCH,     8);
                pool.put(EntityType.HUSK,     13); // desert zombie variant
                break;

            case DEEP_UNDERGROUND:
                pool.put(EntityType.ZOMBIE,     15);
                pool.put(EntityType.SKELETON,   15);
                pool.put(EntityType.CREEPER,    12);
                pool.put(EntityType.CAVE_SPIDER, 10);
                pool.put(EntityType.BOGGED,     10); // 1.21 skeleton variant
                pool.put(EntityType.BREEZE,      8); // 1.21 — overworld trial mob
                // Enderman: rare but allowed
                pool.put(EntityType.ENDERMAN,    5);
                break;

            case ENDGAME:
                pool.put(EntityType.ZOMBIE,     12);
                pool.put(EntityType.SKELETON,   12);
                pool.put(EntityType.CREEPER,    10);
                pool.put(EntityType.PILLAGER,   10);
                pool.put(EntityType.WITCH,       8);
                pool.put(EntityType.RAVAGER,     5);
                pool.put(EntityType.BREEZE,      8);
                pool.put(EntityType.BOGGED,      8);
                pool.put(EntityType.ENDERMAN,    7); // still rare-ish
                pool.put(EntityType.IRON_GOLEM,  6); // friendly tank
                pool.put(EntityType.VILLAGER,    4);
                break;
        }

        if (pool.isEmpty()) return;

        EntityType selected = getRandom(pool);
        loc.getWorld().spawnEntity(loc.clone().add(0.5, 1, 0.5), selected);
    }

    // ==============================
    // ITEM DROPS (PHASE-BASED, NO NETHER/END)
    // ==============================

    private void givePhaseItem(Player player, Phase phase) {
        Map<Material, Integer> pool = new LinkedHashMap<>();

        switch (phase) {

            // ─── PLAINS ──────────────────────────────────────────────────────
            // Dirt, grass, seeds, basic wood — survival starter kit
            case PLAINS:
                pool.put(Material.DIRT,          40);
                pool.put(Material.GRASS_BLOCK,   30);
                pool.put(Material.COARSE_DIRT,   15);
                pool.put(Material.OAK_LOG,       25);
                pool.put(Material.OAK_LEAVES,    20);
                pool.put(Material.OAK_SAPLING,   18);
                pool.put(Material.WHEAT_SEEDS,   20);
                pool.put(Material.WHEAT,         15);
                pool.put(Material.CARROT,        10);
                pool.put(Material.POTATO,        10);
                pool.put(Material.BONE_MEAL,     12);
                pool.put(Material.PUMPKIN_SEEDS,  8);
                pool.put(Material.MELON_SEEDS,    8);
                pool.put(Material.GRAVEL,        10);
                pool.put(Material.FLINT,          8);
                pool.put(Material.APPLE,         10);
                pool.put(Material.HAY_BLOCK,      6);
                pool.put(Material.FLOWER_POT,     4);
                pool.put(Material.DANDELION,      8);
                pool.put(Material.POPPY,          8);
                break;

            // ─── FOREST ──────────────────────────────────────────────────────
            // All wood types, saplings, mushrooms, early crafting materials
            case FOREST:
                pool.put(Material.OAK_LOG,         25);
                pool.put(Material.BIRCH_LOG,        20);
                pool.put(Material.SPRUCE_LOG,       18);
                pool.put(Material.DARK_OAK_LOG,     12);
                pool.put(Material.CHERRY_LOG,       10);
                pool.put(Material.OAK_SAPLING,      15);
                pool.put(Material.BIRCH_SAPLING,    12);
                pool.put(Material.SPRUCE_SAPLING,   10);
                pool.put(Material.DARK_OAK_SAPLING,  8);
                pool.put(Material.CHERRY_SAPLING,    8);
                pool.put(Material.MUSHROOM_STEM,     8);
                pool.put(Material.RED_MUSHROOM,      8);
                pool.put(Material.BROWN_MUSHROOM,    8);
                pool.put(Material.PODZOL,           10);
                pool.put(Material.MOSS_BLOCK,       12);
                pool.put(Material.MOSSY_COBBLESTONE, 8);
                pool.put(Material.SWEET_BERRIES,    10);
                pool.put(Material.FERN,              8);
                pool.put(Material.LARGE_FERN,        6);
                pool.put(Material.VINE,              8);
                pool.put(Material.STICK,            10);
                pool.put(Material.LEATHER,           8); // from forest animals
                pool.put(Material.FEATHER,           8);
                pool.put(Material.STRING,            8);
                pool.put(Material.SPIDER_EYE,        5);
                pool.put(Material.BONE,              6);
                pool.put(Material.ARROW,             6);
                pool.put(Material.GUNPOWDER,         4);
                break;

            // ─── UNDERGROUND ─────────────────────────────────────────────────
            // Stone, ores, cave materials — no deep/rare yet
            case UNDERGROUND:
                pool.put(Material.STONE,         30);
                pool.put(Material.COBBLESTONE,   25);
                pool.put(Material.ANDESITE,      15);
                pool.put(Material.DIORITE,       15);
                pool.put(Material.GRANITE,       15);
                pool.put(Material.GRAVEL,        18);
                pool.put(Material.CLAY,          12);
                pool.put(Material.TUFF,          12);
                pool.put(Material.CALCITE,        8);
                pool.put(Material.COAL_ORE,      18);
                pool.put(Material.IRON_ORE,      14);
                pool.put(Material.COPPER_ORE,    12);
                pool.put(Material.GOLD_ORE,       6);
                pool.put(Material.REDSTONE_ORE,   8);
                pool.put(Material.COAL,          15);
                pool.put(Material.IRON_NUGGET,   10);
                pool.put(Material.RAW_IRON,       8);
                pool.put(Material.RAW_COPPER,     8);
                pool.put(Material.FLINT,         10);
                pool.put(Material.INFESTED_STONE, 4); // silverfish block, spicy
                pool.put(Material.STONE_BRICKS,  10);
                pool.put(Material.MOSSY_STONE_BRICKS, 6);
                pool.put(Material.CRACKED_STONE_BRICKS, 6);
                pool.put(Material.COBWEB,         5);
                pool.put(Material.BONE,           8);
                pool.put(Material.ROTTEN_FLESH,   6);
                pool.put(Material.GUNPOWDER,      5);
                pool.put(Material.ARROW,          6);
                break;

            // ─── OCEAN ───────────────────────────────────────────────────────
            // Sea blocks, fish loot, prismarine, ice biome
            case OCEAN:
                pool.put(Material.SAND,          25);
                pool.put(Material.GRAVEL,        15);
                pool.put(Material.CLAY,          12);
                pool.put(Material.PRISMARINE,    12);
                pool.put(Material.PRISMARINE_BRICKS, 8);
                pool.put(Material.DARK_PRISMARINE, 6);
                pool.put(Material.SEA_LANTERN,    6);
                pool.put(Material.PRISMARINE_SHARD, 10);
                pool.put(Material.PRISMARINE_CRYSTALS, 8);
                pool.put(Material.KELP,          15);
                pool.put(Material.SEAGRASS,      12);
                pool.put(Material.DRIED_KELP_BLOCK, 8);
                pool.put(Material.SPONGE,         4);
                pool.put(Material.WET_SPONGE,     4);
                pool.put(Material.COD,           12);
                pool.put(Material.SALMON,        10);
                pool.put(Material.TROPICAL_FISH,  6);
                pool.put(Material.PUFFERFISH,     4);
                pool.put(Material.INK_SAC,        8);
                pool.put(Material.GLOW_INK_SAC,   4);
                pool.put(Material.NAUTILUS_SHELL, 3);
                pool.put(Material.HEART_OF_THE_SEA, 1); // very rare
                pool.put(Material.SNOW_BLOCK,     8); // cold ocean variant
                pool.put(Material.ICE,            6);
                pool.put(Material.PACKED_ICE,     4);
                pool.put(Material.BLUE_ICE,       2);
                pool.put(Material.TUBE_CORAL_BLOCK, 6);
                break;

            // ─── JUNGLE & SWAMP ──────────────────────────────────────────────
            case JUNGLE_SWAMP:
                pool.put(Material.JUNGLE_LOG,    20);
                pool.put(Material.JUNGLE_LEAVES, 15);
                pool.put(Material.JUNGLE_SAPLING, 12);
                pool.put(Material.ACACIA_LOG,    12);
                pool.put(Material.ACACIA_SAPLING,  8);
                pool.put(Material.BAMBOO,        18);
                pool.put(Material.MELON,         10);
                pool.put(Material.MELON_SEEDS,    8);
                pool.put(Material.COCOA_BEANS,    8);
                pool.put(Material.GLOW_BERRIES,  10);
                pool.put(Material.MUD,           15);
                pool.put(Material.MUD_BRICKS,     8);
                pool.put(Material.MUDDY_MANGROVE_ROOTS, 8);
                pool.put(Material.MANGROVE_LOG,  12);
                pool.put(Material.MANGROVE_ROOTS, 8);
                pool.put(Material.MANGROVE_PROPAGULE, 8);
                pool.put(Material.SLIME_BLOCK,    5);
                pool.put(Material.SLIME_BALL,    10);
                pool.put(Material.LILY_PAD,       8);
                pool.put(Material.FROGSPAWN,      3);
                pool.put(Material.MOSS_BLOCK,    10);
                pool.put(Material.VINE,          10);
                pool.put(Material.SUGAR_CANE,    10);
                pool.put(Material.HONEY_BOTTLE,   6);
                pool.put(Material.HONEYCOMB,      6);
                pool.put(Material.SPIDER_EYE,     6);
                pool.put(Material.ROTTEN_FLESH,   5);
                break;

            // ─── DESERT ──────────────────────────────────────────────────────
            case DESERT:
                pool.put(Material.SAND,           30);
                pool.put(Material.RED_SAND,       15);
                pool.put(Material.SANDSTONE,      20);
                pool.put(Material.CHISELED_SANDSTONE, 8);
                pool.put(Material.SMOOTH_SANDSTONE, 10);
                pool.put(Material.RED_SANDSTONE,  10);
                pool.put(Material.TERRACOTTA,     15);
                pool.put(Material.WHITE_TERRACOTTA, 8);
                pool.put(Material.ORANGE_TERRACOTTA, 8);
                pool.put(Material.YELLOW_TERRACOTTA, 6);
                pool.put(Material.CACTUS,          8);
                pool.put(Material.DEAD_BUSH,       8);
                pool.put(Material.BONE_BLOCK,      8);
                pool.put(Material.SUSPICIOUS_SAND, 3); // archaeology
                pool.put(Material.ANGLER_POTTERY_SHERD, 2);
                pool.put(Material.ARCHER_POTTERY_SHERD, 2);
                pool.put(Material.GOLD_ORE,        8); // desert has more gold
                pool.put(Material.RAW_GOLD,        6);
                pool.put(Material.IRON_ORE,        8);
                pool.put(Material.COAL_ORE,        6);
                break;

            // ─── DEEP UNDERGROUND ────────────────────────────────────────────
            // Deepslate, rare ores, amethyst — big rewards
            case DEEP_UNDERGROUND:
                pool.put(Material.DEEPSLATE,          25);
                pool.put(Material.COBBLED_DEEPSLATE,  20);
                pool.put(Material.DEEPSLATE_TILES,    10);
                pool.put(Material.DEEPSLATE_BRICKS,   10);
                pool.put(Material.TUFF,               12);
                pool.put(Material.DRIPSTONE_BLOCK,    10);
                pool.put(Material.POINTED_DRIPSTONE,   8);
                pool.put(Material.ROOTED_DIRT,         8);
                pool.put(Material.MUD,                 8);
                pool.put(Material.DEEPSLATE_COAL_ORE, 12);
                pool.put(Material.DEEPSLATE_IRON_ORE, 10);
                pool.put(Material.DEEPSLATE_GOLD_ORE,  8);
                pool.put(Material.DEEPSLATE_REDSTONE_ORE, 8);
                pool.put(Material.DEEPSLATE_LAPIS_ORE, 6);
                pool.put(Material.DEEPSLATE_COPPER_ORE, 8);
                pool.put(Material.DEEPSLATE_EMERALD_ORE, 3);
                pool.put(Material.DEEPSLATE_DIAMOND_ORE, 2);
                pool.put(Material.AMETHYST_BLOCK,     6);
                pool.put(Material.BUDDING_AMETHYST,   2);
                pool.put(Material.AMETHYST_SHARD,    10);
                pool.put(Material.CALCITE,             6);
                pool.put(Material.RAW_IRON,            6);
                pool.put(Material.RAW_GOLD,            5);
                pool.put(Material.DIAMOND,             3);
                pool.put(Material.LAPIS_LAZULI,        6);
                pool.put(Material.REDSTONE,            8);
                break;

            // ─── ENDGAME ─────────────────────────────────────────────────────
            // Utility blocks, rare crafting items — no nether/end materials
            case ENDGAME:
                pool.put(Material.OBSIDIAN,          10);  // needs nether access but is overworld-obtainable
                pool.put(Material.CRYING_OBSIDIAN,    4);  // decorative, fine
                pool.put(Material.DIAMOND_ORE,        4);
                pool.put(Material.DIAMOND,            6);
                pool.put(Material.EMERALD,            5);
                pool.put(Material.GOLD_BLOCK,         4);
                pool.put(Material.IRON_BLOCK,         6);
                pool.put(Material.REDSTONE_BLOCK,     5);
                pool.put(Material.AMETHYST_BLOCK,     6);
                pool.put(Material.PISTON,             4);
                pool.put(Material.STICKY_PISTON,      3);
                pool.put(Material.OBSERVER,           3);
                pool.put(Material.DISPENSER,          3);
                pool.put(Material.DROPPER,            3);
                pool.put(Material.HOPPER,             3);
                pool.put(Material.COMPARATOR,         2);
                pool.put(Material.REPEATER,           3);
                pool.put(Material.ENCHANTING_TABLE,   1);
                pool.put(Material.BOOKSHELF,          5);
                pool.put(Material.ANVIL,              2);
                pool.put(Material.COPPER_BULB,        4); // 1.21
                pool.put(Material.CHISELED_COPPER,    4);
                pool.put(Material.TUFF_BRICKS,        5); // 1.21
                pool.put(Material.CHISELED_TUFF,      4);
                pool.put(Material.HEAVY_CORE,         1); // 1.21 mace component — very rare
                pool.put(Material.BEACON,             1); // legendary drop
                break;
        }

        if (pool.isEmpty()) return;

        Material selected = getRandom(pool);
        Map<Integer, ItemStack> leftover =
                player.getInventory().addItem(new ItemStack(selected));

        // Drop overflow at player's feet
        leftover.values().forEach(item ->
                player.getWorld().dropItem(player.getLocation(), item)
        );
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

    private <T> T getRandom(Map<T, Integer> map) {
        int total = map.values().stream().mapToInt(Integer::intValue).sum();
        int roll = (int) (Math.random() * total);
        int cumulative = 0;
        for (Map.Entry<T, Integer> entry : map.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) return entry.getKey();
        }
        return map.keySet().iterator().next();
    }
}
