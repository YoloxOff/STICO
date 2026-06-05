package fr.stico.plugin.managers;

import fr.stico.plugin.SticoPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.*;

public class ColorManager {

    private final SticoPlugin plugin;

    // Couleurs assignées à chaque joueur (persistantes pendant la session)
    private final Map<UUID, TextColor> playerColors = new LinkedHashMap<>();

    // Pool de couleurs disponibles (chargé depuis config)
    private final List<TextColor> colorPool = new ArrayList<>();

    // Couleurs par défaut
    private static final List<TextColor> DEFAULT_COLORS = List.of(
            NamedTextColor.RED,
            NamedTextColor.BLUE,
            NamedTextColor.GREEN,
            NamedTextColor.YELLOW,
            NamedTextColor.GOLD,
            NamedTextColor.AQUA,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.WHITE
    );

    public ColorManager(SticoPlugin plugin) {
        this.plugin = plugin;
        loadColors();
    }

    private void loadColors() {
        colorPool.clear();
        List<String> configColors = plugin.getConfig().getStringList("player-colors");

        if (configColors.isEmpty()) {
            colorPool.addAll(DEFAULT_COLORS);
            return;
        }

        for (String colorName : configColors) {
            NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());
            if (color != null) {
                colorPool.add(color);
            }
        }

        if (colorPool.isEmpty()) {
            colorPool.addAll(DEFAULT_COLORS);
        }
    }

    /**
     * Retourne la couleur assignée à un joueur.
     * Si aucune couleur n'est assignée, en attribue une automatiquement.
     */
    public TextColor getPlayerColor(Player player) {
        return playerColors.computeIfAbsent(player.getUniqueId(), uuid -> assignNextColor());
    }

    /**
     * Assigne manuellement une couleur à un joueur (commande admin).
     */
    public void setPlayerColor(Player player, TextColor color) {
        playerColors.put(player.getUniqueId(), color);
    }

    /**
     * Supprime la couleur assignée à un joueur.
     */
    public void removePlayerColor(UUID uuid) {
        playerColors.remove(uuid);
    }

    /**
     * Attribue la prochaine couleur disponible dans le pool (rotation).
     */
    private TextColor assignNextColor() {
        int index = playerColors.size() % colorPool.size();
        return colorPool.get(index);
    }

    /**
     * Recharge les couleurs depuis la config.
     */
    public void reload() {
        loadColors();
    }

    /**
     * Retourne une TextColor depuis un nom de couleur.
     */
    public static TextColor fromName(String name) {
        NamedTextColor color = NamedTextColor.NAMES.value(name.toLowerCase());
        return color != null ? color : NamedTextColor.WHITE;
    }

    /**
     * Retourne les noms de couleurs disponibles.
     */
    public static List<String> getAvailableColorNames() {
        return List.of(
                "red", "blue", "green", "yellow", "gold",
                "aqua", "light_purple", "white", "dark_red",
                "dark_blue", "dark_green", "dark_aqua",
                "dark_purple", "dark_gray", "gray", "black"
        );
    }
}
