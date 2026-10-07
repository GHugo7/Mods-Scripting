#!/usr/bin/env bash
# Fedora X Windows - transforme une Fedora Workstation/KDE en PC de jeu qui lance les .exe.
#
#   ./install.sh            installation interactive
#   ./install.sh --yes      sans questions
#   ./install.sh --dry-run  affiche ce qui serait fait, sans rien changer
#   ./install.sh --help     toutes les options

set -Eeuo pipefail

readonly FXW_VERSION="1.0"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

# Version d'umu-launcher (Proton hors de Steam) récupérée sur GitHub si Fedora ne l'a pas.
readonly UMU_VERSION="${UMU_VERSION:-1.4.4}"
readonly UMU_RELEASES="https://github.com/Open-Wine-Components/umu-launcher/releases/download"

readonly FLATPAKS=(
    com.heroicgameslauncher.hgl   # Epic Games, GOG, Amazon Prime Gaming
    com.usebottles.bottles        # "bouteilles" Windows isolées, pour les logiciels
    com.vysp3r.ProtonPlus         # installe GE-Proton et les autres versions de Proton
    com.github.tchx84.Flatseal    # permissions des applications Flatpak
)

ASSUME_YES=0
DRY_RUN=0
IMAGE_MODE=0
DO_UPDATE=1
DO_CODECS=1
DO_FLATPAK=1
DO_DEV=1
NVIDIA_MODE="auto"           # auto | modern | 580xx | none
UNINSTALL=0

# ---------------------------------------------------------------- affichage

if [[ -t 1 ]]; then
    C_BLUE=$'\033[1;34m' C_GREEN=$'\033[1;32m' C_YELLOW=$'\033[1;33m' C_RED=$'\033[1;31m' C_OFF=$'\033[0m'
else
    C_BLUE="" C_GREEN="" C_YELLOW="" C_RED="" C_OFF=""
fi

step() { printf '\n%s==> %s%s\n' "$C_BLUE" "$*" "$C_OFF"; }
info() { printf '    %s\n' "$*"; }
ok() { printf '    %s✔%s %s\n' "$C_GREEN" "$C_OFF" "$*"; }
warn() { printf '    %s!%s %s\n' "$C_YELLOW" "$C_OFF" "$*"; }
die() {
    printf '\n%sErreur :%s %s\n' "$C_RED" "$C_OFF" "$*" >&2
    exit 1
}

NOTES=()          # remarques affichées dans le résumé final
REBOOT_NEEDED=0
note() { NOTES+=("$*"); }

on_error() {
    local status=$? line=$1
    # set -E propage le piège dans les sous-shells : on ne signale l'erreur qu'une fois.
    [[ "$BASHPID" == "$$" ]] || return "$status"
    printf '\n%sL\x27installation a échoué%s (ligne %s, code %s).\n' "$C_RED" "$C_OFF" "$line" "$status" >&2
    [[ -n "${LOG_FILE:-}" ]] && printf 'Journal complet : %s\n' "$LOG_FILE" >&2
    printf 'Vous pouvez relancer le script : les étapes déjà faites sont sautées.\n' >&2
}
trap 'on_error $LINENO' ERR

usage() {
    cat <<EOF
Fedora X Windows $FXW_VERSION - jeux et programmes Windows (.exe) sur Fedora

Utilisation : ./install.sh [options]

  -y, --yes           répond oui à toutes les questions
  -n, --dry-run       montre les commandes sans rien exécuter
      --no-update     ne met pas le système à jour d'abord
      --no-codecs     n'installe pas les codecs vidéo/audio (RPM Fusion)
      --no-flatpak    n'installe pas Heroic, Bottles, ProtonPlus, Flatseal
      --no-dev        n'installe pas le compilateur Windows (MinGW)
      --nvidia=MODE   auto (défaut), modern (RTX/GTX 16xx et +), 580xx (GTX 9xx/10xx), none
      --uninstall     retire l'intégration fxw (garde les paquets installés)
      --image         mode construction d'image (Containerfile), utilisé par image/Containerfile
  -h, --help          cette aide
EOF
}

