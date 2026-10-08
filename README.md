# Auto Farm Sweep (Fabric, Minecraft 1.21.11 et 26.2)

Mod client qui fait aller le joueur **X secondes à gauche** puis **Y secondes à droite**,
en boucle, tout en **tapant (cassant) les cultures** visées.

## Utilisation

| Touche | Action |
|---|---|
| `K` | Ouvre le menu de réglages |
| `J` | Démarre depuis le début / arrête complètement |
| `H` | Pause / reprendre (la progression est gardée) |
| `L` | Statistiques |

Ce sont les touches par défaut : elles sont modifiables dans *Options > Contrôles > Divers*, et les messages du mod affichent toujours la touche choisie.

### Déroulement

Une **boucle** = *Allers-retours* × (gauche pendant X s, puis droite pendant Y s), puis le *Délai avant*
(immobile), la *Commande de fin*, puis l'*Attente après*. Le mod répète la boucle *Boucles* fois (0 = sans fin).

Exemple : 3 allers-retours, 2 boucles, commande `/home farm`, délai avant 2 s, attente 3 s →
G-D-G-D-G-D, 2 s immobile, `/home farm`, 3 s d'attente, G-D-G-D-G-D, 2 s immobile, `/home farm`, fin.

### Réglages du menu

Passez la souris sur un bouton ou une case pour voir son explication.

