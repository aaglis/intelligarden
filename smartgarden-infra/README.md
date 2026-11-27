# SmartGarden Infra

Infraestrutura local do SmartGarden, responsável pelo PostgreSQL e pelo broker MQTT Mosquitto.

## Serviços

- PostgreSQL 16 em `localhost:5432`
- Eclipse Mosquitto 2 em `localhost:1883`
- autenticação MQTT obrigatória
- persistência dos bancos e mensagens em volumes Docker

## Primeiro uso

Pré-requisitos: Docker com Compose e Bash.

```bash
cp .env.example .env
docker compose up -d
docker compose ps
```

Antes do `up`, troque no `.env` cada `troque_esta_senha` por uma senha gerada com `openssl rand -hex 16`. O Compose se recusa a subir sem `POSTGRES_PASSWORD`, `MQTT_PASSWORD` e `MQTT_DEVICE_PASSWORD`.

O Compose gera as credenciais do broker em um volume Docker privado antes de iniciar o Mosquitto. O `.env`, com as senhas em texto claro, é ignorado pelo Git.

O PostgreSQL só usa `POSTGRES_PASSWORD` ao criar o banco pela primeira vez. Para trocar a senha de um banco que já existe:

```bash
docker exec -it smartgarden-postgres psql -U smartgarden -d smartgarden -c "\password smartgarden"
```

e atualize o `.env` com a mesma senha.

## Migração do Compose antigo

Se os containers foram iniciados anteriormente pelo repositório `smartgarden-back`, remova somente os containers antes do primeiro `up` neste repositório:

```bash
docker rm -f smartgarden-postgres smartgarden-mosquitto
docker compose up -d
```

Os volumes mantêm deliberadamente os nomes antigos para preservar as leituras já armazenadas. Não use `docker volume rm` durante a migração.

O Compose pode avisar que esses volumes foram criados pelo projeto `smartgarden-back`. Esse aviso é esperado e não impede o uso; ele confirma que os dados antigos estão sendo reutilizados.

## Operação

```bash
# Estado
docker compose ps

# Logs
docker compose logs -f postgres mosquitto

# Parar sem apagar dados
docker compose down

# Recriar os serviços
docker compose up -d --force-recreate
```

Para apagar todos os dados locais:

```bash
docker compose down -v
```

Esse comando é destrutivo e remove o banco PostgreSQL e a persistência do broker. Faça uma cópia antes (veja abaixo).

## Cópia de segurança do banco

As pesagens dos vasos reais são o resultado do projeto e existem só neste banco. O script `backup.sh` faz uma cópia compactada (`pg_dump`, formato custom), confere se o próprio PostgreSQL consegue ler o arquivo e mantém as 30 cópias mais recentes em `~/intelligarden-backups`, fora do repositório. A cópia roda dentro do container, com o usuário e o banco dele, então nenhuma senha passa pelo host.

```bash
# uma cópia agora
./backup.sh

# agendar uma cópia por dia, às 3h (timer do systemd do usuário, sem sudo)
./backup.sh instalar

# ver as execuções e o próximo horário
journalctl --user -u intelligarden-backup.service -n 20 --no-pager
systemctl --user list-timers intelligarden-backup.timer
```

Variáveis opcionais: `BACKUP_DIR` (pasta das cópias), `BACKUP_KEEP` (quantas manter) e `PG_CONTAINER` (nome do container).

O timer do usuário roda enquanto você estiver com a sessão aberta. Se o computador estiver desligado às 3h, a cópia roda assim que ele voltar (`Persistent=true`). Para rodar mesmo sem ninguém logado, habilite com `loginctl enable-linger`.

Uma cópia no mesmo disco não protege contra a perda do computador. De vez em quando, copie a pasta `~/intelligarden-backups` para um pendrive ou para a nuvem.

### Restaurar uma cópia

A restauração substitui os dados atuais pelos da cópia.

```bash
# 1. pare o backend, para nada gravar durante a restauração

# 2. restaure (troque o nome do arquivo)
docker exec -i smartgarden-postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' \
  < ~/intelligarden-backups/smartgarden-AAAA-MM-DD_HHMMSS.dump

# 3. suba o backend de novo
```

