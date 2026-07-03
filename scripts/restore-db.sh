#!/bin/bash
# ==========================================================================
# Restauration de la base MySQL (mgh_db) depuis une sauvegarde.
# À lancer sur le VPS, à la racine du projet :
#     ./scripts/restore-db.sh                       # restaure la dernière sauvegarde
#     ./scripts/restore-db.sh backups/mgh_db_....sql.gz   # restaure un fichier précis
# ==========================================================================
set -euo pipefail

DB_CONTAINER="mgh-mysql"
BACKUP_DIR="./backups"
FILE="${1:-${BACKUP_DIR}/latest.sql.gz}"

if [ ! -f "${FILE}" ]; then
  echo "Fichier de sauvegarde introuvable : ${FILE}"
  echo ""
  echo "Sauvegardes disponibles :"
  ls -lh "${BACKUP_DIR}"/*.sql.gz 2>/dev/null || echo "  (aucune)"
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "Le conteneur ${DB_CONTAINER} n'est pas démarré. Lancez d'abord : docker compose up -d mysql"
  exit 1
fi

echo "ATTENTION : cela va ECRASER les données actuelles de la base par le contenu de :"
echo "   ${FILE}"
read -r -p "Confirmer la restauration ? (tapez OUI) : " confirm
if [ "${confirm}" != "OUI" ]; then
  echo "Restauration annulée."
  exit 0
fi

echo "Restauration en cours..."
gunzip -c "${FILE}" | docker exec -i "${DB_CONTAINER}" sh -c 'exec mysql -uroot -p"${MYSQL_ROOT_PASSWORD}"'
echo "Restauration terminée avec succès."