parse_args() {
    while (($#)); do
        case "$1" in
            -y | --yes) ASSUME_YES=1 ;;
            -n | --dry-run) DRY_RUN=1 ;;
            --no-update) DO_UPDATE=0 ;;
            --no-codecs) DO_CODECS=0 ;;
            --no-flatpak) DO_FLATPAK=0 ;;
            --no-dev) DO_DEV=0 ;;
            --nvidia=*) NVIDIA_MODE="${1#*=}" ;;
            --uninstall) UNINSTALL=1 ;;
            --image) IMAGE_MODE=1 ASSUME_YES=1 DO_UPDATE=0 DO_FLATPAK=0 NVIDIA_MODE=none ;;
            -h | --help) usage; exit 0 ;;
            *) usage; die "option inconnue : $1" ;;
        esac
        shift
    done
    [[ "$NVIDIA_MODE" =~ ^(auto|modern|580xx|none)$ ]] || die "--nvidia doit valoir auto, modern, 580xx ou none"
}

# ---------------------------------------------------------------- exécution

SUDO=()

# Lance une commande en root (sudo si besoin). En --dry-run, l'affiche seulement.
as_root() {
    if ((DRY_RUN)); then
        printf '    [simulation] %s\n' "$*"
        return 0
    fi
    "${SUDO[@]}" "$@"
}

# Lance une commande en tant que l'utilisateur qui a lancé le script.
as_user() {
    [[ -n "$TARGET_USER" ]] || return 0
    if ((DRY_RUN)); then
        printf '    [simulation, %s] %s\n' "$TARGET_USER" "$*"
        return 0
    fi
    if [[ "$(id -un)" == "$TARGET_USER" ]]; then
        "$@"
    else
        sudo -u "$TARGET_USER" -H "$@"
    fi
}

# Écrit un fichier système (contenu lu sur l'entrée standard).
write_root_file() {
    local path="$1" mode="${2:-644}"
    if ((DRY_RUN)); then
        printf '    [simulation] écrire %s\n' "$path"
        cat >/dev/null
        return 0
    fi
    "${SUDO[@]}" install -D -m "$mode" /dev/stdin "$path"
}

# Les paquets introuvables ou cassés (ex. gamescope sur une version en développement) sont sautés.
dnf_install() {
    as_root dnf install -y --skip-unavailable --skip-broken "$@"
}

ask() {
    local question="$1" default="${2:-o}"
    ((ASSUME_YES)) && return 0
    local hint="[O/n]"
    [[ "$default" == n ]] && hint="[o/N]"
    local answer
    read -r -p "    $question $hint " answer </dev/tty || answer=""
    answer="${answer:-$default}"
    [[ "${answer,,}" == o* || "${answer,,}" == y* ]]
}

installed() { rpm -q "$@" >/dev/null 2>&1; }

# ---------------------------------------------------------------- vérifications

FEDORA_VERSION=""
TARGET_USER=""
PREFIX=/usr/local

# Lit un champ de /etc/os-release sans polluer les variables du script.
os_field() {
    # shellcheck source=/dev/null
    (. /etc/os-release && printf '%s' "${!1:-}")
}

