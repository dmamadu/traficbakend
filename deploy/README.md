# Déploiement INVODIS

```
push sur main ──► Build backend|frontend ──► image sha-xxxxxxx ──► Deploy backend|frontend ──SSH──► deploy.sh
                         (auto)                                     (auto si le build a réussi)

Actions → Deploy backend|frontend → Run workflow (version) ──SSH──► /opt/invodis/deploy.sh   (manuel, retour arrière)
```

**Chaque push sur `main` part en production** dès que son *Build* a réussi. Travailler sur une branche et ne
fusionner dans `main` que ce qui est prêt. Un build en échec ne déploie rien ; un backend qui ne répond pas
après le déploiement est remplacé automatiquement par la version précédente.

## Publier une version

- Chaque push sur `main` publie une image `sha-<7 premiers caractères du commit>` et la déploie.
- Pour une version nommée : `git tag v1.4.0 && git push origin v1.4.0` → image `1.4.0` (pas de déploiement
  automatique pour les tags : le déployer avec **Run workflow**).

## Déployer à la main / revenir en arrière

GitHub → **Actions** → **Deploy backend** (ou **Deploy frontend**) → **Run workflow** → version.

Revenir en arrière = relancer avec la version précédente (voir `/opt/invodis/deploy.log`). Attention : le
push suivant sur `main` redéploiera automatiquement la dernière version de `main`.

Sur le VPS, sans passer par GitHub :

```bash
/opt/invodis/deploy.sh backend 1.4.0
/opt/invodis/deploy.sh frontend sha-a1b2c3d
```

Le script : sauvegarde MySQL (backend uniquement, 15 dernières gardées dans `backups/`) → met la version
dans `.env` → `pull` → redémarre le service → vérifie qu'il répond → sinon remet l'ancienne version.

## Mise en place du VPS (une seule fois)

1. **Docker** : `curl -fsSL https://get.docker.com | sh`, puis ajouter l'utilisateur de déploiement au groupe `docker`.

2. **Dossier** :
   ```bash
   sudo mkdir -p /opt/invodis && sudo chown $USER /opt/invodis
   # copier docker-compose.yml, deploy.sh, Caddyfile depuis ce dossier deploy/
   cp .env.example .env && chmod 600 .env   # puis renseigner des secrets NEUFS
   chmod +x deploy.sh
   ```

3. **Accès aux images privées** : créer un jeton GitHub *classic* avec **seulement** `read:packages`, puis
   ```bash
   docker login ghcr.io -u dmamadu    # coller le jeton comme mot de passe
   ```

4. **Clé SSH pour GitHub Actions** (dédiée, sans phrase de passe) :
   ```bash
   ssh-keygen -t ed25519 -f ~/.ssh/github-deploy -N "" -C github-actions-invodis
   cat ~/.ssh/github-deploy.pub >> ~/.ssh/authorized_keys
   ```

5. **Secrets GitHub**, dans **chacun des deux dépôts** (Settings → Secrets and variables → Actions) :

   | Secret | Valeur |
   |---|---|
   | `VPS_HOST` | IP ou nom du VPS |
   | `VPS_USER` | utilisateur de déploiement |
   | `VPS_SSH_KEY` | contenu de `~/.ssh/github-deploy` (clé **privée**) |
   | `VPS_PORT` | facultatif, si SSH n'est pas sur 22 |

6. **Premier démarrage** : voir la migration ci-dessous (la prod tourne encore sous Dokploy).

## Migration depuis Dokploy (une seule fois)

Dokploy occupe les ports 80/443 (Traefik) et héberge la base et les fichiers actuels. On monte la nouvelle
pile **à côté**, on y copie les données, puis on bascule. Retour possible à Dokploy jusqu'à sa désinstallation.

**Avant** : premier *Build* réussi sur les deux dépôts, étapes 1 à 5 faites, prévenir les utilisateurs
d'une coupure (~30 min) : les saisies faites entre l'export et la bascule seraient perdues.

1. **Repérer les conteneurs Dokploy** : `docker ps --format '{{.Names}}\t{{.Image}}'`
   (MySQL, MinIO, backend ; noter aussi où le backend écrit ses uploads : `docker inspect <backend>` → *Mounts*).

2. **Arrêter les écritures** : arrêter l'application backend dans l'interface Dokploy.

3. **Exporter** depuis Dokploy :
   ```bash
   docker exec <mysql-dokploy> sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines gestionProjet' \
     | gzip > /opt/invodis/backups/reprise-dokploy.sql.gz
   docker cp <minio-dokploy>:/data /opt/invodis/backups/minio-data
   docker cp <backend-dokploy>:<dossier uploads> /opt/invodis/backups/uploads
   ```

4. **Démarrer la base et MinIO de la nouvelle pile, importer** :
   ```bash
   cd /opt/invodis
   docker compose up -d mysql minio
   gunzip -c backups/reprise-dokploy.sql.gz \
     | docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" gestionProjet'
   docker compose cp backups/minio-data/. minio:/data
   docker compose restart minio
   ```

5. **Démarrer backend et frontend** (versions dans `.env`), copier les uploads, vérifier en local :
   ```bash
   docker compose up -d backend frontend
   docker compose cp backups/uploads/. backend:/data/uploads
   curl -I http://127.0.0.1:8080/  &&  curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8081/   # 401 attendu
   ```

6. **Bascule** : libérer 80/443 en arrêtant le Traefik de Dokploy, puis démarrer Caddy.
   ```bash
   docker stop dokploy-traefik 2>/dev/null || docker service scale dokploy-traefik=0   # selon la version
   docker compose up -d caddy
   docker compose logs -f caddy      # attendre « certificate obtained » pour les 3 domaines
   ```
   Tester https://invodis.com : connexion, un tableau de bord, une fiche PAP avec photo/document.

   **Retour arrière** : `docker compose stop caddy`, redémarrer `dokploy-traefik` et le backend dans Dokploy.

7. **Après quelques jours sans souci** : dernière sauvegarde, puis désinstaller Dokploy en suivant sa
   documentation officielle (services `dokploy*`, swarm, `/etc/dokploy`). Ne pas supprimer ses volumes
   avant d'être sûr que la reprise est complète.
