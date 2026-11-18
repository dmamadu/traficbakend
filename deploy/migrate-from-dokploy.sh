#!/usr/bin/env bash
# Migration unique : production Dokploy -> nouvelle pile /opt/invodis (docker compose + Caddy).
# À lancer en tant que « deploy », dans /opt/invodis.
#
#   ./migrate-from-dokploy.sh check     état des lieux (ne modifie rien)
#   ./migrate-from-dokploy.sh db        copie la base de prod (à chaud) dans le nouveau MySQL + compare les comptes
#   ./migrate-from-dokploy.sh files     copie les fichiers du MinIO de prod dans le nouveau MinIO + compare
#   ./migrate-from-dokploy.sh start     démarre backend + frontend de la nouvelle pile (sans HTTPS) et les vérifie
#   ./migrate-from-dokploy.sh bascule   COUPURE : arrête la prod Dokploy, recopie base + fichiers, passe Traefik -> Caddy
#   ./migrate-from-dokploy.sh retour    revient à Dokploy (Caddy arrêté, Traefik + ancien backend relancés)
#
# Répétition sans coupure : check, db, files, start. Jour J : bascule.
set -euo pipefail
cd /opt/invodis

OLD_DB_PREFIX="invodis-mysql-tedgme"
OLD_BACK_SERVICE="invodis-backend-at5fcc"
OLD_MINIO="minio"
OLD_MINIO_URL="http://69.62.125.51:9000"
TRAEFIK="dokploy-traefik"
NET="invodis_default"
BACKEND_TAG_DEFAULT="sha-999a542"
FRONTEND_TAG_DEFAULT="sha-de078b6"

ok()   { echo "  ✔ $*"; }
warn() { echo "  ⚠ $*"; }
die()  { echo "  ✖ $*" >&2; exit 1; }
step() { echo; echo "▶ $*"; }

container_of() { docker ps --format '{{.Names}}' | grep -E "^$1(\.|$)" | head -1 || true; }
env_of()       { docker inspect "$1" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n "s/^$2=//p" | head -1; }
dotenv()       { sed -n "s/^$1=//p" .env | head -1; }
set_dotenv()   { sed -i "s|^$1=.*|$1=$2|" .env; }

OLD_DB="$(container_of "$OLD_DB_PREFIX")"
OLD_BACK="$(container_of "$OLD_BACK_SERVICE")"

# Identifiants du MinIO de prod : ceux du backend, sinon les identifiants root du conteneur minio
old_minio_creds() {
  [[ -n "${OLD_MK:-}" && -n "${OLD_MS:-}" ]] && return 0   # déjà lus (bascule : avant l'arrêt du backend)
  OLD_MK=""; OLD_MS=""
  if [[ -n "$OLD_BACK" ]]; then
    OLD_MK="$(env_of "$OLD_BACK" MINIO_ACCESS_KEY)"; OLD_MS="$(env_of "$OLD_BACK" MINIO_SECRET_KEY)"
  fi
  if [[ -z "$OLD_MK" || -z "$OLD_MS" ]]; then
    OLD_MK="$(env_of "$OLD_MINIO" MINIO_ROOT_USER)";     OLD_MS="$(env_of "$OLD_MINIO" MINIO_ROOT_PASSWORD)"
  fi
  if [[ -z "$OLD_MK" || -z "$OLD_MS" ]]; then
    OLD_MK="$(env_of "$OLD_MINIO" MINIO_ACCESS_KEY)";    OLD_MS="$(env_of "$OLD_MINIO" MINIO_SECRET_KEY)"
  fi
  [[ -n "$OLD_MK" && -n "$OLD_MS" ]] || die "identifiants du MinIO de prod introuvables"
}

old_bucket() {
  [[ -n "${OLD_BUCKET:-}" ]] && { echo "$OLD_BUCKET"; return 0; }
  local b=""
  [[ -n "$OLD_BACK" ]] && b="$(env_of "$OLD_BACK" MINIO_BUCKET_NAME)"
  echo "${b:-$(dotenv MINIO_BUCKET_NAME)}"
}

