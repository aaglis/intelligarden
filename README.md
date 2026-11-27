# SmartGarden

Jardim virtual com ESP32 e sensor DHT22: o dispositivo mede temperatura e umidade do ar; o backend guarda as leituras, estima a água no vaso de cada planta pelo clima (VPD × k, calibrado com pesagens) e dispara a irrigação simulada (LED do ESP32) quando uma planta chega no limite. A interface 3D (`intelligarden-web`), servida pelo próprio backend, mostra o jardim com esses dados em tempo real, os gráficos de histórico e o registro diário.

```
ESP32 + DHT22 ──MQTT──▶ Mosquitto ──▶ Spring Boot ──▶ PostgreSQL
                                           │
                                           └──REST/SSE──▶ interface 3D (servida pelo backend)
```

## Estrutura

| Pasta | Conteúdo |
|---|---|
| [`smartgarden-firmware`](smartgarden-firmware) | Firmware do ESP32 (Arduino) |
| [`smartgarden-back`](smartgarden-back) | Backend Java 21 / Spring Boot (REST, MQTT, SSE, água estimada das plantas) |
| [`smartgarden-infra`](smartgarden-infra) | PostgreSQL e Mosquitto via Docker Compose |
| [`intelligarden-web`](intelligarden-web) | Interface 3D (IntelliGarden), servida pelo backend em `/` |

Cada pasta tem seu próprio README com instruções detalhadas.

## Subindo tudo localmente

```bash
# 1. Infra (PostgreSQL + MQTT)
cd smartgarden-infra && cp .env.example .env   # troque cada troque_esta_senha (openssl rand -hex 16)
docker compose up -d && cd ..

# 2. Backend (porta 8080) — também serve a interface 3D em http://localhost:8080/
cd smartgarden-back && mvn spring-boot:run
```

A interface 3D não precisa de um passo próprio: abra `http://localhost:8080/`, ou `http://IP-da-máquina:8080/` de outro aparelho da rede. Ela funciona sem internet. Para mexer na página sem reiniciar o backend, veja o README do `intelligarden-web`.

Para gravar o ESP32, crie `smartgarden-firmware/esp32-dht22-serial/secrets.h` a partir de `secrets.example.h` (veja o README do firmware).

## Vasos reais e pesagem diária

Manjericão, lírio-da-paz e jiboia são vasos reais: a equipe rega de verdade e pesa todo dia, e as pesagens conferem e calibram a água estimada. Passo a passo em [`docs/pesagem-diaria.md`](docs/pesagem-diaria.md).

De onde vem cada número das plantas (faixas de temperatura e umidade, rega, base do modelo de água), com as fontes: [`docs/fichas-das-plantas.md`](docs/fichas-das-plantas.md).

## Cópia de segurança

O banco tem as leituras e as pesagens, que são o resultado do projeto. `smartgarden-infra/backup.sh` faz uma cópia verificada, e `./backup.sh instalar` agenda uma por dia. A restauração está descrita no [README da infra](smartgarden-infra/README.md#cópia-de-segurança-do-banco).

## Segredos

Não são versionados: `smartgarden-infra/.env` (senhas do banco e do broker, lidas pelo Compose e pelo backend) e `smartgarden-firmware/esp32-dht22-serial/secrets.h` (Wi-Fi e senha MQTT do ESP32). O `.env.example` e o `secrets.example.h` só têm marcadores.
