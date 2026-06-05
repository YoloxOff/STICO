package fr.stico.plugin.managers;

import fr.stico.plugin.SticoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class LocatorBarManager {

    private final SticoPlugin plugin;
    private BukkitTask updateTask;

    private final Set<UUID> disabledPlayers = new HashSet<>();
    private final Map<UUID, Integer> savedLevels = new HashMap<>();
    private final Map<UUID, Float> savedExp = new HashMap<>();
    private final Map<UUID, Integer> xpCooldown = new HashMap<>();

    public LocatorBarManager(SticoPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        int interval = plugin.getConfig().getInt("update-interval", 4);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllBars, 0L, interval);
    }

    public void stopTask() {
        if (updateTask != null) updateTask.cancel();
    }

    private void updateAllBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateBar(player);
        }
        int interval = plugin.getConfig().getInt("update-interval", 4);
        xpCooldown.replaceAll((uuid, ticks) -> ticks - interval);
        xpCooldown.entrySet().removeIf(e -> e.getValue() <= 0);
    }

    public void updateBar(Player player) {
        UUID uuid = player.getUniqueId();

        if (!plugin.getConfig().getBoolean("enabled", true)) { restoreXpBar(player); return; }
        if (disabledPlayers.contains(uuid)) { restoreXpBar(player); return; }
        if (xpCooldown.containsKey(uuid)) { restoreXpBar(player); return; }

        List<Player> targets = getVisiblePlayers(player);

        if (targets.isEmpty()) { restoreXpBar(player); return; }

        // Sauvegarde XP réel
        savedLevels.put(uuid, player.getLevel());
        savedExp.put(uuid, player.getExp());

        renderLocatorBar(player, targets);
    }

    /**
     * Affichage via bossbar textuelle dans l'ActionBar ET level text.
     *
     * On utilise le LEVEL TEXT (composant Adventure) pour afficher
     * les points colorés sous forme de texte Minecraft simple :
     * chaque joueur = "● " ou "●↑" ou "●↓" en couleur.
     *
     * La barre XP verte est mise à 0 (invisible) pour laisser toute
     * l'attention sur les indicateurs textuels.
     */
    private void renderLocatorBar(Player viewer, List<Player> targets) {
        ColorManager colorManager = plugin.getColorManager();
        int verticalThreshold = plugin.getConfig().getInt("vertical-arrow-threshold", 10);

        // ── Construit le composant texte pour l'ActionBar ──────────────────
        // Format : "< [Nom] [Nom↑] [Nom↓] >"
        // On utilise uniquement des caractères ASCII-safe + couleurs Minecraft
        Component bar = Component.text("< ", NamedTextColor.DARK_GRAY);

        for (int i = 0; i < targets.size(); i++) {
            Player target = targets.get(i);
            TextColor color = colorManager.getPlayerColor(target);

            double yDiff = target.getLocation().getY() - viewer.getLocation().getY();
            String suffix = "";
            if (yDiff > verticalThreshold) suffix = "+";
            else if (yDiff < -verticalThreshold) suffix = "-";

            // Nom court (max 8 chars) + indicateur vertical
            String name = target.getName();
            if (name.length() > 8) name = name.substring(0, 7) + ".";

            Component entry = Component.text(name + suffix, color, TextDecoration.BOLD);
            bar = bar.append(entry);

            if (i < targets.size() - 1) {
                bar = bar.append(Component.text("  ", NamedTextColor.DARK_GRAY));
            }
        }

        bar = bar.append(Component.text(" >", NamedTextColor.DARK_GRAY));

        // ── Envoie dans l'ActionBar ────────────────────────────────────────
        viewer.sendActionBar(bar);

        // ── Barre XP : niveau = nb joueurs visibles, barre = vide ─────────
        viewer.setLevel(targets.size());
        viewer.setExp(0.0f);
    }

    private List<Player> getVisiblePlayers(Player viewer) {
        List<Player> result = new ArrayList<>();
        boolean sneakHides = plugin.getConfig().getBoolean("sneak-hides", true);
        boolean hideSpectators = plugin.getConfig().getBoolean("hide-spectators", true);
        boolean crossWorld = plugin.getConfig().getBoolean("cross-world", false);
        double maxDistance = plugin.getConfig().getDouble("max-distance", 0);
        World viewerWorld = viewer.getWorld();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer)) continue;
            if (!crossWorld && !target.getWorld().equals(viewerWorld)) continue;
            if (hideSpectators && target.getGameMode() == GameMode.SPECTATOR) continue;
            if (sneakHides && target.isSneaking()) continue;
            if (target.hasPermission("stico.bypass")) continue;
            if (maxDistance > 0 && target.getWorld().equals(viewerWorld)) {
                if (viewer.getLocation().distance(target.getLocation()) > maxDistance) continue;
            }
            result.add(target);
        }
        return result;
    }

    public void restoreXpBar(Player player) {
        UUID uuid = player.getUniqueId();
        if (savedLevels.containsKey(uuid)) {
            player.setLevel(savedLevels.get(uuid));
            player.setExp(savedExp.get(uuid));
            savedLevels.remove(uuid);
            savedExp.remove(uuid);
        }
    }

    public void restoreAllXpBars() {
        for (Player player : Bukkit.getOnlinePlayers()) restoreXpBar(player);
    }

    public void triggerXpCooldown(Player player) {
        xpCooldown.put(player.getUniqueId(), 100);
        restoreXpBar(player);
    }

    public boolean isDisabled(Player player) { return disabledPlayers.contains(player.getUniqueId()); }

    public void togglePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPlayers.contains(uuid)) { disabledPlayers.remove(uuid); }
        else { disabledPlayers.add(uuid); restoreXpBar(player); }
    }

    public void setEnabled(Player player, boolean enabled) {
        if (enabled) disabledPlayers.remove(player.getUniqueId());
        else { disabledPlayers.add(player.getUniqueId()); restoreXpBar(player); }
    }

    public void cleanupPlayer(UUID uuid) {
        disabledPlayers.remove(uuid);
        savedLevels.remove(uuid);
        savedExp.remove(uuid);
        xpCooldown.remove(uuid);
    }
}
