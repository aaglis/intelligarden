// Modelo de configuração local do firmware.
// Copie este arquivo para secrets.h (na mesma pasta) e preencha com os seus dados.
// O secrets.h está no .gitignore e nunca deve ser commitado.

#pragma once

// Rede Wi-Fi 2,4 GHz (o ESP32 não conecta em 5 GHz).
const char* WIFI_SSID = "SEU_WIFI";
const char* WIFI_PASSWORD = "SUA_SENHA";

// Broker MQTT (Mosquitto do smartgarden-infra).
// Use o IP da máquina que roda o broker, na mesma rede do ESP32 (veja com: ip addr).
// Não use localhost: para o ESP32, localhost é ele mesmo.
const char* MQTT_HOST = "192.168.0.10";
const uint16_t MQTT_PORT = 1883;
// Cada ESP32 tem usuário próprio no broker, igual ao DEVICE_CODE do firmware.
const char* MQTT_USER = "esp32-jardim-bloco-a";
const char* MQTT_PASSWORD = "SUA_SENHA_MQTT";  // MQTT_DEVICE_PASSWORD do smartgarden-infra/.env
