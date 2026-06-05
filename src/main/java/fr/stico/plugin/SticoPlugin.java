package fr.stico.plugin;

import fr.stico.plugin.commands.SticoCommand;
import fr.stico.plugin.listeners.PlayerListener;
import fr.stico.plugin.managers.ColorManager;
import fr.stico.plugin.managers.LocatorBarManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SticoPlugin extends JavaPlugin {

    private static SticoPlugin instance;
    private LocatorBarManager locatorBarManager;
    private ColorManager colorManager;

    @Override
    public void onEnable() {
        instance = this;

        // Sauvegarde config par défaut
        saveDefaultConfig();

        // Initialisation des managers
        colorManager = new ColorManager(this);
        locatorBarManager = new LocatorBarManager(this);

        // Enregistrement des listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        // Enregistrement des commandes
        SticoCommand cmd = new SticoCommand(this);
        getCommand("stico").setExecutor(cmd);
        getCommand("stico").setTabCompleter(cmd);

        // Démarrage de la tâche de mise à jour
        locatorBarManager.startTask();

        getLogger().info("╔══════════════════════════════╗");
        getLogger().info("║   STICO v" + getDescription().getVersion() + " activé !        ║");
        getLogger().info("║   Locator Bar pour 1.21      ║");
        getLogger().info("╚══════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (locatorBarManager != null) {
            locatorBarManager.stopTask();
            locatorBarManager.restoreAllXpBars();
        }
        getLogger().info("[STICO] Plugin désactivé. Barres XP restaurées.");
    }

    public static SticoPlugin getInstance() {
        return instance;
    }

    public LocatorBarManager getLocatorBarManager() {
        return locatorBarManager;
    }

    public ColorManager getColorManager() {
        return colorManager;
    }
}
