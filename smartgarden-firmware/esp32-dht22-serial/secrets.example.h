// Modelo de configuração local do firmware.
// Copie este arquivo para secrets.h (na mesma pasta) e preencha com os seus dados.
// O secrets.h está no .gitignore e nunca deve ser commitado.

#pragma once

// Rede Wi-Fi 2,4 GHz (o ESP32 não conecta em 5 GHz).
const char* WIFI_SSID = "SEU_WIFI";
const char* WIFI_PASSWORD = "SUA_SENHA";

// IP da máquina que roda o backend, na mesma rede do ESP32 (veja com: ip addr).
// Não use localhost: para o ESP32, localhost é ele mesmo.
const char* API_URL = "http://192.168.0.10:8080/api/readings";
