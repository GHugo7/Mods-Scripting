# Auto Farm Sweep (Fabric, Minecraft 1.21.11)

Mod client qui fait aller le joueur **X secondes à gauche** puis **Y secondes à droite**,
en boucle, tout en **tapant (cassant) les cultures** visées.

## Utilisation

| Touche | Action |
|---|---|
| `K` | Ouvre le menu de réglages |
| `J` | Démarre / arrête |

Les touches sont modifiables dans *Options > Contrôles > Divers*.

Dans le menu :
- **Secondes à gauche / à droite** : durée de chaque côté (décimales acceptées, ex. `7.5`).
- **Allers-retours** : nombre de cycles gauche+droite, `0` = infini.
- **Casser les blocs visés** : maintient le clic gauche sur le bloc visé.
- **Cultures uniquement** : ne tape que blé, carottes, patates, betteraves, verrues, cacao,
  canne à sucre, cactus, bambou, melons et citrouilles (évite de casser la terre labourée).
- **Cultures mûres uniquement** : ignore les cultures pas encore arrivées à maturité.

Visez les cultures (regard vers le bas/devant) avant de lancer. Ouvrir un menu (inventaire, chat, Échap)
met le mod en pause ; il s'arrête si vous mourez. Les réglages sont sauvegardés dans
`config/autofarm.properties`.

## Installation

1. Installer [Fabric Loader](https://fabricmc.net/use/) pour 1.21.11 et [Fabric API](https://modrinth.com/mod/fabric-api).
2. Télécharger le `.jar` (onglet *Actions* du dépôt → dernier build → artefact `autofarm-mod`),
   ou compiler avec `./gradlew build` (Java 21) : le jar est dans `build/libs/`.
3. Mettre le jar dans le dossier `mods/`.

> ⚠️ Sur un serveur multijoueur, ce genre d'automatisation peut être interdit par les règles.
