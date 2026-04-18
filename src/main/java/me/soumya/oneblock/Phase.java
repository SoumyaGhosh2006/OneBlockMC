package me.soumya.oneblock;

/**
 * Defines the 9 OneBlock progression phases.
 * Each phase has a start block count, max blocks, and display name.
 * Matches the datapack's phase structure.
 */
public enum Phase {

    // Phase name         start, max,   display name
    PLAINS              (0,     150,   "§a☘ Phase 1: Plains"),
    UNDERGROUND         (150,   300,   "§8⛏ Phase 2: Underground"),
    SNOW                (300,   450,   "§f❄ Phase 3: Snowy Tundra"),
    OCEAN               (450,   600,   "§b🌊 Phase 4: Ocean"),
    JUNGLE              (600,   750,   "§2🌴 Phase 5: Jungle"),
    SWAMP               (750,   900,   "§2🐸 Phase 6: Swamp"),
    DESERT              (900,   1050,  "§e🏜 Phase 7: Desert"),
    RED_DESERT          (1050,  1200,  "§c🏜 Phase 8: Red Desert"),
    DESOLATE            (1200,  9999999, "§7💀 Phase 9: Desolate");

    private final int startBlock;
    private final int maxBlock;
    private final String displayName;

    Phase(int startBlock, int maxBlock, String displayName) {
        this.startBlock = startBlock;
        this.maxBlock = maxBlock;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStartBlock() {
        return startBlock;
    }

    public int getMaxBlock() {
        return maxBlock;
    }

    /**
     * Returns the current phase based on how many blocks have been broken.
     */
    public static Phase getCurrent(int blocksBroken) {
        for (Phase phase : values()) {
            if (blocksBroken >= phase.startBlock && blocksBroken < phase.maxBlock) {
                return phase;
            }
        }
        return DESOLATE; // fallback to last phase
    }

    /**
     * Returns the NEXT phase, or null if already at last phase.
     */
    public static Phase getNext(int blocksBroken) {
        Phase[] phases = values();
        Phase current = getCurrent(blocksBroken);
        int idx = current.ordinal();
        return (idx + 1 < phases.length) ? phases[idx + 1] : null;
    }
}