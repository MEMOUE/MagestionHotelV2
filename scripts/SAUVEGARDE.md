# Sauvegarde & restauration de la base de données

Objectif : **ne jamais perdre les données des utilisateurs**, même si le conteneur
MySQL tombe en panne ou si le volume Docker `mysql_data` est supprimé.

## Comment ça marche

Un conteneur `mgh-db-backup` tourne en permanence dans la stack et lance un
`mysqldump` compressé **chaque jour à 03h00** (fuseau `Africa/Abidjan`).

Les sauvegardes sont écrites dans le dossier **`./backups`** sur le disque du VPS,
c'est-à-dire **en dehors des volumes Docker**. Conséquence :

- `docker compose down -v` supprime le volume `mysql_data`… mais **pas** `./backups`.
- Si le conteneur MySQL est détruit, les dumps restent sur l'hôte.

Rétention : les 14 derniers jours sont conservés (variable `BACKUP_KEEP_DAYS`).
Le fichier `backups/latest.sql.gz` pointe toujours vers la dernière sauvegarde valide.

## Déploiement sur le VPS

```bash
git pull
docker compose up -d db-backup     # démarre uniquement le nouveau service
docker logs -f mgh-db-backup        # vérifier : "Sauvegarde OK (...)"
ls -lh backups/                     # un fichier mgh_db_AAAA-MM-JJ_...sql.gz doit apparaître
```

Une sauvegarde est lancée immédiatement au démarrage, puis chaque jour à 3h.

## Forcer une sauvegarde manuelle

```bash
docker exec mgh-db-backup bash -c '
  mysqldump --host=mysql --user=root --password="$MYSQL_PASSWORD" \
    --single-transaction --routines --triggers --events \
    --databases "$MYSQL_DATABASE" | gzip > /backups/manuel_$(date +%F_%H-%M).sql.gz'
```

## Restaurer les données

```bash
./scripts/restore-db.sh                              # restaure la dernière sauvegarde
./scripts/restore-db.sh backups/mgh_db_2026-07-03_03-00-00.sql.gz   # un fichier précis
```

Le script demande une confirmation (`OUI`) car il **écrase** les données actuelles.

---

## Renforcement recommandé (fortement conseillé)

### 1. Copie hors-site
Les sauvegardes ci-dessus protègent contre la perte du volume/conteneur, **pas contre
la perte du VPS entier**. Ajoutez une copie ailleurs (autre serveur, S3, Google Drive…).
Exemple avec `rclone` en cron sur le VPS :

```bash
0 4 * * * rclone copy /chemin/vers/projet/backups remote:mgh-backups --max-age 25h
```

### 2. Rendre le volume MySQL résistant à `down -v` (optionnel)
Pour que même les données « live » ne soient jamais effacées par un `docker compose down -v`,
on peut remplacer le volume nommé par un bind mount hôte. **Migration requise** (ne pas
faire à chaud sans sauvegarde) :

```yaml
# service mysql :
    volumes:
      - /opt/mgh/mysql-data:/var/lib/mysql   # au lieu de mysql_data:/var/lib/mysql
```
Un bind mount hôte n'est jamais supprimé par `docker compose down -v`.
