package me.soumya.oneblock;

import org.bukkit.plugin.java.JavaPlugin;

public class OneBlockPlugin extends JavaPlugin {

    private static OneBlockPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getLogger().info("§aOneBlock Plugin Enabled! Good luck beating the game!");
    }

    @Override
    public void onDisable() {
        getLogger().info("OneBlock Plugin Disabled.");
    }

    public static OneBlockPlugin getInstance() {
        return instance;
    }
}