wait_healthy() { # service, délai max en s
  local id i
  for ((i = 0; i < $2; i += 3)); do
    id="$(docker compose ps -q "$1")"
    [[ -n "$id" && "$(docker inspect -f '{{.State.Health.Status}}' "$id" 2>/dev/null)" == healthy ]] && return 0
    sleep 3
  done
  return 1
}

wait_http() { # url, délai max en s ; succès = code HTTP 1xx-4xx
  local code i
  for ((i = 0; i < $2; i += 3)); do
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$1" || true)"
    [[ "$code" =~ ^[1-4][0-9][0-9]$ ]] && { echo "$code"; return 0; }
    sleep 3
  done
  return 1
}

# Comptage exact des lignes de chaque table (même requête sur l'ancienne et la nouvelle base)
read -r -d '' COUNT_SQL <<'SQL' || true
SELECT CONCAT('SELECT ''', table_name, ''', COUNT(*) FROM `', table_name, '`;')
FROM information_schema.tables
WHERE table_schema = 'gestionProjet' AND table_type = 'BASE TABLE' ORDER BY table_name;
SQL

count_rows() { # conteneur
  docker exec -i "$1" sh -c 'mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" -e "$1" 2>/dev/null \
    | mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" gestionProjet 2>/dev/null' sh "$COUNT_SQL"
}

mc_run() { # script mc ; alias « old » et « new » déjà définis
  old_minio_creds
  OLD_MK="$OLD_MK" OLD_MS="$OLD_MS" NEW_MK="$(dotenv MINIO_ACCESS_KEY)" NEW_MS="$(dotenv MINIO_SECRET_KEY)" \
  docker run --rm --network "$NET" -e OLD_MK -e OLD_MS -e NEW_MK -e NEW_MS -e BUCKET="$2" -e OLD_URL="$OLD_MINIO_URL" \
    --entrypoint sh minio/mc:latest -c '
      mc alias set old "$OLD_URL" "$OLD_MK" "$OLD_MS" >/dev/null &&
      mc alias set new http://minio:9000 "$NEW_MK" "$NEW_MS" >/dev/null && '"$1"
}

# ---------------------------------------------------------------------------------------------------------

cmd_check() {
  step "Ancienne production (Dokploy)"
  [[ -n "$OLD_DB" ]]   && ok "base      : $OLD_DB ($(docker exec "$OLD_DB" mysql --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1))" || warn "base introuvable"
  [[ -n "$OLD_BACK" ]] && ok "backend   : $OLD_BACK" || warn "backend introuvable (arrêté ?)"
  docker ps --format '{{.Names}}' | grep -qx "$OLD_MINIO" && ok "minio     : $OLD_MINIO" || warn "minio introuvable"
  docker ps --format '{{.Names}}' | grep -qx "$TRAEFIK"   && ok "traefik   : $TRAEFIK (restart=$(docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$TRAEFIK"))" || warn "traefik arrêté"
  docker service ls --format '{{.Name}}' 2>/dev/null | grep -qx "$OLD_BACK_SERVICE" && ok "service   : $OLD_BACK_SERVICE ($(docker service ls -f name="$OLD_BACK_SERVICE" --format '{{.Replicas}}'))" || warn "service $OLD_BACK_SERVICE introuvable"
  old_minio_creds && ok "identifiants MinIO de prod trouvés"
  local b; b="$(old_bucket)"; ok "bucket de prod : $b"
  [[ "$b" == "$(dotenv MINIO_BUCKET_NAME)" ]] || warn "MINIO_BUCKET_NAME du .env ($(dotenv MINIO_BUCKET_NAME)) ≠ prod : 'start' l'alignera"

  step "Nouvelle pile"
  docker compose ps --format '{{.Service}}\t{{.Status}}' 2>/dev/null | sed 's/^/  /' || true
  [[ -z "$(docker compose ps -q 2>/dev/null)" ]] && ok "rien de démarré pour l'instant"
  return 0
}

cmd_db() {
  [[ -n "$OLD_DB" ]] || die "base de prod introuvable"
  mkdir -p backups
  local f="backups/dokploy-$(date +%Y%m%d-%H%M%S).sql.gz"

  step "Export de la base de prod (à chaud, cohérent) → $f"
  docker exec "$OLD_DB" sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --no-tablespaces gestionProjet 2>/dev/null' | gzip > "$f"
  zcat "$f" | tail -1 | grep -q 'Dump completed' || die "export incomplet ($f)"
  ok "export complet : $(du -h "$f" | cut -f1)"

  step "Démarrage du nouveau MySQL"
  docker compose up -d mysql >/dev/null 2>&1
  wait_healthy mysql 120 || die "le nouveau MySQL ne démarre pas (docker compose logs mysql)"
  local NEW_DB; NEW_DB="$(docker compose ps -q mysql)"
  ok "prêt"

  step "Import (la base gestionProjet de la nouvelle pile est remplacée)"
  docker exec -i "$NEW_DB" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null -e "DROP DATABASE IF EXISTS gestionProjet; CREATE DATABASE gestionProjet;"'
  zcat "$f" | docker exec -i "$NEW_DB" sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" gestionProjet 2>/dev/null' || die "échec de l'import"
  ok "importé"

  step "Comparaison ligne à ligne (prod / nouvelle)"
  count_rows "$OLD_DB" > /tmp/invodis-old-counts
  count_rows "$NEW_DB" > /tmp/invodis-new-counts
  local tables total
  tables="$(wc -l < /tmp/invodis-old-counts)"; total="$(awk '{s+=$2} END {print s+0}' /tmp/invodis-old-counts)"
  if diff -q /tmp/invodis-old-counts /tmp/invodis-new-counts >/dev/null; then
    ok "identiques : $tables tables, $total lignes"
  else
    warn "écarts (une écriture en prod pendant la copie suffit à en créer) :"
    diff /tmp/invodis-old-counts /tmp/invodis-new-counts | sed 's/^/    /' | head -20
  fi
  for t in user project; do grep -E "^$t\s" /tmp/invodis-new-counts | awk '{printf "    %-10s %s lignes\n", $1, $2}' || true; done
}

cmd_files() {
  local b; b="$(old_bucket)"
  step "Démarrage du nouveau MinIO"
  docker compose up -d minio >/dev/null 2>&1
  sleep 5; ok "démarré"

  step "Copie du bucket « $b » (prod → nouvelle pile)"
  mc_run 'mc mb --ignore-existing "new/$BUCKET" >/dev/null && mc mirror --overwrite --remove --quiet "old/$BUCKET" "new/$BUCKET" >/dev/null &&
          echo "  prod     : $(mc ls -r --summarize "old/$BUCKET" | tail -2 | tr "\n" " ")" &&
          echo "  nouvelle : $(mc ls -r --summarize "new/$BUCKET" | tail -2 | tr "\n" " ")"' "$b"
}

cmd_start() {
  local bt ft b
  bt="${1:-$BACKEND_TAG_DEFAULT}"; ft="${2:-$FRONTEND_TAG_DEFAULT}"; b="$(old_bucket)"
  step "Versions : backend $bt, frontend $ft, bucket $b"
  set_dotenv BACKEND_TAG "$bt"; set_dotenv FRONTEND_TAG "$ft"; set_dotenv MINIO_BUCKET_NAME "$b"

  docker compose up -d backend frontend >/dev/null 2>&1
  local code
  code="$(wait_http http://127.0.0.1:8081/ 180)" && ok "backend répond (HTTP $code, 401 attendu)" || { docker compose logs --tail=40 backend; die "le backend ne répond pas"; }
  code="$(wait_http http://127.0.0.1:8080/ 60)"  && ok "frontend répond (HTTP $code)"           || die "le frontend ne répond pas"

  local logs; logs="$(docker compose logs backend 2>&1)"
  grep -q 'Started GestionProjetApplication' <<<"$logs" && ok "Spring Boot démarré"
  grep -q 'Impossible de vérifier/créer le bucket' <<<"$logs" && warn "le backend n'accède pas à MinIO" || ok "MinIO accessible"
  echo "  erreurs dans les logs : $(grep -c ' ERROR ' <<<"$logs" || true)"
}

cmd_bascule() {
  echo "COUPURE DE LA PRODUCTION : arrêt du backend Dokploy, copie finale, Traefik → Caddy."
  read -r -p "Taper OUI pour continuer : " r; [[ "$r" == OUI ]] || die "annulé"

  docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$TRAEFIK" > .traefik-restart-policy 2>/dev/null || true
  [[ -n "$(docker compose ps -q backend 2>/dev/null)" ]] || die "lancer d'abord la répétition (db, files, start)"
  # À lire tant que le backend Dokploy tourne : ses variables donnent le bucket et les identifiants MinIO
  old_minio_creds; OLD_BUCKET="$(old_bucket)"

  step "Arrêt du backend Dokploy (plus aucune écriture)"
  docker service scale -d "$OLD_BACK_SERVICE=0" >/dev/null
  sleep 10; OLD_BACK=""; ok "arrêté"

  cmd_db
  cmd_files

  step "Redémarrage du backend de la nouvelle pile sur la base finale"
  docker compose restart backend >/dev/null 2>&1
  wait_http http://127.0.0.1:8081/ 180 >/dev/null && ok "backend prêt" || die "backend KO : ./migrate-from-dokploy.sh retour"

  step "Traefik → Caddy"
  docker update --restart=no "$TRAEFIK" >/dev/null
  docker stop "$TRAEFIK" >/dev/null && ok "Traefik arrêté (ne redémarrera plus au reboot)"
  docker compose up -d caddy >/dev/null 2>&1 && ok "Caddy démarré"

  step "Certificats HTTPS (jusqu'à 2 min)"
  local i
  for ((i = 0; i < 120; i += 5)); do
    [[ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 https://api.invodis.com/ || true)" == 401 ]] && break
    sleep 5
  done
  echo "  https://invodis.com      → $(curl -s -o /dev/null -w '%{http_code}' --max-time 10 https://invodis.com/ || true)  (200 attendu)"
  echo "  https://www.invodis.com  → $(curl -s -o /dev/null -w '%{http_code}' --max-time 10 https://www.invodis.com/ || true)  (200 attendu)"
  echo "  https://api.invodis.com  → $(curl -s -o /dev/null -w '%{http_code}' --max-time 10 https://api.invodis.com/ || true)  (401 attendu)"
  echo
  echo "Tester maintenant dans le navigateur : connexion, un tableau de bord, une fiche PAP avec document."
  echo "En cas de problème : ./migrate-from-dokploy.sh retour"
}

cmd_retour() {
  echo "Retour à Dokploy. Les saisies faites sur la nouvelle pile depuis la bascule NE SERONT PAS reprises."
  read -r -p "Taper OUI pour continuer : " r; [[ "$r" == OUI ]] || die "annulé"
  docker compose stop caddy backend >/dev/null 2>&1 && ok "Caddy et nouveau backend arrêtés"
  docker update --restart="$(cat .traefik-restart-policy 2>/dev/null || echo always)" "$TRAEFIK" >/dev/null
  docker start "$TRAEFIK" >/dev/null && ok "Traefik relancé"
  docker service scale -d "$OLD_BACK_SERVICE=1" >/dev/null && ok "backend Dokploy relancé"
  echo "  vérifier https://invodis.com dans 1 à 2 minutes"
}

case "${1:-}" in
  check)   cmd_check ;;
  db)      cmd_db ;;
  files)   cmd_files ;;
  start)   shift; cmd_start "$@" ;;
  bascule) cmd_bascule ;;
  retour)  cmd_retour ;;
  *) sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
