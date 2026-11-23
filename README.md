# SmartGarden

Sistema de monitoramento ambiental de jardins com ESP32 e sensor DHT22: o dispositivo mede temperatura e umidade do ar, o backend armazena e analisa as leituras e o frontend mostra dashboard, monitoramento em tempo real e relatórios.

```
ESP32 + DHT22 ──HTTP/MQTT──▶ Spring Boot ──▶ PostgreSQL
                                  │
                                  └──REST/SSE──▶ Angular
```

## Estrutura

| Pasta | Conteúdo |
|---|---|
| [`smartgarden-firmware`](smartgarden-firmware) | Firmware do ESP32 (Arduino) |
| [`smartgarden-back`](smartgarden-back) | Backend Java 21 / Spring Boot (REST, MQTT, SSE) |
| [`smartgarden-front`](smartgarden-front) | Frontend Angular 19 |
| [`smartgarden-infra`](smartgarden-infra) | PostgreSQL e Mosquitto via Docker Compose |

Cada pasta tem seu próprio README com instruções detalhadas.

## Subindo tudo localmente

```bash
# 1. Infra (PostgreSQL + MQTT)
cd smartgarden-infra && cp .env.example .env && docker compose up -d && cd ..

# 2. Backend (porta 8080)
cd smartgarden-back && mvn spring-boot:run

# 3. Frontend (porta 4200, com proxy de /api para o backend)
cd smartgarden-front && pnpm install && pnpm start
```

Para gravar o ESP32, crie `smartgarden-firmware/esp32-dht22-serial/secrets.h` a partir de `secrets.example.h` (veja o README do firmware).

## Segredos

Não são versionados: `smartgarden-infra/.env` e `smartgarden-firmware/esp32-dht22-serial/secrets.h`. Os valores em `.env.example` e os padrões do `application.yml` são credenciais **apenas de desenvolvimento local**.
