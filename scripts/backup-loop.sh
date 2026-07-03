#!/bin/bash
# ==========================================================================
# Boucle de sauvegarde automatique de la base MySQL (mgh_db)
# Exécutée dans le conteneur "mgh-db-backup".
# - Lance un mysqldump compressé chaque jour à 03h00 (fuseau TZ)
# - Écrit dans /backups qui est monté sur le disque de l'HÔTE (./backups)
#   => les sauvegardes SURVIVENT à la suppression du volume mysql_data
# - Applique une rotation (garde BACKUP_KEEP_DAYS jours)
# ==========================================================================
set -euo pipefail

BACKUP_DIR="/backups"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
BACKUP_HOUR="${BACKUP_HOUR:-03}"   # heure de la sauvegarde (00-23)

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

do_backup() {
  mkdir -p "$BACKUP_DIR"
  local ts file
  ts="$(date '+%Y-%m-%d_%H-%M-%S')"
  file="${BACKUP_DIR}/${MYSQL_DATABASE}_${ts}.sql.gz"

  log "Démarrage de la sauvegarde -> ${file}"
  if mysqldump \
        --host="${MYSQL_HOST}" \
        --user="${MYSQL_USER}" \
        --password="${MYSQL_PASSWORD}" \
        --single-transaction --quick --routines --triggers --events \
        --default-character-set=utf8mb4 \
        --databases "${MYSQL_DATABASE}" \
      | gzip -9 > "${file}"; then

    # Vérifie que le dump n'est pas vide/corrompu
    if [ ! -s "${file}" ] || ! gzip -t "${file}" 2>/dev/null; then
      log "ECHEC : fichier de sauvegarde vide ou corrompu, suppression."
      rm -f "${file}"
      return 1
    fi

    log "Sauvegarde OK ($(du -h "${file}" | cut -f1))"
    # Lien symbolique vers la dernière sauvegarde valide
    ln -sf "$(basename "${file}")" "${BACKUP_DIR}/latest.sql.gz"

    # Rotation : supprime les sauvegardes de plus de KEEP_DAYS jours
    find "${BACKUP_DIR}" -name "${MYSQL_DATABASE}_*.sql.gz" -type f -mtime "+${KEEP_DAYS}" -print -delete
  else
    log "ECHEC du mysqldump, suppression du fichier partiel."
    rm -f "${file}"
    return 1
  fi
}

seconds_until_backup_hour() {
  local now today_target tomorrow_target
  now="$(date +%s)"
  today_target="$(date -d "today ${BACKUP_HOUR}:00:00" +%s)"
  if [ "${now}" -lt "${today_target}" ]; then
    echo "$(( today_target - now ))"
  else
    tomorrow_target="$(date -d "tomorrow ${BACKUP_HOUR}:00:00" +%s)"
    echo "$(( tomorrow_target - now ))"
  fi
}

log "Service de sauvegarde démarré (base=${MYSQL_DATABASE}, heure=${BACKUP_HOUR}h, rétention=${KEEP_DAYS}j, TZ=${TZ:-?})"

# Sauvegarde immédiate au démarrage pour garantir qu'il existe toujours au moins un dump
do_backup || log "La sauvegarde initiale a échoué (MySQL pas encore prêt ?), on continue la boucle."

while true; do
  sleep_sec="$(seconds_until_backup_hour)"
  log "Prochaine sauvegarde dans ${sleep_sec}s (à ${BACKUP_HOUR}h00)."
  sleep "${sleep_sec}"
  do_backup || log "La sauvegarde a échoué, nouvelle tentative demain."
done
