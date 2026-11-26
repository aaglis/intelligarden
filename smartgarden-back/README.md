# SmartGarden Back

Backend do SmartGarden usando `Java 21`, `Spring Boot`, `PostgreSQL`, MQTT, REST e SSE.

## Arquitetura desta fase

`ESP32 -> MQTT -> Mosquitto -> Spring Boot -> PostgreSQL`

O ESP32 publica telemetria com QoS 1. O backend consome, valida, persiste e notifica o Angular por SSE. REST continua disponível para consultas, relatórios e ingestão compatível.

## Stack

- Java 21
- Spring Boot 3.5.14
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Bean Validation
- SpringDoc OpenAPI / Swagger UI
- Spring Integration MQTT / Eclipse Paho
- Eclipse Mosquitto 2
- Server-Sent Events

## Como subir PostgreSQL e Mosquitto

A infraestrutura foi separada no repositório `smartgarden-infra`. No primeiro uso:

```bash
cd ../smartgarden-infra
cp .env.example .env
docker compose up -d
```

Isso cria o PostgreSQL (`localhost:5432`, banco e usuário `smartgarden`) e o broker MQTT (`localhost:1883`, usuário `smartgarden`). As senhas ficam só no `smartgarden-infra/.env`, que não vai para o Git.

O backend lê esse mesmo `.env` ao iniciar (`spring.config.import` no `application.yml`), então não é preciso exportar nada. O usuário do backend lê `telemetry` e `status` de todos os dispositivos e publica em `commands`; cada ESP32 tem usuário próprio. Consulte o README do `smartgarden-infra` para configuração e migração dos volumes.

## Migrações de banco

O schema agora é versionado com `Flyway`.

- scripts: `src/main/resources/db/migration`
- estratégia JPA: `ddl-auto=validate`

Em banco vazio, o Flyway cria toda a estrutura. Em banco já existente, a opção `baseline-on-migrate` evita conflito com schema legado e permite passar a controlar as próximas mudanças por migração.

## Como rodar a aplicação

```bash
mvn spring-boot:run
```

As senhas vêm do `smartgarden-infra/.env`. Variáveis de ambiente têm prioridade, se quiser sobrescrever:

```bash
export SMARTGARDEN_DB_URL=jdbc:postgresql://localhost:5432/smartgarden
export SMARTGARDEN_DB_USERNAME=smartgarden
export SMARTGARDEN_DB_PASSWORD=sua_senha_do_postgres
export SMARTGARDEN_MQTT_BROKER_URL=tcp://localhost:1883
export SMARTGARDEN_MQTT_USERNAME=smartgarden
export SMARTGARDEN_MQTT_PASSWORD=sua_senha_do_mqtt
mvn spring-boot:run
```

Sem o `.env` e sem as variáveis, a aplicação não sobe e acusa `Could not resolve placeholder 'POSTGRES_PASSWORD'` (ou `MQTT_PASSWORD`).

## Swagger / OpenAPI

Com a aplicação no ar, a documentação interativa fica em:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui/index.html`

Os endpoints OpenAPI ficam em:

- `http://localhost:8080/v3/api-docs`
- `http://localhost:8080/v3/api-docs.yaml`

## Endpoints principais

### Criar dispositivo

`POST /api/devices`

Exemplo de body:

```json
{
  "deviceCode": "esp32-jardim-bloco-a",
  "name": "ESP32 Jardim Bloco A",
  "location": "Universidade - Bloco A"
}
```

### Listar dispositivos

`GET /api/devices`

### Consultar dispositivo

`GET /api/devices/{deviceCode}`

Além do cadastro, traz `online`, `statusChangedAt`, `ipAddress` e `firmwareVersion`, informados pelo próprio ESP32 via MQTT (veja "Status do dispositivo" abaixo).

### Enviar comando ao dispositivo

`POST /api/devices/{deviceCode}/commands` → `202 Accepted`

```json
{ "command": "identify" }
```

```json
{ "command": "irrigation", "durationSeconds": 10 }
```

- `identify`: pisca o LED da placa (para achar o sensor).
- `irrigation`: irrigação simulada; o LED fica aceso por `durationSeconds` (1 a 60, padrão 10).

O comando é publicado em `smartgarden/devices/{deviceCode}/commands` (QoS 1). Se o dispositivo estiver offline, o broker descarta o comando, e a resposta traz `deviceOnline: false`. Sem MQTT habilitado, responde `503`.

### Registrar leitura

`POST /api/readings`

Exemplo de body:

```json
{
  "deviceCode": "esp32-jardim-bloco-a",
  "temperatureC": 27.4,
  "humidityPercent": 63.1,
  "recordedAt": "2026-05-21T14:30:00Z",
  "messageId": "9d892fe8-d62a-4bd9-a0cc-a4c70f78271e"
}
```

`messageId` é opcional no REST. Quando informado, torna a operação idempotente. Se o dispositivo ainda não existir, ele é criado automaticamente com nome padrão.

