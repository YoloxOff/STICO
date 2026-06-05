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

    // Joueurs qui ont désactivé la barre manuellement
    private final Set<UUID> disabledPlayers = new HashSet<>();

    // Stocke le niveau XP réel des joueurs pour le restaurer
    private final Map<UUID, Integer> savedLevels = new HashMap<>();
    private final Map<UUID, Float> savedExp = new HashMap<>();

    // Cooldown XP : masque la barre locator pendant X ticks si XP change
    private final Map<UUID, Integer> xpCooldown = new HashMap<>();

    // ─── Caractères de la barre ───────────────────────────────────────────────
    // On utilise des caractères Unicode pour dessiner la barre
    // Chaque "slot" de joueur est représenté par un bloc coloré + symbole directionnel
    private static final String ARROW_UP   = "▲";
    private static final String ARROW_DOWN = "▼";
    private static final String DOT        = "⬤";
    private static final String SEPARATOR  = " ";

    public LocatorBarManager(SticoPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        int interval = plugin.getConfig().getInt("update-interval", 4);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllBars, 0L, interval);
    }

    public void stopTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    /**
     * Met à jour la barre XP de tous les joueurs connectés.
     */
    private void updateAllBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateBar(player);
        }

        // Décrémenter les cooldowns XP
        xpCooldown.replaceAll((uuid, ticks) -> ticks - plugin.getConfig().getInt("update-interval", 4));
        xpCooldown.entrySet().removeIf(e -> e.getValue() <= 0);
    }

    /**
     * Met à jour la barre d'un joueur spécifique.
     */
    public void updateBar(Player player) {
        UUID uuid = player.getUniqueId();

        // Plugin désactivé globalement
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            restoreXpBar(player);
            return;
        }

        // Joueur a désactivé sa barre
        if (disabledPlayers.contains(uuid)) {
            restoreXpBar(player);
            return;
        }

        // Cooldown XP actif → on laisse la barre XP native s'afficher
        if (xpCooldown.containsKey(uuid)) {
            restoreXpBar(player);
            return;
        }

        // Récupère les joueurs visibles
        List<Player> targets = getVisiblePlayers(player);

        if (targets.isEmpty()) {
            // Aucun joueur visible → afficher la barre XP normale
            restoreXpBar(player);
            return;
        }

        // Sauvegarde les vraies valeurs XP
        savedLevels.put(uuid, player.getLevel());
        savedExp.put(uuid, player.getExp());

        // Construction de la barre locator
        renderLocatorBar(player, targets);
    }

    /**
     * Construit et affiche la barre locator dans la barre d'XP du joueur.
     * 
     * Le principe : on utilise setLevel() pour afficher le niveau XP,
     * et setExp() (valeur 0.0 à 1.0) pour contrôler la progression visuelle.
     * 
     * Pour la "barre" elle-même, on envoie un BossBar ou on exploite
     * le titre du niveau via sendActionBar (plus propre et non-intrusif).
     * 
     * Architecture : 
     *  - La barre XP verte affiche une progression représentant les joueurs
     *    (divisée en segments selon le nombre de joueurs)
     *  - Le niveau XP affiche un résumé textuel via le componant de niveau
     *  - Un ActionBar au-dessus affiche les icônes colorées des joueurs
     *    avec les flèches directionnelles (comme la 1.21.6)
     */
    private void renderLocatorBar(Player viewer, List<Player> targets) {
        int count = targets.size();
        ColorManager colorManager = plugin.getColorManager();

        // ── 1. ActionBar : icônes des joueurs avec couleurs et flèches ──────
        Component barComponent = buildLocatorComponent(viewer, targets, colorManager);
        viewer.sendActionBar(barComponent);

        // ── 2. Barre XP : on segmente la barre selon le nombre de joueurs ──
        // On met le niveau à 0 pour éviter l'affichage du chiffre de niveau
        // et on utilise la progression pour "dessiner" la présence
        float progress = Math.min(1.0f, count / 10.0f); // 10 joueurs = barre pleine
        viewer.setLevel(0);
        viewer.setExp(progress);
    }

    /**
     * Construit le composant visuel de la barre locator.
     * Format : [⬤↑] [⬤] [⬤↓]  (icône coloré + flèche si décalage vertical)
     */
    private Component buildLocatorComponent(Player viewer, List<Player> targets, ColorManager colorManager) {
        int verticalThreshold = plugin.getConfig().getInt("vertical-arrow-threshold", 10);

        Component bar = Component.text("◀ ", NamedTextColor.DARK_GRAY);

        for (int i = 0; i < targets.size(); i++) {
            Player target = targets.get(i);
            TextColor color = colorManager.getPlayerColor(target);

            // Calcul de la direction verticale
            double yDiff = target.getLocation().getY() - viewer.getLocation().getY();
            String icon;

            if (yDiff > verticalThreshold) {
                icon = DOT + ARROW_UP;
            } else if (yDiff < -verticalThreshold) {
                icon = DOT + ARROW_DOWN;
            } else {
                icon = DOT;
            }

            // Nom du joueur au survol (hover event)
            Component playerIcon = Component.text(icon, color)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            Component.text(target.getName(), color)
                                    .append(Component.newline())
                                    .append(Component.text("X: " + (int) target.getLocation().getX(), NamedTextColor.GRAY))
                                    .append(Component.text(" Y: " + (int) target.getLocation().getY(), NamedTextColor.GRAY))
                                    .append(Component.text(" Z: " + (int) target.getLocation().getZ(), NamedTextColor.GRAY))
                    ));

            bar = bar.append(playerIcon);

            if (i < targets.size() - 1) {
                bar = bar.append(Component.text(SEPARATOR, NamedTextColor.GRAY));
            }
        }

        bar = bar.append(Component.text(" ▶", NamedTextColor.DARK_GRAY));
        return bar;
    }

    /**
     * Retourne la liste des joueurs visibles pour un viewer donné.
     * Respecte les règles : sneak, spectator, distance, monde, bypass.
     */
    private List<Player> getVisiblePlayers(Player viewer) {
        List<Player> result = new ArrayList<>();
        boolean sneakHides = plugin.getConfig().getBoolean("sneak-hides", true);
        boolean hideSpectators = plugin.getConfig().getBoolean("hide-spectators", true);
        boolean crossWorld = plugin.getConfig().getBoolean("cross-world", false);
        double maxDistance = plugin.getConfig().getDouble("max-distance", 0);

        World viewerWorld = viewer.getWorld();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer)) continue;

            // Monde différent
            if (!crossWorld && !target.getWorld().equals(viewerWorld)) continue;

            // Spectateur caché
            if (hideSpectators && target.getGameMode() == GameMode.SPECTATOR) continue;

            // Joueur accroupi → invisible sur la barre
            if (sneakHides && target.isSneaking()) continue;

            // Permission bypass → n'apparaît pas sur la barre des autres
            if (target.hasPermission("stico.bypass")) continue;

            // Distance maximale
            if (maxDistance > 0 && target.getWorld().equals(viewerWorld)) {
                double dist = viewer.getLocation().distance(target.getLocation());
                if (dist > maxDistance) continue;
            }

            result.add(target);
        }

        return result;
    }

    /**
     * Restaure la vraie barre XP d'un joueur.
     */
    public void restoreXpBar(Player player) {
        UUID uuid = player.getUniqueId();
        if (savedLevels.containsKey(uuid)) {
            player.setLevel(savedLevels.get(uuid));
            player.setExp(savedExp.get(uuid));
            savedLevels.remove(uuid);
            savedExp.remove(uuid);
        }
    }

    /**
     * Restaure la barre XP de tous les joueurs (used on disable).
     */
    public void restoreAllXpBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            restoreXpBar(player);
        }
    }

    /**
     * Déclenche un cooldown XP pour un joueur (masque la barre locator pendant 5s).
     */
    public void triggerXpCooldown(Player player) {
        xpCooldown.put(player.getUniqueId(), 100); // 100 ticks = 5 secondes
        restoreXpBar(player);
    }

    // ── Gestion toggle ────────────────────────────────────────────────────────

    public boolean isDisabled(Player player) {
        return disabledPlayers.contains(player.getUniqueId());
    }

    public void togglePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);
        } else {
            disabledPlayers.add(uuid);
            restoreXpBar(player);
        }
    }

    public void setEnabled(Player player, boolean enabled) {
        if (enabled) {
            disabledPlayers.remove(player.getUniqueId());
        } else {
            disabledPlayers.add(player.getUniqueId());
            restoreXpBar(player);
        }
    }

    public void cleanupPlayer(UUID uuid) {
        disabledPlayers.remove(uuid);
        savedLevels.remove(uuid);
        savedExp.remove(uuid);
        xpCooldown.remove(uuid);
    }
}
