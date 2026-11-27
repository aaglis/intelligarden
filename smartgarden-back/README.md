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

**Interface 3D:** o backend também serve a página do `intelligarden-web` em `http://localhost:8080/` (ou `http://IP-da-máquina:8080/` de outro aparelho da mesma rede). No build, o Maven copia `../intelligarden-web/index.html` e `vendor/` para `static/`; depois de mudar a página, reinicie o backend para ela ser copiada de novo. A página vai com `Cache-Control: no-cache`, para o navegador não ficar com uma versão antiga.

CORS: só importa quando a página está em outro endereço. Por padrão aceita o Angular (`http://localhost:4200`) e o 3D servido à parte (`http://localhost:5173`). Para outras origens, defina `SMARTGARDEN_CORS_ALLOWED_ORIGINS` com as origens separadas por vírgula.

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

Leitura com salto implausível é recusada (`400` no REST; pelo MQTT, descartada com aviso no log): mais de 5 °C ou 20 pontos de umidade de diferença para a leitura anterior do mesmo dispositivo, se ela tiver menos de 2 minutos. Isso barra a falha do DHT22 que entrega temperatura e umidade pela metade numa leitura isolada. Se o ambiente mudar de verdade, a leitura volta a ser aceita depois de 2 minutos.

### Acompanhar leituras em tempo real

`GET /api/events/readings`

Use `deviceCode` para filtrar um dispositivo. O endpoint SSE envia os eventos `connected`, `reading-created`, `device-status` (dispositivo ficou online/offline), `plants-updated` (água estimada mudou; lista das plantas afetadas), `plant-irrigated` (irrigação simulada disparada: `{plant, record}`) e `heartbeat`.

## Plantas e água estimada

Não há sensor de solo: cada planta tem uma "caixa d'água" **estimada**, que esvazia conforme o clima medido pelo sensor ligado a ela e enche com regas e irrigações.

- **VPD** (déficit de pressão de vapor, kPa) mostra o quanto o ar está seco. Vem da temperatura e da umidade de cada leitura: `es = 0,6108 × exp(17,27 T / (T + 237,3))` (FAO-56, Allen et al., 1998, eq. 11) e `VPD = es × (1 − UR/100)`.
- **Perda** a cada leitura: `k × VPD médio × horas desde a leitura anterior`, com `k` (mL por hora por kPa) próprio da planta e do vaso. A primeira leitura só ancora a estimativa; lacunas maiores que 12 h contam como 12 h.
- **Limite**: quando a água estimada chega em `irrigationThresholdPercent`, a planta fica com `needsWater: true`.
  - `autoIrrigation: true` (planta virtual): o backend registra uma irrigação simulada com `irrigationMl`, acende o LED do sensor (1 s a cada 10 mL, até 60 s) e avisa a interface. Intervalo mínimo de 30 min entre irrigações automáticas.
  - `autoIrrigation: false` (vaso real): só sinaliza; a equipe rega e registra. Simular irrigação nesse vaso responde `409`.
- **Pesagem**: compara a perda medida pela balança (1 g = 1 mL) com a estimada desde a pesagem anterior, se não houve rega entre elas. Com pelo menos 3 kPa·h de clima no intervalo, calcula o `k` que explicaria a perda medida. A planta guarda o `suggestedCoefficient` (mediana das últimas 10 pesagens) e o `meanErrorPercent`; para adotar, envie o novo `lossCoefficient` no `PATCH`.
- **Vaso cheio**: uma pesagem com `fullPot: true` (logo depois de regar e escorrer) vira a referência. A partir daí, cada pesagem também corrige a água estimada para o valor medido.

As 6 plantas do protótipo 3D já vêm cadastradas (migração `V6`), ligadas ao `esp32-jardim-bloco-a`. Os valores de `k` são estimativas iniciais (intervalo típico entre regas da espécie com VPD de 1,5 kPa) e precisam ser calibrados com as pesagens; as faixas de temperatura e umidade ainda precisam das fontes nas fichas das espécies.

### Listar e consultar plantas

`GET /api/plants` (opcional `?deviceCode=`) e `GET /api/plants/{code}`

Cada planta traz `estimatedWaterMl`, `estimatedWaterPercent`, `lastVpdKpa`, `lossMlPerHour` (ritmo com o clima atual), `hoursToThreshold` (horas até o limite se o clima continuar igual), `needsWater` e os dados de pesagem e calibração (`wateredSinceWeighing` diz se houve rega depois da última pesagem, caso em que a próxima não compara perdas).

### Cadastrar e alterar planta

`POST /api/plants` → `201`

```json
{
  "code": "alecrim",
  "name": "Alecrim",
  "deviceCode": "esp32-jardim-bloco-a",
  "waterCapacityMl": 250,
  "irrigationThresholdPercent": 30,
  "lossCoefficient": 2.0,
  "autoIrrigation": true,
  "initialWaterPercent": 100
}
```

`irrigationMl` é opcional (padrão: o que leva do limite ao vaso cheio). `PATCH /api/plants/{code}` altera só os campos enviados; mudar `waterCapacityMl` mantém a porcentagem de água.

### Registro diário

`POST /api/plants/{code}/records` → `201`

```json
{ "type": "weighing", "weightGrams": 860, "fullPot": false }
```

```json
{ "type": "watering", "waterMl": 180, "drained": true }
```

```json
{ "type": "observation", "note": "Folhas de baixo amareladas" }
```

A resposta da pesagem traz `estimatedLossMl`, `measuredLossMl`, `measuredCoefficient` e, se houver peso de vaso cheio, `measuredWaterPercent`. Rega com `drained: true` deixa o vaso cheio na estimativa.

`GET /api/plants/{code}/records?limit=20` lista pesagens, regas, irrigações e observações, da mais nova para a mais antiga.

### Simular irrigação

`POST /api/plants/{code}/irrigations` → `201`

Coloca `irrigationMl` na água estimada, registra a irrigação e acende o LED do sensor. Recusado com `409` para vasos reais.

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

Esse endpoint retorna a série histórica já ordenada por `recordedAt`, pronta para alimentar um gráfico. Quando há mais leituras que `limit`, o período é dividido em `limit` faixas de tempo iguais e volta a última leitura de cada faixa: os pontos cobrem a janela inteira (com `hours=24&limit=288`, um ponto a cada 5 minutos). Faixa sem leitura fica vazia, então um período com o sensor desligado aparece como buraco.

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
