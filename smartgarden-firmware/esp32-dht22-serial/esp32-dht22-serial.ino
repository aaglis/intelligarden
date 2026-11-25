#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <time.h>

// ===== CONFIGURAÇÃO DO SENSOR =====
#define DHTPIN 4          // GPIO conectado ao DATA do DHT22
#define DHTTYPE DHT22     // Tipo do sensor

// ===== CONFIGURAÇÃO LOCAL =====
// WIFI_SSID, WIFI_PASSWORD e os dados do broker MQTT ficam em secrets.h, que não vai para o Git.
// Copie secrets.example.h para secrets.h e preencha com os dados da sua rede.
#if __has_include("secrets.h")
#include "secrets.h"
#else
#error "Arquivo secrets.h nao encontrado: copie secrets.example.h para secrets.h e preencha."
#endif

const char* DEVICE_CODE = "esp32-jardim-bloco-a";

// O backend assina smartgarden/devices/+/telemetry e tira o código do dispositivo do tópico.
const String TELEMETRY_TOPIC = String("smartgarden/devices/") + DEVICE_CODE + "/telemetry";

// ===== INTERVALOS =====
const unsigned long READ_INTERVAL_MS = 30000;
const unsigned long WIFI_FIRST_WAIT_MS = 20000;
const unsigned long WIFI_RETRY_INTERVAL_MS = 60000;  // a rede do campus pode levar ~40 s para associar
const unsigned long MQTT_RETRY_INTERVAL_MS = 5000;

// Qualquer horário depois de 2024 indica que o relógio já foi acertado pelo NTP.
const time_t VALID_TIME_AFTER = 1704067200;

DHT dht(DHTPIN, DHTTYPE);
WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);

unsigned long lastReadAt = 0;
unsigned long lastWifiRetryAt = 0;
unsigned long lastMqttRetryAt = 0;
bool hasRead = false;
bool hasTriedMqtt = false;
bool wifiOnline = false;

// Inicia a conexão uma vez e espera até WIFI_FIRST_WAIT_MS. Depois disso o próprio ESP32
// reconecta sozinho (setAutoReconnect); chamar WiFi.begin() de novo enquanto ele ainda está
// associando só gera o erro "sta is connecting, cannot set config" e atrasa a conexão.
void startWiFi() {
  Serial.print("Conectando ao Wi-Fi ");
  Serial.print(WIFI_SSID);
  Serial.println("...");

  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  lastWifiRetryAt = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - lastWifiRetryAt < WIFI_FIRST_WAIT_MS) {
    delay(250);
  }
}

bool ensureWiFiConnected() {
  if (WiFi.status() == WL_CONNECTED) {
    if (!wifiOnline) {
      wifiOnline = true;
      Serial.print("Wi-Fi conectado. IP do ESP32: ");
      Serial.println(WiFi.localIP());
    }
    return true;
  }

  if (wifiOnline) {
    wifiOnline = false;
    lastWifiRetryAt = millis();
    Serial.println("Wi-Fi caiu. Aguardando reconexao automatica...");
  }

  // Se a reconexão automática não resolver em WIFI_RETRY_INTERVAL_MS, recomeça do zero (sem travar o loop).
  if (millis() - lastWifiRetryAt >= WIFI_RETRY_INTERVAL_MS) {
    lastWifiRetryAt = millis();
    Serial.print("Wi-Fi ainda fora (status ");
    Serial.print(WiFi.status());
    Serial.println("). Reiniciando a conexao...");
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  }
  return false;
}