check_system() {
    step "Vérification du système"
    [[ -r /etc/os-release ]] || die "/etc/os-release est introuvable : ce n'est pas une Fedora."
    local os_id os_like os_name
    os_id="$(os_field ID)"
    os_like="$(os_field ID_LIKE)"
    os_name="$(os_field PRETTY_NAME)"
    if [[ "$os_id" != fedora ]]; then
        if [[ " $os_like " == *" fedora "* ]]; then
            warn "$os_name est dérivé de Fedora mais n'est pas Fedora : à vos risques."
            warn "(Bazzite et Nobara ont déjà tout ce que ce script installe.)"
            ask "Continuer quand même ?" n || exit 0
        else
            die "ce script est fait pour Fedora (système détecté : ${os_name:-inconnu})."
        fi
    fi
    FEDORA_VERSION="$(rpm -E %fedora)"
    if ((FEDORA_VERSION < 43)); then
        die "Fedora $FEDORA_VERSION n'est plus maintenue. Mettez à jour vers Fedora 44 d'abord."
    elif ((FEDORA_VERSION > 45)); then
        warn "Fedora $FEDORA_VERSION est plus récente que ce script (testé pour 43 à 45)."
    fi
    ok "${os_name:-Fedora $FEDORA_VERSION}"

    if ((IMAGE_MODE)); then
        PREFIX=/usr
        ((EUID == 0)) || die "--image doit tourner en root (dans le Containerfile)."
        return
    fi

    if [[ -e /run/ostree-booted ]]; then
        die "Fedora Atomic (Silverblue, Kinoite, Bazzite...) détectée : ce script modifie le système avec dnf.
Utilisez plutôt l'image (voir fedora/image/README.md), ou Bazzite qui contient déjà tout."
    fi

    if ((EUID == 0)); then
        TARGET_USER="${SUDO_USER:-}"
        [[ -n "$TARGET_USER" && "$TARGET_USER" != root ]] ||
            die "lancez ce script en tant qu'utilisateur normal (pas root) : ./install.sh"
    else
        TARGET_USER="$(id -un)"
        SUDO=(sudo)
        if ((!DRY_RUN)); then
            info "Le mot de passe administrateur va être demandé (sudo)."
            sudo -v || die "sudo est nécessaire pour installer les paquets."
            # Garde sudo actif pendant toute l'installation.
            (while sleep 50; do sudo -n true 2>/dev/null || exit; kill -0 "$$" 2>/dev/null || exit; done) &
        fi
    fi
    ok "utilisateur : $TARGET_USER"

    if ((!DRY_RUN)) && ! curl -fsS --max-time 10 -o /dev/null https://mirrors.fedoraproject.org/; then
        die "pas d'accès à Internet (mirrors.fedoraproject.org injoignable)."
    fi
}

# ---------------------------------------------------------------- étapes

