#!/usr/bin/env python3
"""
Simulador de pesagens: testa a calibração do sistema sem vasos de verdade.

Faz o papel da equipe com a balança nos vasos "reais" (plantas com a irrigação simulada desligada),
seguindo o protocolo de docs/pesagem-diaria.md:

  - primeira vez: pesagem de vaso cheio, que vira a referência;
  - depois, uma por dia: a perda de água vem do clima REAL que o sensor mediu desde a pesagem anterior,
    com um k "verdadeiro" escondido para cada planta (diferente do k da ficha), uma variação de um dia
    para o outro (±8 %) e o ruído de uma balança de cozinha (±1 g);
  - quando a balança mostra menos de 70 % de água, rega até escorrer e pesa de novo, marcando vaso cheio.

Se o sistema estiver certo, o k sugerido na ficha de cada planta chega perto do k verdadeiro abaixo.
Todo registro sai com a nota "Pesagem simulada", para separar das pesagens de verdade.

Uso:
  ./simular-pesagens.py            pesa agora
  ./simular-pesagens.py instalar   agenda uma vez por dia, às 9h (timer do systemd do usuário, sem sudo)
  ./simular-pesagens.py remover    tira o agendamento

Variáveis: SIMULADOR_API (padrão http://localhost:8080) e SIMULADOR_SENSOR (padrão esp32-jardim-bloco-a).
Só fala com o backend desta máquina (localhost).
"""
import json
import math
import os
import random
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

API = os.environ.get('SIMULADOR_API', 'http://localhost:8080').rstrip('/')
SENSOR = os.environ.get('SIMULADOR_SENSOR', 'esp32-jardim-bloco-a')

# O "vaso de verdade" de cada planta: o k que a calibração deve encontrar e o peso de vaso cheio (vaso + terra + água).
VASOS = {
    'manjericao': {'k_verdadeiro': 2.6, 'peso_cheio_g': 1180},
    'lirio': {'k_verdadeiro': 2.2, 'peso_cheio_g': 1650},
    'jiboia': {'k_verdadeiro': 0.75, 'peso_cheio_g': 960},
}
REGA_ABAIXO_DE = 70.0        # % de água pela balança
VARIACAO_DIARIA = 0.08       # desvio da perda de um dia para o outro
RUIDO_BALANCA_G = 1.0
INTERVALO_MINIMO_H = 6.0     # não pesa de novo o mesmo vaso antes disso
LACUNA_MAX_H = 12.0          # como no backend: sensor parado por muito tempo não conta mais que isso
NOTA = 'Pesagem simulada (teste, sem vaso real)'
NOTA_CHEIO = 'Pesagem simulada: regado até escorrer, vaso cheio (teste, sem vaso real)'