Para só conferir uma cópia, sem tocar no banco real, restaure num banco temporário:

```bash
docker exec smartgarden-postgres sh -c 'createdb -U "$POSTGRES_USER" restore_teste'
docker exec -i smartgarden-postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d restore_teste --no-owner' < ~/intelligarden-backups/ARQUIVO.dump
docker exec smartgarden-postgres sh -c 'psql -U "$POSTGRES_USER" -d restore_teste -c "select count(*) from environmental_readings"'
docker exec smartgarden-postgres sh -c 'dropdb -U "$POSTGRES_USER" restore_teste'
```

Em 27/11/2026, uma cópia foi restaurada assim e saiu idêntica ao banco real: leituras, plantas, registros, curva da água e as 9 migrações.

## Pesagens simuladas (teste, sem vaso real)

Sem vasos de verdade, `simular-pesagens.py` faz o papel da equipe com a balança, seguindo o protocolo de `docs/pesagem-diaria.md`, nos vasos com a irrigação simulada desligada (manjericão, lírio e jiboia):

- na primeira vez, pesa o vaso cheio;
- depois, uma pesagem por dia: a perda vem do clima real medido pelo sensor desde a pesagem anterior, com um k "verdadeiro" escondido para cada planta, uma variação de ±8 % de um dia para o outro e o ruído de uma balança de cozinha;
- quando a balança mostra menos de 70 % de água, rega e pesa de novo, como vaso cheio.

Se o sistema estiver certo, o k sugerido na ficha de cada planta chega perto do k verdadeiro que está no script (manjericão 2,6; lírio 2,2; jiboia 0,75). Num teste de 10 dias simulados no backend de teste, chegou a 2,61, 2,33 e 0,75.

Todo registro sai com a nota "Pesagem simulada", para separar das pesagens de verdade. A página ainda não mostra essa nota: na ficha, a pesagem simulada aparece como uma pesagem comum.

```bash
./simular-pesagens.py            # pesa agora
./simular-pesagens.py instalar   # uma vez por dia, às 9h (timer do systemd do usuário)
./simular-pesagens.py remover    # tira o agendamento
```

O script só grava no backend desta máquina (`localhost`) e pula um vaso pesado há menos de 6 horas. Para apagar as pesagens simuladas, restaure uma cópia do banco feita antes delas (veja "Restaurar uma cópia").

## Configuração MQTT

Há dois tipos de usuário, criados pelo serviço `mosquitto-auth` a partir do `.env`:

| Usuário | Senha (`.env`) | Pode |
|---|---|---|
| `smartgarden` (backend) | `MQTT_PASSWORD` | ler `smartgarden/devices/+/telemetry` e `+/status`; publicar em `+/commands` |
| `MQTT_DEVICE_USER` (ESP32, igual ao código do dispositivo) | `MQTT_DEVICE_PASSWORD` | publicar só a própria `telemetry` e `status`; ler só os próprios `commands` |

As regras do dispositivo usam `pattern ... %u`, ou seja, o nome do usuário entra no tópico: um ESP32 não consegue publicar como se fosse outro. A senha do dispositivo vai também no `secrets.h` do firmware. Gere uma com `openssl rand -hex 16`.

O broker guarda as leituras (`queue_qos0_messages`) enquanto o backend está reiniciando, porque o backend usa sessão persistente; quando ele volta, recebe o que chegou nesse meio-tempo.

Depois de alterar senhas ou usuários no `.env`, recrie o inicializador e o broker:

```bash
docker compose up -d --force-recreate mosquitto-auth mosquitto
```

Se a porta `1883` estiver ocupada pelo Mosquitto instalado no sistema:

```bash
sudo systemctl disable --now mosquitto
```

## Integração com o backend

O `smartgarden-back` aponta para esta infraestrutura (`localhost:5432` e `tcp://localhost:1883`) e lê as senhas deste mesmo `.env` ao iniciar. Variáveis `SMARTGARDEN_*` no processo do backend têm prioridade, se precisar sobrescrever.