### Acompanhar leituras em tempo real

`GET /api/events/readings`

Use `deviceCode` para filtrar um dispositivo. O endpoint SSE envia os eventos `connected`, `reading-created`, `device-status` (dispositivo ficou online/offline) e `heartbeat`.

## Contrato MQTT

Tópico: `smartgarden/devices/{deviceCode}/telemetry`

```json
{
  "messageId": "9d892fe8-d62a-4bd9-a0cc-a4c70f78271e",
  "temperatureC": 27.4,
  "humidityPercent": 63.1,
  "recordedAt": null
}
```

O backend extrai `deviceCode` do tópico. `messageId` é obrigatório no MQTT e impede duplicação causada por retransmissões QoS 1. Se `recordedAt` for nulo, é usado o horário do backend.

### Status do dispositivo

Tópico: `smartgarden/devices/{deviceCode}/status` (mensagem retida)

```json
{ "state": "online", "ip": "10.50.22.73", "firmware": "1.1.0" }
```

O ESP32 publica `online` ao conectar e registra `{"state":"offline"}` como *last will*: se ele sumir sem se despedir (queda de energia, Wi-Fi), o broker publica o `offline` sozinho depois de até 45 s (1,5 × keep-alive de 30 s). O backend atualiza o dispositivo e emite o evento SSE `device-status` só quando o estado muda.

### Comandos

Tópico: `smartgarden/devices/{deviceCode}/commands`

```json
{ "commandId": "6f1c…", "command": "irrigation", "durationSeconds": 10, "issuedAt": "2026-11-26T01:26:28Z" }
```

### Listar leituras

`GET /api/readings`

Filtros disponíveis:

- `deviceCode`
- `page`
- `size`
- `startAt`
- `endAt`

Exemplo:

`GET /api/readings?deviceCode=esp32-jardim-bloco-a&page=0&size=20`

Exemplo de resposta:

```json
{
  "content": [
    {
      "id": 2,
      "deviceId": 1,
      "deviceCode": "esp32-jardim-bloco-a",
      "deviceName": "ESP32 Jardim Bloco A",
      "messageId": "9d892fe8-d62a-4bd9-a0cc-a4c70f78271e",
      "temperatureC": 31.9,
      "humidityPercent": 64.6,
      "recordedAt": "2026-05-21T12:16:53.231716916Z",
      "receivedAt": "2026-05-21T12:16:53.235524803Z",
      "createdAt": "2026-05-21T12:16:53.235524803Z",
      "updatedAt": "2026-05-21T12:16:53.235524803Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Última leitura de um dispositivo

`GET /api/readings/latest?deviceCode=esp32-jardim-bloco-a`

### Histórico para gráfico

`GET /api/readings/history?deviceCode=esp32-jardim-bloco-a&hours=24&limit=120`

Parâmetros:

- `deviceCode`: obrigatório
- `hours`: janela em horas quando `startAt` não for informado
- `limit`: quantidade máxima de pontos retornados
- `startAt`
- `endAt`

Esse endpoint retorna a série histórica já ordenada por `recordedAt`, pronta para alimentar o gráfico do dashboard.

### Resumo para dashboard

`GET /api/dashboard/summary`

Parâmetros:

- `hours`: janela de agregação em horas, entre `1` e `168`

Exemplo:

`GET /api/dashboard/summary?hours=24`

Esse endpoint retorna:

- total de dispositivos
- total de dispositivos ativos
- total de leituras
- leituras dentro da janela
- média de temperatura na janela
- média de umidade na janela
- total de alertas ativos
- lista de alertas calculados
- última leitura conhecida por dispositivo
- critérios ambientais globais usados nos diagnósticos

### Relatório ambiental

`GET /api/reports/environmental`

Parâmetros obrigatórios:

- `deviceCode`: código do dispositivo
- `startAt`: início do período em ISO 8601
- `endAt`: fim do período em ISO 8601

O período deve ter no máximo 31 dias. Exemplo:

`GET /api/reports/environmental?deviceCode=esp32-jardim-bloco-a&startAt=2026-05-01T00:00:00-03:00&endAt=2026-05-08T00:00:00-03:00`

O relatório é calculado sob demanda e retorna:

- médias, mínimos e máximos de temperatura e umidade
- quantidade e percentual de leituras ambientalmente adequadas
- série temporal agregada em até 120 pontos
- total de exceções e as 50 ocorrências mais recentes fora das faixas
- critérios ambientais usados no cálculo

## Critérios ambientais

As faixas são globais e os limites são inclusivos:

- temperatura agradável: de 20 °C a 30 °C
- temperatura fria: abaixo de 20 °C
- temperatura quente: acima de 30 °C
- umidade normal: de 40% a 70%
- umidade baixa: abaixo de 40%
- umidade alta: acima de 70%
- ambiente adequado: temperatura e umidade simultaneamente dentro das faixas
