package fr.stico.plugin.listeners;

import fr.stico.plugin.SticoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final SticoPlugin plugin;

    public PlayerListener(SticoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("join-message", true)) {
            event.getPlayer().sendMessage(
                Component.text("[", NamedTextColor.DARK_GRAY)
                    .append(Component.text("STICO", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Locator Bar activée ! ", NamedTextColor.GRAY))
                    .append(Component.text("/stico toggle", NamedTextColor.YELLOW))
                    .append(Component.text(" pour désactiver.", NamedTextColor.GRAY))
            );
        }
        plugin.getLocatorBarManager().updateBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLocatorBarManager().cleanupPlayer(event.getPlayer().getUniqueId());
        plugin.getColorManager().removePlayerColor(event.getPlayer().getUniqueId());
    }

    /**
     * Quand un joueur gagne de l'XP → masque la barre locator pendant 5s
     * (comportement identique à la 1.21.6)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onExpChange(PlayerExpChangeEvent event) {
        if (event.getAmount() != 0) {
            plugin.getLocatorBarManager().triggerXpCooldown(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelChange(PlayerLevelChangeEvent event) {
        plugin.getLocatorBarManager().triggerXpCooldown(event.getPlayer());
    }
}
