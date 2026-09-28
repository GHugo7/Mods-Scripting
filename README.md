# Auto Farm Sweep (Fabric, Minecraft 1.21.11)

Mod client qui fait aller le joueur **X secondes à gauche** puis **Y secondes à droite**,
en boucle, tout en **tapant (cassant) les cultures** visées.

## Utilisation

| Touche | Action |
|---|---|
| `K` | Ouvre le menu de réglages |
| `J` | Démarre / arrête |

Les touches sont modifiables dans *Options > Contrôles > Divers*.

Dans le menu (passez la souris sur un bouton pour voir son explication) :
- **Secondes à gauche / à droite** : durée de chaque côté (décimales acceptées, ex. `7.5`).
- **Allers-retours** : nombre de cycles gauche+droite, `0` = infini.
- **Casse auto** : maintient le clic gauche sur le bloc visé.
- **Que cultures** : ne tape que blé, carottes, patates, betteraves, verrues, cacao,
  canne à sucre, cactus, bambou, melons et citrouilles (évite de casser la terre labourée).
- **Que mûres** : ignore les cultures pas encore arrivées à maturité.
- **Arrêt si le chat dit** : si un message du chat contient ce texte (majuscules ignorées), le mod s'arrête
  et vous prévient. Par défaut `/captcha start`. Laisser vide pour désactiver. Après le captcha, `J` pour reprendre.
- **Alarme** : série de bips pendant ~5 s, joués hors de Minecraft (le volume du jeu ne compte pas).
- **Volume alarme** : curseur de 0 à 100 % (30 % par défaut). Utilisez « Tester l'alerte » pour régler.
- **Notif PC** : notification Windows / macOS / Linux (`notify-send`).
- **Tester** : déclenche l'alerte pour vérifier le son et la notification.

Au captcha, un gros titre « CAPTCHA ! » s'affiche aussi en jeu et l'icône de Minecraft clignote dans la barre des tâches.

Visez les cultures (regard vers le bas/devant) avant de lancer. Le mod continue de tourner quand un menu est
ouvert (Échap, inventaire, fenêtre en arrière-plan) ; il s'arrête avec `J` ou si vous mourez.
En solo, Échap met le monde en pause : ouvrez le monde en LAN pour que la ferme continue, et `F3 + P`
désactive la pause automatique quand la fenêtre perd le focus. Les réglages sont sauvegardés dans
`config/autofarm.properties`.

## Installation

1. Installer [Fabric Loader](https://fabricmc.net/use/) pour 1.21.11 et [Fabric API](https://modrinth.com/mod/fabric-api).
2. Télécharger le `.jar` dans les [Releases](https://github.com/GHugo7/Mods-Scripting/releases) (une release par build),
   ou compiler avec `./gradlew build` (JDK 25 requis pour Gradle/Loom ; le mod reste compatible Java 21) : le jar est dans `build/libs/`.
3. Mettre le jar dans le dossier `mods/`.

> ⚠️ Sur un serveur multijoueur, ce genre d'automatisation peut être interdit par les règles.