| Réglage | Ancien nom | Ce qu'il fait |
|---|---|---|
| **Secondes à gauche / à droite** | — | Durée de chaque côté (décimales acceptées, ex. `7.5`). |
| **Allers-retours** | — | Nombre d'allers-retours (gauche puis droite) dans une boucle. Minimum 1. |
| **Boucles** | Allers-retours (0 = ∞) | Nombre de fois que la boucle est répétée. `0` ou vide = sans fin. |
| **Commande de fin** | — | Commande envoyée à la fin de chaque boucle (ex. `/home farm`). Plusieurs : séparez par `;`. Vide = aucune. |
| **Délai avant (s)** | — | Temps où le joueur reste immobile à la fin des allers-retours, avant d'envoyer la commande (2 s par défaut). Plus naturel qu'une téléportation instantanée. Ignoré s'il n'y a pas de commande. |
| **Attente après (s)** | — | Temps d'attente après la commande de fin avant de repartir (ex. le temps de la téléportation). |
| **Hasard délais (%)** | — | Variation aléatoire du *Délai avant* et de l'*Attente après* (20 % par défaut) : 20 % sur 2 s donne entre 1,6 et 2,4 s. 0 = délais fixes. Les durées gauche/droite ne sont pas modifiées, pour ne pas décaler le joueur dans le champ. |
| **Texte captcha** | Arrêt si le chat dit | Si un message du chat contient ce texte (majuscules ignorées), le mod se **met en pause en gardant sa progression** et vous prévient. Par défaut `/captcha start`. Vide = désactivé. Après le captcha, `H` pour reprendre. |
| **Casse auto** | Casser / Casser les blocs visés | Maintient le clic gauche pour casser ce que vous visez pendant les déplacements. **NON** = le mod se déplace seulement. |
| **Seulement cultures** | Cultures / Cultures uniquement | **OUI** = ne casse que les cultures (blé, carottes, patates, betteraves, verrues, cacao, canne à sucre, cactus, bambou, melon, citrouille), jamais la terre labourée. **NON** = casse n'importe quel bloc visé. |
| **Seulement mûres** | Mûres / Cultures mûres uniquement | **OUI** = ignore les cultures qui n'ont pas fini de pousser. **NON** = casse aussi les jeunes pousses. |
| **Notif PC** | — | Au captcha, notification Windows / macOS / Linux, même si Minecraft est en arrière-plan. |
| **Téléphone (ntfy)** | — | Nom de votre topic [ntfy](https://ntfy.sh) pour recevoir les alertes sur le téléphone (captcha et mentions). Vide = désactivé. Voir ci-dessous. |
| **Mention → tél** | — | Notification sur le téléphone quand un autre joueur écrit votre pseudo dans le chat ou vous envoie un message privé. Vos propres messages sont ignorés (l'auteur est le pseudo juste avant le séparateur : `:`, `>`, `»`, `▶`, `➤`...). |
| **Alarme** | — | Au captcha, bips pendant ~5 s, joués hors de Minecraft (le volume du jeu ne compte pas). |
| **Volume** | Volume alarme | Volume des bips, de 0 à 100 % (30 % par défaut). |
| **Démarrer / Arrêter** | — | Démarre depuis le début, ou arrête complètement (progression perdue). Comme `J`. |
| **Pause / Reprendre** | — | Met en pause puis reprend exactement au même endroit (même côté, même temps restant, même aller-retour et même boucle). Comme `H`. Les réglages modifiés pendant la pause sont pris en compte à la reprise. |
| **Tester l'alerte** | Tester | Déclenche l'alerte tout de suite pour vérifier le son et la notification. |

Au captcha, un gros titre « CAPTCHA ! » s'affiche aussi en jeu et l'icône de Minecraft clignote dans la barre des tâches.

Visez les cultures (regard vers le bas/devant) avant de lancer. Le mod continue de tourner quand un menu est
ouvert (Échap, inventaire, fenêtre en arrière-plan) ; il se met en pause avec `H`, s'arrête avec `J` ou si vous mourez.
En solo, Échap met le monde en pause : ouvrez le monde en LAN pour que la ferme continue, et `F3 + P`
désactive la pause automatique quand la fenêtre perd le focus. Les réglages sont sauvegardés dans
`config/autofarm.properties`.

### Statistiques (touche `L` ou bouton « Statistiques » du menu)

Le mod lit le chat du serveur pendant le farm :

| Source | Exemple | Compté comme |
|---|---|---|
| Bilan de moisson | `Bénéfices : 72 457 731.61$`, `Cultures récoltées : 605` | **Argent** et **Cultures** (avec la moyenne par heure) |
| Récompenses | `Tu as reçu +5 Crystaux !`, `Tu as reçu un LuckyBlock !`, `Tu as gagné 1 Token !`, `Tu as obtenu un Fragment de Clé !` | Une ligne par récompense, détectée automatiquement (les nouvelles récompenses apparaissent toutes seules) |
| Événements | `La Moisson Dorée explose…`, `La Pluie Cristalline ruisselle…`, `L'Aura Solaire Suprême s'embrase…` | Nombre d'apparitions de chaque événement |

L'argent ne vient **que** des bilans de moisson : les gains des événements y sont déjà inclus, donc
`Tu as reçu 625,000,000 $` n'est pas ajouté une deuxième fois. Les messages des autres joueurs (`X a reçu des cadeaux`) sont ignorés.

L'écran affiche deux colonnes : **Session** (depuis le dernier démarrage) et **Total** (toutes les sessions, gardé même
après avoir quitté le jeu, dans `config/autofarm-stats.properties`). On y règle aussi :

- les cases ✔/✖ devant chaque ligne : afficher ou masquer la ligne dans le panneau à l'écran et les bilans ;
- **Panneau à l'écran** : statistiques de la session en haut à gauche pendant le farm ;
- **Bilan tél** : bilan de la session sur le téléphone jamais, à l'arrêt, ou à l'arrêt et toutes les X minutes ;
- **Remettre la session / le total à 0** (le total demande une confirmation).
- **Alertes chat** : textes du chat qui déclenchent une alerte, séparés par `;` (par défaut
  `booster de moisson vient d'expirer`). Quand un message contient l'un d'eux : titre à l'écran, **son de cloche**
  (différent du son du captcha) et notification téléphone. Le farm continue. Vos propres messages sont ignorés.

À l'arrêt du farm, le bilan de la session est aussi écrit dans le chat.

### Notifications sur le téléphone (ntfy)

1. Installez l'appli **ntfy** ([Android](https://play.google.com/store/apps/details?id=io.heckel.ntfy) / [iPhone](https://apps.apple.com/app/ntfy/id1625396347)).
2. Dans l'appli, appuyez sur **+** et abonnez-vous à un topic avec un nom long et difficile à deviner,
   par exemple `farm-ghugo-8k2q` : les topics ntfy sont publics, n'importe qui connaissant le nom peut lire les messages.
3. Mettez le même nom dans la case **Téléphone (ntfy)** du menu, puis cliquez sur **Tester l'alerte** :
   la notification doit arriver sur le téléphone.

Vous recevez alors une notification **urgente** au captcha, et une notification quand quelqu'un vous mentionne
(au plus une toutes les 5 s). La case accepte aussi une adresse complète (`https://mon-serveur-ntfy/topic`)
si vous hébergez votre propre serveur ntfy.

## Installation

1. Installer [Fabric Loader](https://fabricmc.net/use/) et [Fabric API](https://modrinth.com/mod/fabric-api) pour votre version de Minecraft.
2. Télécharger le `.jar` dans les [Releases](https://github.com/GHugo7/Mods-Scripting/releases). Chaque release en contient deux :
   - `autofarm-1.0.N+1.21.11.jar` pour Minecraft **1.21.11** ;
   - `autofarm-1.0.N+26.2.jar` pour Minecraft **26.2** (Java 25 requis, fourni par le launcher officiel).
3. Mettre le jar dans le dossier `mods/`.

Compiler soi-même (JDK 25) : `./gradlew build` pour 1.21.11, `./gradlew -p versions/26.2 build` pour 26.2.
Les deux versions partagent le même code source (`src/`) ; le dossier `versions/26.2` ne contient que la configuration du build.

> ⚠️ Sur un serveur multijoueur, ce genre d'automatisation peut être interdit par les règles.
