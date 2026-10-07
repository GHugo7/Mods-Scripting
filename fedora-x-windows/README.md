# Fedora X Windows

Le meilleur des deux mondes : **Fedora** (son terminal, ses commandes, ses logiciels) et
**Windows** (les `.exe`, tous les jeux). Deux façons d'y arriver, à choisir selon tes jeux.

| | [Variante 1 : Fedora d'abord](fedora/README.md) | [Variante 2 : Windows d'abord](windows/README.md) |
|---|---|---|
| Système de base (noyau) | Linux (Fedora) | **Windows** |
| Commandes Fedora (`dnf`, bash…) | ✅ partout | ✅ dans le terminal (Fedora intégré via WSL) |
| Applications Linux (Fichiers, éditeur…) | ✅ | ✅ dans le menu Démarrer |
| Interface | GNOME ou KDE | Windows habillé façon Fedora |
| Double-clic sur un `.exe` | ✅ via Proton/Wine | ✅ natif |
| Jeux Steam, Epic, GOG | ✅ ~85-90 % des jeux | ✅ 100 % |
| **Valorant, LoL, Fortnite, CoD…** (anti-triche noyau) | ❌ impossible | ✅ |
| Compiler des `.exe` | ✅ MinGW | ✅ MinGW dans Fedora, et lancement direct |
| Installation | `./install.sh` sur une Fedora | `setup.ps1` sur Windows 11 |

**En résumé :** si tu joues à Valorant ou League of Legends, prends la **variante 2**.
Sinon, la **variante 1** te donne un vrai Fedora qui lance presque tous les jeux Windows.
Les deux peuvent cohabiter sur le même PC (double démarrage).

## Pourquoi pas « un Fedora avec le noyau Windows » ?

C'est la variante 2, sous la seule forme qui existe vraiment :

- Le noyau Windows est **fermé** : sa licence interdit de le modifier ou de le redistribuer
  dans un autre système. Personne ne peut fabriquer « une distribution Fedora avec le noyau
  Windows ».
- **WSL** (Windows Subsystem for Linux) fait le chemin inverse, officiellement : il fait
  tourner la vraie Fedora (publiée par le projet Fedora) à l'intérieur de Windows, avec ses
  applications graphiques. Le noyau reste celui de Windows, donc **Vanguard fonctionne**.
- Faire croire à Vanguard qu'un autre système est Windows (noyau « compatible », machine
  virtuelle cachée…) serait un contournement d'anti-triche : bannissement assuré du compte
  et du matériel. Ce projet ne le fait pas et ne le fera pas.

## Aussi dans ce dépôt

[`../os/`](../os/README.md) : NovaOS, un petit système d'exploitation écrit de zéro (noyau x86
en C), pour apprendre comment fonctionne un OS.