setup_repos() {
    step "Dépôts RPM Fusion (Steam, pilotes NVIDIA, codecs)"
    local base="https://mirrors.rpmfusion.org"
    local pkgs=()
    installed rpmfusion-free-release ||
        pkgs+=("$base/free/fedora/rpmfusion-free-release-$FEDORA_VERSION.noarch.rpm")
    installed rpmfusion-nonfree-release ||
        pkgs+=("$base/nonfree/fedora/rpmfusion-nonfree-release-$FEDORA_VERSION.noarch.rpm")
    if ((${#pkgs[@]})); then
        as_root dnf install -y "${pkgs[@]}"
        ok "RPM Fusion activé"
    else
        ok "RPM Fusion déjà activé"
    fi
    # Codec H.264 de Cisco (dépôt Fedora désactivé par défaut).
    installed dnf5-plugins || dnf_install dnf5-plugins
    as_root dnf config-manager setopt fedora-cisco-openh264.enabled=1
}

system_update() {
    ((DO_UPDATE)) || return 0
    step "Mise à jour du système"
    info "Indispensable avant d'installer un pilote NVIDIA (le noyau doit être à jour)."
    local before after
    before="$(rpm -q kernel --qf '%{VERSION}-%{RELEASE}\n' 2>/dev/null | sort -V | tail -n1 || true)"
    as_root dnf upgrade -y --refresh
    after="$(rpm -q kernel --qf '%{VERSION}-%{RELEASE}\n' 2>/dev/null | sort -V | tail -n1 || true)"
    if [[ "$before" != "$after" ]]; then
        REBOOT_NEEDED=1
        note "Un nouveau noyau a été installé."
    fi
    ok "système à jour"
}

GPU_VENDORS=""
NVIDIA_IDS=()

detect_gpus() {
    local dev class vendor
    for dev in /sys/bus/pci/devices/*; do
        class="$(cat "$dev/class" 2>/dev/null)" || continue
        [[ "$class" == 0x03* ]] || continue
        vendor="$(cat "$dev/vendor")"
        case "$vendor" in
            0x10de) GPU_VENDORS+=" nvidia"; NVIDIA_IDS+=("$(cat "$dev/device")") ;;
            0x1002) GPU_VENDORS+=" amd" ;;
            0x8086) GPU_VENDORS+=" intel" ;;
        esac
    done
}

# Génération d'une carte NVIDIA d'après son identifiant PCI :
#   >= 0x1e00 : Turing et plus récent (GTX 16xx, RTX) -> pilote actuel
#   >= 0x1340 : Maxwell, Pascal, Volta (GTX 750 à 1080) -> branche 580xx
#   sinon     : Kepler et plus ancien -> plus de pilote propriétaire, on garde nouveau
nvidia_generation() {
    local id=$(($1))
    if ((id >= 0x1e00)); then echo modern
    elif ((id >= 0x1340)); then echo 580xx
    else echo legacy
    fi
}

setup_codecs() {
    ((DO_CODECS)) || return 0
    step "Codecs multimédia (vidéos des jeux, streaming)"
    if installed ffmpeg-free; then
        as_root dnf swap -y ffmpeg-free ffmpeg --allowerasing
    fi
    dnf_install gstreamer1-plugins-bad-freeworld gstreamer1-plugins-ugly gstreamer1-plugin-libav \
        gstreamer1-plugins-good-extras gstreamer1-plugin-openh264 mozilla-openh264 lame-libs
    # Décodage vidéo matériel.
    if [[ "$GPU_VENDORS" == *amd* ]] || [[ "$GPU_VENDORS" == *intel* ]]; then
        installed mesa-va-drivers && ! installed mesa-va-drivers-freeworld &&
            as_root dnf swap -y mesa-va-drivers mesa-va-drivers-freeworld --allowerasing
    fi
    if [[ "$GPU_VENDORS" == *intel* ]]; then
        dnf_install intel-media-driver
    fi
    ok "codecs installés"
}

# Version 32 bits du pilote de décodage AMD/Intel, une fois Steam installé.
swap_codecs_i686() {
    ((DO_CODECS)) || return 0
    if installed mesa-va-drivers.i686 && ! installed mesa-va-drivers-freeworld.i686; then
        as_root dnf swap -y mesa-va-drivers.i686 mesa-va-drivers-freeworld.i686 --allowerasing || true
    fi
}

setup_nvidia() {
    [[ "$GPU_VENDORS" == *nvidia* ]] || return 0
    [[ "$NVIDIA_MODE" == none ]] && return 0
    step "Pilote NVIDIA"

    local mode="$NVIDIA_MODE" id gen
    if [[ "$mode" == auto ]]; then
        mode=""
        for id in "${NVIDIA_IDS[@]}"; do
            gen="$(nvidia_generation "$id")"
            info "Carte NVIDIA $id : génération « $gen »"
            # La carte la plus récente décide (portable avec deux cartes NVIDIA : rare).
            if [[ "$gen" == modern ]]; then mode=modern
            elif [[ "$gen" == 580xx && "$mode" != modern ]]; then mode=580xx
            fi
        done
        if [[ -z "$mode" ]]; then
            warn "Carte NVIDIA trop ancienne (Kepler ou avant) : plus aucun pilote propriétaire ne la gère."
            note "Votre carte NVIDIA est trop ancienne pour le pilote propriétaire : les jeux récents ne tourneront pas."
            return 0
        fi
    fi

    local pkgs=(akmod-nvidia xorg-x11-drv-nvidia-cuda xorg-x11-drv-nvidia-libs.i686 libva-nvidia-driver)
    [[ "$mode" == 580xx ]] && pkgs=(akmod-nvidia-580xx xorg-x11-drv-nvidia-580xx-cuda
        xorg-x11-drv-nvidia-580xx-libs.i686 libva-nvidia-driver)

    if installed "${pkgs[0]}"; then
        ok "pilote NVIDIA déjà installé (${pkgs[0]})"
        return 0
    fi
    ask "Installer le pilote NVIDIA propriétaire (${pkgs[0]}) ?" || return 0

    # Secure Boot : le module doit être signé avec une clé enregistrée dans le micrologiciel.
    if command -v mokutil >/dev/null && mokutil --sb-state 2>/dev/null | grep -qi enabled; then
        info "Secure Boot est activé : création d'une clé pour signer le pilote."
        dnf_install akmods kmodtool mokutil openssl
        [[ -f /etc/pki/akmods/certs/public_key.der ]] || as_root kmodgenca -a
        if ! mokutil --test-key /etc/pki/akmods/certs/public_key.der 2>/dev/null | grep -qi "already enrolled"; then
            info "Choisissez un mot de passe temporaire : il sera redemandé UNE fois au redémarrage."
            if ((ASSUME_YES)) || ((DRY_RUN)); then
                note "Secure Boot : lancez « sudo mokutil --import /etc/pki/akmods/certs/public_key.der », puis au redémarrage : Enroll MOK > Continue > Yes > mot de passe."
            else
                as_root mokutil --import /etc/pki/akmods/certs/public_key.der
                note "Au redémarrage, un écran bleu (MOK Manager) s'affiche : Enroll MOK > Continue > Yes > votre mot de passe temporaire. Sans cela le pilote NVIDIA ne se charge pas."
            fi
        fi
    fi

    dnf_install "${pkgs[@]}"
    info "Compilation du module NVIDIA (2 à 5 minutes)..."
    as_root akmods --force || true
    local newest
    newest="$(rpm -q kernel-core --qf '%{VERSION}-%{RELEASE}.%{ARCH}\n' 2>/dev/null | sort -V | tail -n1 || true)"
    if [[ -n "$newest" && "$newest" != "$(uname -r)" ]]; then
        as_root akmods --force --kernels "$newest" || true
    fi
    if ((!DRY_RUN)) && ! modinfo -F version nvidia -k "${newest:-$(uname -r)}" >/dev/null 2>&1; then
        warn "Le module NVIDIA n'est pas encore prêt : il sera compilé au prochain démarrage."
        note "Premier démarrage après installation NVIDIA : laissez 5 minutes à l'écran noir le temps de la compilation."
    fi
    REBOOT_NEEDED=1
    ok "pilote NVIDIA installé ($mode)"
}

install_gaming() {
    step "Steam, Wine, Lutris et outils de jeu"
    # steam tire toute la pile graphique 32 bits (Mesa, Vulkan) nécessaire aux jeux.
    dnf_install steam steam-devices \
        wine wine-dxvk winetricks ntsync-autoload \
        lutris gamemode mangohud gamescope goverlay vulkan-tools \
        icoutils desktop-file-utils xdg-utils efibootmgr python3 \
        cabextract unzip 7zip
    swap_codecs_i686

    # Charge ntsync tout de suite (synchronisation rapide pour Wine 11 / Proton 11).
    if ((!IMAGE_MODE)) && modinfo ntsync >/dev/null 2>&1 && [[ ! -e /dev/ntsync ]]; then
        as_root modprobe ntsync || true
    fi
    if ! installed ntsync-autoload && modinfo ntsync >/dev/null 2>&1; then
        echo ntsync | write_root_file /etc/modules-load.d/ntsync.conf
    fi
    ok "outils de jeu installés"
}

install_umu() {
    step "umu-launcher (Proton en dehors de Steam)"
    if command -v umu-run >/dev/null 2>&1 || installed umu-launcher; then
        ok "umu-launcher déjà présent"
        return 0
    fi
    # 1) dépôts Fedora, s'il y est un jour.
    if dnf -q repoquery --available umu-launcher 2>/dev/null | grep -q umu-launcher; then
        dnf_install umu-launcher
        ok "umu-launcher installé depuis les dépôts"
        return 0
    fi
    # 2) le RPM publié par le projet pour cette version de Fedora.
    local rpm_url="$UMU_RELEASES/$UMU_VERSION/umu-launcher-$UMU_VERSION.fc$FEDORA_VERSION.x86_64.rpm"
    if curl -fsIL --max-time 20 -o /dev/null "$rpm_url" && as_root dnf install -y "$rpm_url"; then
        ok "umu-launcher $UMU_VERSION installé"
        return 0
    fi
    # 3) la version "zipapp" autonome.
    local tar_url="$UMU_RELEASES/$UMU_VERSION/umu-launcher-$UMU_VERSION-zipapp.tar"
    local dest="$PREFIX/lib/umu-launcher"
    info "Pas de RPM pour Fedora $FEDORA_VERSION : installation de la version autonome."
    if ((DRY_RUN)); then
        printf '    [simulation] télécharger %s dans %s\n' "$tar_url" "$dest"
        return 0
    fi
    local tmp
    tmp="$(mktemp -d)"
    curl -fL --max-time 300 -o "$tmp/umu.tar" "$tar_url" || die "téléchargement d'umu-launcher impossible : $tar_url"
    tar -xf "$tmp/umu.tar" -C "$tmp"
    local run
    run="$(find "$tmp" -name umu-run -type f | head -n1)"
    [[ -n "$run" ]] || die "umu-run introuvable dans l'archive téléchargée"
    as_root mkdir -p "$dest"
    as_root cp -a "$(dirname "$run")/." "$dest/"
    as_root ln -sf "$dest/umu-run" "$PREFIX/bin/umu-run"
    rm -rf -- "$tmp"
    ok "umu-launcher $UMU_VERSION installé dans $dest"
}

install_dev() {
    ((DO_DEV)) || return 0
    step "Compilateur Windows (fabrique des .exe depuis Fedora)"
    dnf_install mingw64-gcc mingw64-gcc-c++ mingw32-gcc mingw32-gcc-c++ mingw64-winpthreads-static
    ok "MinGW installé : x86_64-w64-mingw32-gcc prog.c -o prog.exe"
}

setup_flatpak() {
    ((DO_FLATPAK)) || return 0
    step "Applications Flathub (Heroic, Bottles, ProtonPlus, Flatseal)"
    dnf_install flatpak
    as_root flatpak remote-add --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo
    # Fedora peut avoir ajouté Flathub filtré ou désactivé.
    as_root flatpak remote-modify --no-filter --enable flathub
    local app
    for app in "${FLATPAKS[@]}"; do
        if flatpak info --system "$app" >/dev/null 2>&1; then
            ok "$app déjà installé"
        else
            as_root flatpak install -y --noninteractive --system flathub "$app" || warn "échec : $app"
        fi
    done
    ok "applications Flatpak installées"
}

# Retrouve les fichiers binfmt fournis par le paquet wine-systemd (enregistrement "MZ" global).
wine_binfmt_files() {
    rpm -ql wine-systemd 2>/dev/null | grep '/binfmt\.d/.*\.conf$' || true
}

install_fxw() {
    step "Intégration des .exe (double-clic, ./programme.exe, menu)"
    local share="$PREFIX/share"
    [[ -f "$SCRIPT_DIR/files/fxw" ]] || die "fichier manquant : $SCRIPT_DIR/files/fxw"

    write_root_file "$PREFIX/bin/fxw" 755 <"$SCRIPT_DIR/files/fxw"
    write_root_file "$share/icons/hicolor/scalable/apps/fxw.svg" <"$SCRIPT_DIR/files/fxw.svg"
    sed "s|@BIN@|$PREFIX/bin|g" "$SCRIPT_DIR/files/fxw.desktop" | write_root_file "$share/applications/fxw.desktop"

    # Double-clic : fxw devient l'application par défaut pour les fichiers Windows.
    local types=(application/x-msdownload application/vnd.microsoft.portable-executable
        application/x-ms-dos-executable application/x-dosexec application/x-ms-ne-executable
        application/x-msi application/x-ms-shortcut application/x-bat)
    set_default_mime_system /etc/xdg/mimeapps.list fxw.desktop "${types[@]}"
    as_user xdg-mime default fxw.desktop "${types[@]}" || warn "xdg-mime indisponible : association par utilisateur non faite"

    # Terminal : "./setup.exe" passe par fxw (binfmt_misc, sur l'extension .exe).
    local binfmt_dir=/etc/binfmt.d
    ((IMAGE_MODE)) && binfmt_dir=/usr/lib/binfmt.d
    printf '%s\n' "# Fedora X Windows : ./programme.exe est lancé par fxw (Proton ou Wine)" \
        ":fxw-exe:E::exe::$PREFIX/bin/fxw:" ":fxw-EXE:E::EXE::$PREFIX/bin/fxw:" |
        write_root_file "$binfmt_dir/fxw.conf"
    # wine-systemd enregistre /usr/bin/wine pour TOUS les fichiers commençant par "MZ" :
    # on le neutralise (un fichier du même nom dans /etc l'emporte) pour garder un seul chemin.
    local f
    while IFS= read -r f; do
        [[ -n "$f" ]] || continue
        echo "# Neutralisé par Fedora X Windows : voir $binfmt_dir/fxw.conf" |
            write_root_file "/etc/binfmt.d/$(basename "$f")"
    done < <(wine_binfmt_files)

    if ((!IMAGE_MODE)); then
        as_root systemctl restart systemd-binfmt.service || warn "systemd-binfmt n'a pas redémarré"
        as_root update-desktop-database "$share/applications" || true
        as_root gtk-update-icon-cache -q -f "$share/icons/hicolor" 2>/dev/null || true
    fi

    # Raccourci « Redémarrer sous Windows » seulement si Windows est installé à côté
    # (dans une image, on ne peut pas le savoir : la commande « fxw windows » reste disponible).
    if ((!IMAGE_MODE)) && efibootmgr 2>/dev/null | grep -q "Windows Boot Manager"; then
        sed "s|@BIN@|$PREFIX/bin|g" "$SCRIPT_DIR/files/fxw-windows.desktop" |
            write_root_file "$share/applications/fxw-windows.desktop"
        ok "Windows détecté : raccourci « Redémarrer sous Windows » ajouté"
    fi
    ok "fxw installé dans $PREFIX/bin"
}

# Fixe l'application par défaut de types MIME dans un mimeapps.list, sans écraser le reste.
set_default_mime_system() {
    local file="$1" app="$2"
    shift 2
    if ((DRY_RUN)); then
        printf '    [simulation] %s : %s par défaut pour %s types\n' "$file" "$app" "$#"
        return 0
    fi
    local tmp src=/dev/null
    tmp="$(mktemp)"
    [[ -f "$file" ]] && src="$file"
    awk -v app="$app" -v types="$*" '
        BEGIN { n = split(types, t, " "); for (i = 1; i <= n; i++) want[t[i]] = 1 }
        function emit() { for (i = 1; i <= n; i++) print t[i] "=" app ";"; done = 1 }
        /^\[.*\]$/ { if (sec == "[Default Applications]" && !done) emit(); sec = $0; print; next }
        sec == "[Default Applications]" { split($0, kv, "="); if (kv[1] in want) next }
        { print }
        END { if (!done) { if (sec != "[Default Applications]") print "[Default Applications]"; emit() } }
    ' "$src" >"$tmp"
    write_root_file "$file" <"$tmp"
    rm -f -- "$tmp"
}

uninstall() {
    step "Retrait de l'intégration Fedora X Windows"
    local p
    for p in /usr/local /usr; do
        [[ -f "$p/bin/fxw" ]] || continue
        as_root rm -f "$p/bin/fxw" "$p/share/applications/fxw.desktop" \
            "$p/share/applications/fxw-windows.desktop" "$p/share/icons/hicolor/scalable/apps/fxw.svg"
    done
    as_root rm -f /etc/binfmt.d/fxw.conf
    local f
    while IFS= read -r f; do
        f="/etc/binfmt.d/$(basename "$f")"
        if grep -qs "Fedora X Windows" "$f"; then
            as_root rm -f "$f"
        fi
    done < <(wine_binfmt_files)
    if [[ -f /etc/xdg/mimeapps.list ]]; then
        as_root sed -i '/=fxw\.desktop;$/d' /etc/xdg/mimeapps.list
    fi
    as_root systemctl restart systemd-binfmt.service || true
    ok "fxw retiré. Les paquets (Steam, Wine...) et vos jeux (~/Games/FXW) sont conservés."
    info "Pour retirer aussi les paquets : sudo dnf remove steam wine lutris umu-launcher"
}

summary() {
    step "Terminé !"
    cat <<EOF
    Ce qui est prêt :
      - Steam (activez Proton : Steam > Paramètres > Compatibilité > « Activer Steam Play
        pour tous les autres titres »)
      - double-clic sur un .exe / .msi : lancé avec Proton par « fxw »
      - dans un terminal : chmod +x setup.exe && ./setup.exe
      - les programmes installés apparaissent dans le menu des applications
      - Heroic (Epic, GOG), Lutris (Battle.net, EA, Ubisoft), Bottles (logiciels)
      - compilateur Windows : x86_64-w64-mingw32-gcc prog.c -o prog.exe && ./prog.exe
      - vérification : fxw doctor

    Rappel : les jeux avec un anti-triche noyau (Valorant, League of Legends,
    Fortnite, Call of Duty...) ne fonctionnent pas sous Linux. Pour eux : double
    démarrage avec Windows, puis « fxw windows » pour y redémarrer une fois.
EOF
    local n
    for n in "${NOTES[@]}"; do
        printf '\n    %s!%s %s\n' "$C_YELLOW" "$C_OFF" "$n"
    done
    if ((REBOOT_NEEDED)) && ((!IMAGE_MODE)); then
        printf '\n    %sRedémarrez l\x27ordinateur pour terminer.%s\n' "$C_YELLOW" "$C_OFF"
    fi
    if [[ -n "${LOG_FILE:-}" ]]; then
        printf '\n    Journal : %s\n' "$LOG_FILE"
    fi
}

main() {
    parse_args "$@"

    if ((!DRY_RUN)) && ((!IMAGE_MODE)); then
        LOG_FILE="$(mktemp /tmp/fxw-install-XXXXXX.log)"
        exec > >(tee -a "$LOG_FILE") 2>&1
    fi

    printf '%sFedora X Windows %s%s\n' "$C_BLUE" "$FXW_VERSION" "$C_OFF"
    ((DRY_RUN)) && warn "mode simulation : rien ne sera modifié"

    check_system
    if ((UNINSTALL)); then
        uninstall
        return
    fi
    detect_gpus
    info "Cartes graphiques :${GPU_VENDORS:- aucune détectée}"

    if ((!ASSUME_YES)); then
        echo
        info "Ce script va : activer RPM Fusion, mettre à jour le système, installer les"
        info "codecs, le pilote graphique, Steam, Wine, Proton (umu), Lutris, MinGW,"
        info "des applications Flathub, et configurer le lancement des .exe."
        ask "Continuer ?" || exit 0
    fi

    setup_repos
    system_update
    setup_codecs
    setup_nvidia
    install_gaming
    install_umu
    install_dev
    setup_flatpak
    install_fxw
    ((IMAGE_MODE)) && as_root dnf clean all
    summary
}

# Permet de charger les fonctions sans rien lancer (tests).
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    main "$@"
fi
