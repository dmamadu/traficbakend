#!/usr/bin/env bash
# Déploie une version d'image INVODIS sur le VPS, avec retour automatique à l'ancienne en cas d'échec.
#
#   ./deploy.sh backend  1.4.0
#   ./deploy.sh frontend sha-a1b2c3d
#
# Lancé à la main sur le VPS, ou par les workflows « Deploy backend / Deploy frontend » via SSH.
set -euo pipefail

DIR="/opt/invodis"
SERVICE="${1:-}"
VERSION="${2:-}"

usage() { echo "Usage : $0 backend|frontend <version>"; exit 1; }

case "$SERVICE" in
  backend)  VAR="BACKEND_TAG";  URL="http://127.0.0.1:8081/"; TIMEOUT=180 ;;
  frontend) VAR="FRONTEND_TAG"; URL="http://127.0.0.1:8080/"; TIMEOUT=60  ;;
  *) usage ;;
esac
[[ "$VERSION" =~ ^[A-Za-z0-9._-]{1,64}$ ]] || { echo "Version invalide : '$VERSION'"; usage; }

cd "$DIR"
exec 9>"$DIR/.deploy.lock"
# Un seul déploiement à la fois (backend et frontend modifient le même .env) : le second attend son tour
if ! flock -n 9; then
  echo "▶ Un autre déploiement est en cours : attente (10 min max)…"
  flock -w 600 9 || { echo "✖ Toujours occupé après 10 min, abandon."; exit 1; }
fi

OLD="$(grep -E "^${VAR}=" .env | cut -d= -f2- || true)"
echo "▶ $SERVICE : ${OLD:-aucune} → $VERSION"

set_tag() { sed -i "s/^${VAR}=.*/${VAR}=$1/" .env; }

# Le service répond (n'importe quel code HTTP < 500 : le backend répond 401 sans jeton, c'est normal)
healthy() {
  local deadline=$((SECONDS + TIMEOUT)) code
  while (( SECONDS < deadline )); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$URL" || true)"
    if [[ "$code" =~ ^[1-4][0-9][0-9]$ ]]; then echo "  réponse HTTP $code"; return 0; fi
    sleep 5
  done
  return 1
}

# Sauvegarde de la base avant chaque backend : ddl-auto=update modifie le schéma au démarrage
if [[ "$SERVICE" == "backend" ]]; then
  mkdir -p backups
  FILE="backups/gestionProjet-$(date +%Y%m%d-%H%M%S)-avant-${VERSION}.sql.gz"
  echo "▶ Sauvegarde MySQL → $FILE"
  docker compose exec -T mysql sh -c \
    'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines gestionProjet' \
    | gzip > "$FILE"
  # On garde les 15 dernières
  ls -1t backups/*.sql.gz | tail -n +16 | xargs -r rm --
fi

set_tag "$VERSION"
if ! docker compose pull "$SERVICE"; then
  echo "✖ Image introuvable : $SERVICE:$VERSION"
  set_tag "$OLD"
  exit 1
fi
docker compose up -d --no-deps "$SERVICE"

echo "▶ Vérification (jusqu'à ${TIMEOUT}s)…"
if ! healthy; then
  echo "✖ $SERVICE $VERSION ne répond pas → retour à ${OLD}"
  docker compose logs --tail=60 "$SERVICE" || true
  if [[ -n "$OLD" ]]; then
    set_tag "$OLD"
    docker compose up -d --no-deps "$SERVICE"
  fi
  echo "$(date -Is) ECHEC $SERVICE $VERSION (retour $OLD)" >> deploy.log
  exit 1
fi

echo "$(date -Is) OK $SERVICE $OLD → $VERSION" >> deploy.log
docker image prune -f >/dev/null
echo "✔ $SERVICE $VERSION déployé"
