# SmartGarden

Jardim virtual com ESP32 e sensor DHT22: o dispositivo mede temperatura e umidade do ar; o backend guarda as leituras, estima a água no vaso de cada planta pelo clima (VPD × k, calibrado com pesagens) e dispara a irrigação simulada (LED do ESP32) quando uma planta chega no limite. O frontend mostra o jardim, o monitoramento em tempo real e relatórios.

```
ESP32 + DHT22 ──MQTT──▶ Mosquitto ──▶ Spring Boot ──▶ PostgreSQL
                                           │
                                           └──REST/SSE──▶ Angular
```

## Estrutura

| Pasta | Conteúdo |
|---|---|
| [`smartgarden-firmware`](smartgarden-firmware) | Firmware do ESP32 (Arduino) |
| [`smartgarden-back`](smartgarden-back) | Backend Java 21 / Spring Boot (REST, MQTT, SSE, água estimada das plantas) |
| [`smartgarden-front`](smartgarden-front) | Frontend Angular 19 |
| [`smartgarden-infra`](smartgarden-infra) | PostgreSQL e Mosquitto via Docker Compose |
| [`intelligarden-web`](intelligarden-web) | Protótipo 3D da nova interface (IntelliGarden) |

Cada pasta tem seu próprio README com instruções detalhadas.

## Subindo tudo localmente

```bash
# 1. Infra (PostgreSQL + MQTT)
cd smartgarden-infra && cp .env.example .env   # troque cada troque_esta_senha (openssl rand -hex 16)
docker compose up -d && cd ..

# 2. Backend (porta 8080)
cd smartgarden-back && mvn spring-boot:run

# 3. Frontend (porta 4200, com proxy de /api para o backend)
cd smartgarden-front && pnpm install && pnpm start
```

Para gravar o ESP32, crie `smartgarden-firmware/esp32-dht22-serial/secrets.h` a partir de `secrets.example.h` (veja o README do firmware).

## Segredos

Não são versionados: `smartgarden-infra/.env` (senhas do banco e do broker, lidas pelo Compose e pelo backend) e `smartgarden-firmware/esp32-dht22-serial/secrets.h` (Wi-Fi e senha MQTT do ESP32). O `.env.example` e o `secrets.example.h` só têm marcadores.