// Tenta (re)conectar ao broker sem travar o loop: no máximo uma tentativa a cada MQTT_RETRY_INTERVAL_MS.
bool ensureMqttConnected() {
  if (mqtt.connected()) {
    return true;
  }

  if (!ensureWiFiConnected()) {
    return false;
  }

  if (hasTriedMqtt && millis() - lastMqttRetryAt < MQTT_RETRY_INTERVAL_MS) {
    return false;
  }

  hasTriedMqtt = true;
  lastMqttRetryAt = millis();

  Serial.print("Conectando ao broker MQTT ");
  Serial.print(MQTT_HOST);
  Serial.print(":");
  Serial.print(MQTT_PORT);
  Serial.println("...");

  if (mqtt.connect(DEVICE_CODE, MQTT_USER, MQTT_PASSWORD)) {
    Serial.println("MQTT conectado.");
    return true;
  }

  // Códigos do PubSubClient: -2 broker inalcançável, 4 usuário/senha inválidos, 5 não autorizado.
  Serial.print("Falha no MQTT, estado: ");
  Serial.println(mqtt.state());
  return false;
}

// UUID v4 aleatório. O backend usa esse id para descartar mensagens repetidas.
String newMessageId() {
  uint8_t b[16];
  for (int i = 0; i < 16; i += 4) {
    uint32_t r = esp_random();
    memcpy(b + i, &r, 4);
  }
  b[6] = (b[6] & 0x0F) | 0x40;
  b[8] = (b[8] & 0x3F) | 0x80;

  char id[37];
  snprintf(id, sizeof(id),
           "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
           b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7],
           b[8], b[9], b[10], b[11], b[12], b[13], b[14], b[15]);
  return String(id);
}

// Horário da leitura em UTC (ISO 8601). Vazio enquanto o NTP não acertou o relógio;
// nesse caso o backend usa o horário em que recebeu a mensagem.
String recordedAtUtc() {
  time_t now = time(nullptr);
  if (now < VALID_TIME_AFTER) {
    return "";
  }

  struct tm utc;
  gmtime_r(&now, &utc);
  char buf[25];
  strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", &utc);
  return String(buf);
}

void publishReading(float temperature, float humidity) {
  if (!ensureMqttConnected()) {
    Serial.println("Sem conexao com o broker. Leitura nao enviada.");
    return;
  }

  String payload = "{";
  payload += "\"messageId\":\"" + newMessageId() + "\",";
  payload += "\"temperatureC\":" + String(temperature, 2) + ",";
  payload += "\"humidityPercent\":" + String(humidity, 2);

  String recordedAt = recordedAtUtc();
  if (recordedAt.length() > 0) {
    payload += ",\"recordedAt\":\"" + recordedAt + "\"";
  }
  payload += "}";

  bool sent = mqtt.publish(TELEMETRY_TOPIC.c_str(), payload.c_str());

  Serial.println("===== ENVIO MQTT =====");
  Serial.print("Topico: ");
  Serial.println(TELEMETRY_TOPIC);
  Serial.print("Payload: ");
  Serial.println(payload);
  Serial.println(sent ? "Publicado." : "Falha ao publicar.");
  Serial.println("---------------------------------");
}

void setup() {
  Serial.begin(115200);
  delay(2000);  // estabiliza a serial e o DHT22

  Serial.println("=================================");
  Serial.println("INTELLIGARDEN - SENSOR DO AMBIENTE");
  Serial.println("ESP32 + DHT22 via MQTT");
  Serial.println("=================================");

  dht.begin();

  // Relógio em UTC pelo NTP, para carimbar o horário de cada leitura.
  configTime(0, 0, "a.st1.ntp.br", "pool.ntp.org");

  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setKeepAlive(30);

  startWiFi();
  ensureMqttConnected();
}

void loop() {
  ensureMqttConnected();
  mqtt.loop();

  if (hasRead && millis() - lastReadAt < READ_INTERVAL_MS) {
    return;
  }

  hasRead = true;
  lastReadAt = millis();

  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();

  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("ERRO: Falha ao ler o DHT22! Verifique alimentacao, GPIO e cabo DATA.");
    return;
  }

  Serial.print("Leitura: ");
  Serial.print(temperature, 2);
  Serial.print(" C, ");
  Serial.print(humidity, 2);
  Serial.println(" %");

  publishReading(temperature, humidity);
}