def chamar(metodo, caminho, corpo=None):
    dados = None if corpo is None else json.dumps(corpo).encode()
    req = urllib.request.Request(API + caminho, data=dados, method=metodo, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.load(resp)
    except urllib.error.HTTPError as erro:
        raise SystemExit(f'ERRO: {metodo} {caminho}: HTTP {erro.code} {erro.read().decode()[:300]}')
    except urllib.error.URLError as erro:
        raise SystemExit(f'ERRO: backend fora do ar em {API} ({erro.reason}).')


def quando(texto):
    return datetime.fromisoformat(texto.replace('Z', '+00:00'))


def vpd(t, ur):
    return 0.6108 * math.exp(17.27 * t / (t + 237.3)) * (1 - min(max(ur, 0.0), 100.0) / 100.0)


def vpd_horas_desde(inicio):
    """Integral do VPD (kPa·h) medida pelo sensor desde `inicio` até a última leitura (trapézio)."""
    q = urllib.parse.urlencode({'deviceCode': SENSOR, 'startAt': inicio.isoformat(), 'limit': 500})
    leituras = chamar('GET', f'/api/readings/history?{q}')['readings']
    pontos = sorted((quando(r['recordedAt']), vpd(float(r['temperatureC']), float(r['humidityPercent']))) for r in leituras)
    total, anterior = 0.0, None
    for t, v in pontos:
        if anterior is not None:
            horas = min((t - anterior[0]).total_seconds() / 3600.0, LACUNA_MAX_H)
            total += (anterior[1] + v) / 2.0 * horas
        anterior = (t, v)
    return total


def pesar(code, peso, cheio):
    nota = NOTA_CHEIO if cheio else NOTA
    return chamar('POST', f'/api/plants/{code}/records',
                  {'type': 'weighing', 'weightGrams': peso, 'fullPot': cheio, 'note': nota})


def simular(code, vaso, agora):
    p = chamar('GET', f'/api/plants/{code}')
    if p['autoIrrigation']:
        return f'{code}: pulado (irrigação simulada ligada, não é vaso real)'

    if p['fullWeightG'] is None or p['lastWeightG'] is None:
        peso = round(vaso['peso_cheio_g'] + random.gauss(0, 2))
        pesar(code, peso, True)
        return f'{code}: primeira pesagem, vaso cheio com {peso} g'

    desde = quando(p['lastWeighedAt'])
    if (agora - desde).total_seconds() < INTERVALO_MINIMO_H * 3600:
        return f'{code}: pulado (pesado há menos de {INTERVALO_MINIMO_H:.0f} h)'

    capacidade, cheio = p['waterCapacityMl'], p['fullWeightG']
    perda = vaso['k_verdadeiro'] * vpd_horas_desde(desde) * random.gauss(1, VARIACAO_DIARIA)
    seco = cheio - capacidade
    peso = round(max(seco, p['lastWeightG'] - max(perda, 0.0)) + random.gauss(0, RUIDO_BALANCA_G))
    r = pesar(code, peso, False)
    agua = 100.0 * (capacidade - (cheio - peso)) / capacidade
    texto = f'{code}: {peso} g, perdeu {r["measuredLossMl"]:.0f} mL'
    if r['estimatedLossMl'] is not None:
        texto += f' (o sistema estimou {r["estimatedLossMl"]:.0f} mL)'
    texto += f', {agua:.0f} % de água'

    if agua < REGA_ABAIXO_DE:
        peso_cheio = round(vaso['peso_cheio_g'] + random.gauss(0, 3))
        pesar(code, peso_cheio, True)
        texto += f'; regado, vaso cheio com {peso_cheio} g'
    return texto


def pesar_agora():
    host = urllib.parse.urlparse(API).hostname
    if host not in ('localhost', '127.0.0.1', '::1'):
        raise SystemExit(f'ERRO: o simulador só grava no backend desta máquina, não em {host}.')
    agora = quando(chamar('GET', f'/api/readings/latest?deviceCode={SENSOR}')['recordedAt'])
    print(f'{datetime.now():%Y-%m-%d %H:%M:%S} pesagens simuladas em {API}')
    for code, vaso in VASOS.items():
        print('  ' + simular(code, vaso, agora))


def unidades():
    pasta = Path.home() / '.config/systemd/user'
    return pasta, pasta / 'intelligarden-pesagem-simulada.service', pasta / 'intelligarden-pesagem-simulada.timer'


def instalar():
    pasta, servico, timer = unidades()
    pasta.mkdir(parents=True, exist_ok=True)
    servico.write_text(f"""[Unit]
Description=Pesagens simuladas dos vasos do IntelliGarden (teste, sem vaso real)

[Service]
Type=oneshot
Environment=SIMULADOR_API={API} SIMULADOR_SENSOR={SENSOR}
ExecStart=/usr/bin/env python3 "{Path(__file__).resolve()}"
""")
    timer.write_text("""[Unit]
Description=Pesagem simulada diária do IntelliGarden

[Timer]
# todo dia às 9h; se o computador estiver desligado nessa hora, roda assim que ele voltar
OnCalendar=*-*-* 09:00:00
Persistent=true
RandomizedDelaySec=5min

[Install]
WantedBy=timers.target
""")
    subprocess.run(['systemctl', '--user', 'daemon-reload'], check=True)
    subprocess.run(['systemctl', '--user', 'enable', '--now', timer.name], check=True)
    subprocess.run(['systemctl', '--user', 'list-timers', timer.name, '--no-pager'], check=True)


def remover():
    pasta, servico, timer = unidades()
    subprocess.run(['systemctl', '--user', 'disable', '--now', timer.name], check=False)
    for arquivo in (servico, timer):
        arquivo.unlink(missing_ok=True)
    subprocess.run(['systemctl', '--user', 'daemon-reload'], check=True)
    print('Agendamento removido. As pesagens já gravadas continuam no banco.')


if __name__ == '__main__':
    acao = sys.argv[1] if len(sys.argv) > 1 else ''
    {'': pesar_agora, 'instalar': instalar, 'remover': remover}.get(
        acao, lambda: sys.exit('uso: simular-pesagens.py [instalar|remover]'))()
