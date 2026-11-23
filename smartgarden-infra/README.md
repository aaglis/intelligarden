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

O Compose gera a credencial em um volume Docker privado antes de iniciar o Mosquitto. Apenas o arquivo `.env` com a senha em texto claro é ignorado pelo Git.

Credenciais padrão de desenvolvimento:

```text
PostgreSQL: smartgarden / smartgarden
MQTT: smartgarden / smartgarden_mqtt
```

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

O usuário `smartgarden` pode publicar e assinar somente:

```text
smartgarden/devices/+/telemetry
```

Depois de alterar `MQTT_PASSWORD` no `.env`, recrie o inicializador e o broker:

```bash
docker compose up -d --force-recreate mosquitto-auth mosquitto
```

Se a porta `1883` estiver ocupada pelo Mosquitto instalado no sistema:

```bash
sudo systemctl disable --now mosquitto
```

## Integração com o backend

Os valores padrão do `smartgarden-back` já apontam para esta infraestrutura:

```text
PostgreSQL: jdbc:postgresql://localhost:5432/smartgarden
MQTT: tcp://localhost:1883
```

Se as credenciais forem alteradas, configure no processo do backend:

```bash
export SMARTGARDEN_DB_USERNAME=smartgarden
export SMARTGARDEN_DB_PASSWORD=senha_do_postgres
export SMARTGARDEN_MQTT_USERNAME=smartgarden
export SMARTGARDEN_MQTT_PASSWORD=senha_do_mqtt
```
