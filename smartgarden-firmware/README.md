# SmartGarden Firmware

Firmware do sensor do ambiente do projeto (ESP32 + DHT22). A cada 30 s ele lê **temperatura** e **umidade do ar** e publica a leitura no broker **MQTT** (Mosquitto do `smartgarden-infra`). O backend assina o tópico e grava as leituras no PostgreSQL.

```
ESP32 + DHT22 ──MQTT──▶ Mosquitto ──▶ Spring Boot ──▶ PostgreSQL
```

---

## Componentes e bibliotecas

- ESP32 Dev Module
- Sensor DHT22
- DHT sensor library (Adafruit) e Adafruit Unified Sensor
- PubSubClient (Nick O'Leary), cliente MQTT

```bash
arduino-cli lib install "DHT sensor library" "Adafruit Unified Sensor" PubSubClient
```

## Ligações do sensor DHT22

| DHT22 | ESP32 |
|---|---|
| VCC | 3.3V |
| GND | GND |
| DATA | GPIO 4 |

## Configuração antes de gravar na placa

O Wi-Fi e os dados do broker ficam em `esp32-dht22-serial/secrets.h`, que está no `.gitignore` e não vai para o repositório. Crie o arquivo a partir do modelo:

```bash
cp esp32-dht22-serial/secrets.example.h esp32-dht22-serial/secrets.h
```

E preencha:

```cpp
const char* WIFI_SSID = "SEU_WIFI";          // rede 2,4 GHz
const char* WIFI_PASSWORD = "SUA_SENHA";
const char* MQTT_HOST = "192.168.0.10";      // IP da máquina do broker, nunca localhost
const uint16_t MQTT_PORT = 1883;
const char* MQTT_USER = "smartgarden";
const char* MQTT_PASSWORD = "SUA_SENHA_MQTT";  // MQTT_PASSWORD do smartgarden-infra/.env
```

Sem o `secrets.h`, a compilação para com uma mensagem explicando o que fazer.

O código do dispositivo fica no `esp32-dht22-serial.ino` e também define o tópico:

```cpp
const char* DEVICE_CODE = "esp32-jardim-bloco-a";
```

### Gravando pela linha de comando (arduino-cli)

```bash
arduino-cli compile --fqbn esp32:esp32:esp32 esp32-dht22-serial
arduino-cli upload -p /dev/ttyACM0 --fqbn esp32:esp32:esp32 esp32-dht22-serial
```

No Linux, o usuário precisa de acesso à porta serial: `sudo usermod -aG dialout $USER` e novo login.

## Mensagem publicada

Tópico:

```text
smartgarden/devices/esp32-jardim-bloco-a/telemetry
```

Payload (JSON):

```json
{
  "messageId": "6f1c2a9e-4b7d-4c1e-9a3f-2d8b5e7c1a40",
  "temperatureC": 27.40,
  "humidityPercent": 63.10,
  "recordedAt": "2026-11-25T15:30:00Z"
}
```

- `messageId`: UUID gerado no ESP32. O backend ignora mensagens com um id que já recebeu.
- `recordedAt`: horário da leitura em UTC, acertado por NTP. Enquanto o relógio não sincroniza, o campo é omitido e o backend usa o horário de chegada.

## Como testar

1. Suba a infra (`smartgarden-infra`) e o backend.
2. Grave o firmware e abra o monitor serial em 115200 (`arduino-cli monitor -p /dev/ttyACM0 -c baudrate=115200`).
3. Devem aparecer `MQTT conectado.` e, a cada 30 s, `Publicado.`.
4. Para ver as mensagens chegando no broker:

```bash
docker exec smartgarden-mosquitto mosquitto_sub -u smartgarden -P "$MQTT_PASSWORD" \
  -t 'smartgarden/devices/+/telemetry' -v
```

Se aparecer `Falha no MQTT, estado: -2`, o ESP32 não alcança o broker (IP errado ou porta 1883 bloqueada). Estado `4` ou `5` indica usuário ou senha incorretos.

O backend ainda aceita `POST /api/readings`, útil para testes sem o dispositivo.
