#!/usr/bin/env bash
# Cópia de segurança do banco do IntelliGarden (PostgreSQL do Docker Compose).
#
# Uso:
#   ./backup.sh            faz uma cópia agora
#   ./backup.sh instalar   agenda uma cópia por dia, às 3h (timer do systemd do usuário, sem sudo)
#
# Variáveis opcionais:
#   BACKUP_DIR    pasta das cópias (padrão: ~/intelligarden-backups, fora do repositório)
#   BACKUP_KEEP   quantas cópias manter (padrão: 30; as mais antigas são apagadas)
#   PG_CONTAINER  nome do container do banco (padrão: smartgarden-postgres)
#
# A cópia roda dentro do container, com o usuário e o banco dele: nenhuma senha passa pelo host.
# Restauração: veja o README desta pasta.
set -euo pipefail

INFRA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$HOME/intelligarden-backups}"
BACKUP_KEEP="${BACKUP_KEEP:-30}"
PG_CONTAINER="${PG_CONTAINER:-smartgarden-postgres}"

log() { printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }

copiar() {
  mkdir -p "$BACKUP_DIR"
  chmod 700 "$BACKUP_DIR"
  local name tmp tables size
  name="smartgarden-$(date '+%Y-%m-%d_%H%M%S').dump"
  tmp="$BACKUP_DIR/.$name.partial"
  trap 'rm -f "$tmp"' EXIT

  if ! docker inspect -f '{{.State.Running}}' "$PG_CONTAINER" 2>/dev/null | grep -q true; then
    log "ERRO: o container $PG_CONTAINER não está rodando (suba com: docker compose up -d)."
    exit 1
  fi

  log "Copiando o banco para $BACKUP_DIR/$name"
  docker exec "$PG_CONTAINER" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner' > "$tmp"

  # Só vale como cópia se o próprio PostgreSQL conseguir ler o arquivo e ele tiver as tabelas com dados.
  tables=$(docker exec -i "$PG_CONTAINER" pg_restore --list < "$tmp" | grep -c ' TABLE DATA ' || true)
  if [ "${tables:-0}" -lt 1 ]; then
    log "ERRO: a cópia saiu ilegível ou vazia; nada foi guardado."
    exit 1
  fi

  mv "$tmp" "$BACKUP_DIR/$name"
  chmod 600 "$BACKUP_DIR/$name"
  trap - EXIT
  size=$(du -h "$BACKUP_DIR/$name" | cut -f1)
  log "Cópia pronta: $name ($size, $tables tabelas com dados)."

  # Mantém só as BACKUP_KEEP cópias mais recentes.
  local old
  old=$(ls -1t "$BACKUP_DIR"/smartgarden-*.dump 2>/dev/null | tail -n +"$((BACKUP_KEEP + 1))" || true)
  if [ -n "$old" ]; then
    log "Apagando $(echo "$old" | wc -l) cópia(s) antiga(s) (mantém $BACKUP_KEEP)."
    echo "$old" | xargs -r rm -f --
  fi
}

instalar() {
  local units="$HOME/.config/systemd/user"
  mkdir -p "$units"
  cat > "$units/intelligarden-backup.service" <<EOF
[Unit]
Description=Cópia de segurança do banco do IntelliGarden

[Service]
Type=oneshot
Environment=BACKUP_DIR=$BACKUP_DIR BACKUP_KEEP=$BACKUP_KEEP PG_CONTAINER=$PG_CONTAINER
ExecStart=/usr/bin/env bash "$INFRA_DIR/backup.sh"
EOF
  cat > "$units/intelligarden-backup.timer" <<'EOF'
[Unit]
Description=Cópia diária do banco do IntelliGarden

[Timer]
# todo dia às 3h; se o computador estiver desligado nessa hora, roda assim que ele voltar
OnCalendar=*-*-* 03:00:00
Persistent=true
RandomizedDelaySec=10min

[Install]
WantedBy=timers.target
EOF
  systemctl --user daemon-reload
  systemctl --user enable --now intelligarden-backup.timer
  log "Agendado. Próxima execução:"
  systemctl --user list-timers intelligarden-backup.timer --no-pager | head -2
}

case "${1:-}" in
  "") copiar ;;
  instalar) instalar ;;
  *) echo "uso: $0 [instalar]" >&2; exit 2 ;;
esac
