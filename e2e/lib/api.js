/* Atalhos para o backend de teste (perfil e2e: banco em memória, relógio controlado pelo teste). */
import { expect } from '@playwright/test';
import { Vaso, DIA, HORA } from './modelo.js';

export const SENSOR = 'esp32-jardim-bloco-a';
const iso = (quando) => new Date(quando).toISOString();

async function ok(resposta, oQue) {
  if (!resposta.ok()) throw new Error(`${oQue}: HTTP ${resposta.status()} ${await resposta.text()}`);
  return resposta.status() === 204 ? null : resposta.json();
}

export async function relogio(request, quando) {
  const r = await ok(await request.put('/api/e2e/clock', { data: { now: iso(quando) } }), 'acertar o relógio');
  expect(Date.parse(r.now)).toBe(new Date(quando).getTime());
}

export async function horaDoServidor(request) {
  return Date.parse((await ok(await request.get('/api/e2e/clock'), 'ler o relógio')).now);
}

/** Próximo dia às 9h (Fortaleza, UTC−3), depois do relógio atual do servidor: cada teste começa num dia novo. */
export async function proximaManha(request) {
  const agora = await horaDoServidor(request);
  const diaLocal = Math.floor((agora - 3 * HORA) / DIA) + 1;
  return diaLocal * DIA + 3 * HORA + 9 * HORA;
}

export async function leitura(request, { t, ur, quando }) {
  return ok(await request.post('/api/readings', {
    data: { deviceCode: SENSOR, temperatureC: t, humidityPercent: ur, recordedAt: iso(quando) }
  }), `leitura de ${iso(quando)}`);
}

export async function leituras(request, lista) {
  for (const l of lista) await leitura(request, l);
}

export const planta = async (request, code) => ok(await request.get(`/api/plants/${code}`), `planta ${code}`);
export const registros = async (request, code, limite = 20) =>
  ok(await request.get(`/api/plants/${code}/records?limit=${limite}`), `registros de ${code}`);
export const curva = async (request, code, horas) =>
  ok(await request.get(`/api/plants/${code}/water-history?hours=${horas}&limit=1000`), `curva de ${code}`);
export const registrar = async (request, code, corpo) =>
  ok(await request.post(`/api/plants/${code}/records`, { data: corpo }), `registro em ${code}`);

/**
 * Deixa o vaso num estado conhecido, como no dia 1 da pesagem: uma leitura do sensor e, logo depois,
 * a pesagem de vaso cheio. Devolve a conta de referência já sincronizada com esse estado.
 */
export async function comecarComVasoCheio(request, code, { quando, t, ur, peso }) {
  await relogio(request, quando);
  await leitura(request, { t, ur, quando });
  await registrar(request, code, { type: 'weighing', weightGrams: peso, fullPot: true });
  return vasoDeReferencia(await planta(request, code), { quando, t, ur, peso });
}

/** Conta de referência com os parâmetros que a planta tem no backend e o estado de logo depois do vaso cheio. */
export function vasoDeReferencia(d, { quando, t, ur, peso }) {
  const vaso = new Vaso({
    capacidadeMl: d.waterCapacityMl,
    limitePercent: d.irrigationThresholdPercent,
    k: d.lossCoefficient,
    irrigacaoMl: d.irrigationMl,
    automatico: d.autoIrrigation
  });
  vaso.ancorar({ t, ur, quando, ultimaRega: d.lastWateredAt ? Date.parse(d.lastWateredAt) : null });
  vaso.pesagem({ peso, cheio: true });
  return vaso;
}
