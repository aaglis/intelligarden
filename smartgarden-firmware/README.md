# IntelliGarden · firmware

Firmware do sensor do ambiente do projeto (ESP32 + DHT22). A cada 30 s ele lê **temperatura** e **umidade do ar** e publica a leitura no broker **MQTT** (Mosquitto do `smartgarden-infra`). Ele também avisa quando está online/offline e recebe comandos do backend: piscar o LED e **irrigação simulada** (LED aceso).

```
ESP32 + DHT22 ──MQTT──▶ Mosquitto ──▶ Spring Boot ──▶ PostgreSQL
```

---

## Componentes e bibliotecas

- ESP32 Dev Module
- Sensor DHT22
- DHT sensor library (Adafruit) e Adafruit Unified Sensor
- PubSubClient (Nick O'Leary), cliente MQTT
- ArduinoJson, para ler os comandos

```bash
arduino-cli lib install "DHT sensor library" "Adafruit Unified Sensor" PubSubClient ArduinoJson
```

O LED usado é o azul da placa (GPIO 2 no ESP32 DevKit, `LED_PIN` no código).

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
const char* MQTT_USER = "esp32-jardim-bloco-a";   // igual ao DEVICE_CODE
const char* MQTT_PASSWORD = "SUA_SENHA_MQTT";      // MQTT_DEVICE_PASSWORD do smartgarden-infra/.env
```

Cada ESP32 entra no broker com o próprio usuário, igual ao código do dispositivo, e só pode usar os tópicos dele.

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

## Tópicos

| Tópico | Sentido | Para quê |
|---|---|---|
| `smartgarden/devices/<codigo>/telemetry` | ESP32 → backend | leitura a cada 30 s |
| `smartgarden/devices/<codigo>/status` | ESP32 → backend (retida) | `online` ao conectar; `offline` publicado pelo broker (last will) se o ESP32 sumir |
| `smartgarden/devices/<codigo>/commands` | backend → ESP32 | `identify` (pisca o LED) e `irrigation` (LED aceso por N s) |

### Leitura (telemetry)

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

### Status

```json
{ "state": "online", "ip": "10.50.22.73", "firmware": "1.1.0" }
```

Ao conectar, o ESP32 registra no broker o "testamento" `{"state":"offline"}`. Se ele desligar ou perder o Wi-Fi sem se despedir, o broker publica isso sozinho em até 45 s.

### Comandos

```json
{ "command": "identify" }
{ "command": "irrigation", "durationSeconds": 10 }
```

O ESP32 conecta com sessão limpa: comandos enviados enquanto ele estava offline são descartados, para não acender o LED com uma irrigação antiga quando ele volta.

## Como testar

1. Suba a infra (`smartgarden-infra`) e o backend.
2. Grave o firmware e abra o monitor serial em 115200 (`arduino-cli monitor -p /dev/ttyACM0 -c baudrate=115200`).
3. Devem aparecer `MQTT conectado.` e, a cada 30 s, `Publicado.`.
4. Para ver as mensagens chegando no broker:

```bash
docker exec smartgarden-mosquitto mosquitto_sub -u smartgarden -P "$MQTT_PASSWORD" \
  -t 'smartgarden/devices/+/telemetry' -t 'smartgarden/devices/+/status' -v
```

Para testar os comandos com o backend no ar:

```bash
curl -X POST localhost:8080/api/devices/esp32-jardim-bloco-a/commands \
  -H 'content-type: application/json' -d '{"command":"irrigation","durationSeconds":5}'
```

No monitor serial aparecem `Comando recebido: irrigation` e, 5 s depois, `Irrigacao simulada encerrada`. Abrir o monitor serial reinicia a placa; espere o `MQTT conectado.` antes de mandar o comando.

Se aparecer `Falha no MQTT, estado: -2`, o ESP32 não alcança o broker (IP errado ou porta 1883 bloqueada). Estado `4` ou `5` indica usuário ou senha incorretos.

O backend ainda aceita `POST /api/readings`, útil para testes sem o dispositivo.
