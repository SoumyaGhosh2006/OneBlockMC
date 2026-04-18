package me.soumya.oneblock;

/**
 * Defines the OneBlock progression phases.
 * Each phase has a start block count and a display name.
 * The block face shown is the NEXT phase's representative block.
 */
public enum Phase {

    // Phase name,         start block,  display name
    PLAINS              (0,     "§a☘ Plains"),
    FOREST              (100,   "§2🌲 Forest"),
    UNDERGROUND         (300,   "§8⛏ Underground"),
    OCEAN               (600,   "§b🌊 Ocean"),
    JUNGLE_SWAMP        (900,   "§2🌴 Jungle & Swamp"),
    DESERT              (1200,  "§e🏜 Desert"),
    DEEP_UNDERGROUND    (1500,  "§7💎 Deep Underground"),
    ENDGAME             (2000,  "§6⚡ Endgame");

    private final int startBlock;
    private final String displayName;

    Phase(int startBlock, String displayName) {
        this.startBlock = startBlock;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the current phase based on how many blocks have been broken.
     */
    public static Phase getCurrent(int blocksBroken) {
        Phase current = PLAINS;
        for (Phase phase : values()) {
            if (blocksBroken >= phase.startBlock) {
                current = phase;
            }
        }
        return current;
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