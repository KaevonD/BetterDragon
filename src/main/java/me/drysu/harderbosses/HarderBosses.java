package me.drysu.harderbosses;

import org.bukkit.plugin.java.JavaPlugin;

public final class HarderBosses extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new MyListener(this), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
