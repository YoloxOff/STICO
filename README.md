# STICO 🗺️
**Locator Bar pour Minecraft 1.21** — imite la feature native de la 1.21.6

## Installation
1. Compile avec `./build.sh` (ou `mvn clean package`)
2. Copie `target/STICO-1.0.0.jar` dans ton dossier `plugins/`
3. Redémarre le serveur (Paper 1.21 requis)

## Fonctionnement
La barre d'XP est remplacée par une barre de localisation affichant
les icônes colorés des joueurs connectés, avec des flèches ↑ ↓ si
un joueur est significativement au-dessus ou en dessous.

Comportement identique à la 1.21.6 :
- ⬤ = joueur au même niveau
- ⬤▲ = joueur au-dessus
- ⬤▼ = joueur en dessous
- Si tu gagnes de l'XP → barre XP normale pendant 5s, puis retour locator
- Si un joueur se baisse (sneak) → disparaît de ta barre

## Commandes
| Commande | Permission | Description |
|---|---|---|
| `/stico toggle` | `stico.use` | Activer/désactiver ta barre |
| `/stico color <joueur> <couleur>` | `stico.admin` | Changer la couleur d'un joueur |
| `/stico status` | `stico.admin` | Voir l'état du plugin |
| `/stico reload` | `stico.admin` | Recharger la config |

## Permissions spéciales
- `stico.bypass` — Le joueur n'apparaît pas sur la barre des autres (invisible)

## Config (config.yml)
```yaml
enabled: true           # Activer le plugin
update-interval: 4      # Fréquence (ticks) — 4 = 0.2s
max-distance: 0         # 0 = illimité
cross-world: false      # Voir joueurs d'autres mondes
vertical-arrow-threshold: 10  # Écart Y pour afficher les flèches
sneak-hides: true       # Sneak = invisible sur la barre
hide-spectators: true   # Cacher les spectateurs
join-message: true      # Message de bienvenue
```

## Compatibilité
- **Serveur** : Paper 1.21
- **Java** : 21+
