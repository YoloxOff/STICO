package fr.stico.plugin.commands;

import fr.stico.plugin.SticoPlugin;
import fr.stico.plugin.managers.ColorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SticoCommand implements CommandExecutor, TabCompleter {

    private final SticoPlugin plugin;

    public SticoCommand(SticoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── /stico help ────────────────────────────────────────────────
            case "help" -> sendHelp(sender);

            // ── /stico toggle ──────────────────────────────────────────────
            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(prefix().append(Component.text("Cette commande est réservée aux joueurs.", NamedTextColor.RED)));
                    return true;
                }
                if (!player.hasPermission("stico.use")) {
                    player.sendMessage(prefix().append(Component.text("Permission refusée.", NamedTextColor.RED)));
                    return true;
                }
                plugin.getLocatorBarManager().togglePlayer(player);
                boolean disabled = plugin.getLocatorBarManager().isDisabled(player);
                player.sendMessage(prefix().append(Component.text(
                        disabled ? "Locator Bar désactivée." : "Locator Bar activée.",
                        disabled ? NamedTextColor.RED : NamedTextColor.GREEN
                )));
            }

            // ── /stico reload ──────────────────────────────────────────────
            case "reload" -> {
                if (!sender.hasPermission("stico.admin")) {
                    sender.sendMessage(prefix().append(Component.text("Permission refusée.", NamedTextColor.RED)));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getColorManager().reload();
                plugin.getLocatorBarManager().stopTask();
                plugin.getLocatorBarManager().startTask();
                sender.sendMessage(prefix().append(Component.text("Configuration rechargée !", NamedTextColor.GREEN)));
            }

            // ── /stico color <joueur> <couleur> ───────────────────────────
            case "color" -> {
                if (!sender.hasPermission("stico.admin")) {
                    sender.sendMessage(prefix().append(Component.text("Permission refusée.", NamedTextColor.RED)));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(prefix().append(Component.text("Usage : /stico color <joueur> <couleur>", NamedTextColor.YELLOW)));
                    sender.sendMessage(prefix().append(Component.text("Couleurs : " + String.join(", ", ColorManager.getAvailableColorNames()), NamedTextColor.GRAY)));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(prefix().append(Component.text("Joueur introuvable : " + args[1], NamedTextColor.RED)));
                    return true;
                }
                TextColor color = ColorManager.fromName(args[2]);
                plugin.getColorManager().setPlayerColor(target, color);
                sender.sendMessage(prefix().append(
                        Component.text("Couleur de ", NamedTextColor.GRAY)
                                .append(Component.text(target.getName(), color))
                                .append(Component.text(" définie sur ", NamedTextColor.GRAY))
                                .append(Component.text(args[2].toLowerCase(), color))
                                .append(Component.text(".", NamedTextColor.GRAY))
                ));
            }

            // ── /stico status ──────────────────────────────────────────────
            case "status" -> {
                if (!sender.hasPermission("stico.admin")) {
                    sender.sendMessage(prefix().append(Component.text("Permission refusée.", NamedTextColor.RED)));
                    return true;
                }
                boolean globalEnabled = plugin.getConfig().getBoolean("enabled", true);
                int interval = plugin.getConfig().getInt("update-interval", 4);
                int online = Bukkit.getOnlinePlayers().size();
                sender.sendMessage(prefix().append(Component.text("=== Statut STICO ===", NamedTextColor.GOLD)));
                sender.sendMessage(Component.text("  Plugin : ", NamedTextColor.GRAY)
                        .append(Component.text(globalEnabled ? "✔ Actif" : "✘ Inactif", globalEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
                sender.sendMessage(Component.text("  Intervalle : ", NamedTextColor.GRAY)
                        .append(Component.text(interval + " ticks (" + (interval / 20.0) + "s)", NamedTextColor.YELLOW)));
                sender.sendMessage(Component.text("  Joueurs en ligne : ", NamedTextColor.GRAY)
                        .append(Component.text(online, NamedTextColor.AQUA)));
                sender.sendMessage(Component.text("  Sneak cache : ", NamedTextColor.GRAY)
                        .append(Component.text(plugin.getConfig().getBoolean("sneak-hides") ? "✔ Oui" : "✘ Non", NamedTextColor.YELLOW)));
            }

            default -> {
                sender.sendMessage(prefix().append(Component.text("Sous-commande inconnue. ", NamedTextColor.RED))
                        .append(Component.text("/stico help", NamedTextColor.YELLOW)));
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("  ★ STICO - Locator Bar", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("  /stico toggle", NamedTextColor.YELLOW)
                .append(Component.text(" - Activer/désactiver ta barre", NamedTextColor.GRAY)));
        if (sender.hasPermission("stico.admin")) {
            sender.sendMessage(Component.text("  /stico color <joueur> <couleur>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Changer la couleur d'un joueur", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("  /stico status", NamedTextColor.YELLOW)
                    .append(Component.text(" - Voir l'état du plugin", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("  /stico reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Recharger la config", NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.text(""));
    }

    private Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("STICO", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("toggle", "help"));
            if (sender.hasPermission("stico.admin")) {
                subs.addAll(List.of("color", "status", "reload"));
            }
            subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .forEach(completions::add);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("color") && sender.hasPermission("stico.admin")) {
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .forEach(completions::add);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("color") && sender.hasPermission("stico.admin")) {
            ColorManager.getAvailableColorNames().stream()
                    .filter(c -> c.startsWith(args[2].toLowerCase()))
                    .forEach(completions::add);
        }

        return completions;
    }
}
