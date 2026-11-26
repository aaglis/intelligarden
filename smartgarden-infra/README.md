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

Esse comando é destrutivo e remove o banco PostgreSQL e a persistência do broker.

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
