/* Seis horas de clima numa planta virtual: a água que o backend estima e o que a ficha mostra, contra a conta. */
import { test, expect } from '@playwright/test';
import { clima, HORA } from '../lib/modelo.js';
import { relogio, proximaManha, leituras, planta, curva, comecarComVasoCheio } from '../lib/api.js';
import { abrirPagina, abrirPlanta, horarioPrevisto, esperarAguaNaFicha } from '../lib/pagina.js';
import { numero, bateComArredondamento, bateComApi } from '../lib/conferir.js';

const CODE = 'samambaia';

test('a água estimada desce com o clima como na conta, e a ficha mostra os números certos', async ({ page, request }) => {
  const t0 = await proximaManha(request);
  const vaso = await comecarComVasoCheio(request, CODE, { quando: t0, t: 26.0, ur: 55.0, peso: 1500 });
  const lista = clima({ inicio: t0, horas: 6, passoMin: 10, t: [24, 31], ur: [40, 62] });
  const fim = lista.at(-1).quando;
  await relogio(request, fim);
  await leituras(request, lista);
  for (const l of lista) vaso.leitura(l);

  await test.step('backend: estado da planta', async () => {
    const d = await planta(request, CODE);
    bateComApi(d.estimatedWaterMl, vaso.aguaMl, 1, 'água estimada (mL)');
    bateComApi(d.estimatedWaterPercent, vaso.percent, 1, 'água estimada (%)');
    bateComApi(d.lastVpdKpa, vaso.vpdAtual, 3, 'VPD da última leitura');
    bateComApi(d.lossMlPerHour, vaso.ritmoMlPorHora, 2, 'ritmo de perda (mL/h)');
    bateComApi(d.hoursToThreshold, vaso.horasAteLimite, 1, 'horas até o limite');
    bateComApi(d.estimatedLossSinceWeighingMl, vaso.perdaDesdePesagem, 1, 'perda desde a pesagem');
    expect(d.needsWater).toBe(false);
  });

  await test.step('backend: curva da água, um ponto a cada 10 minutos', async () => {
    const c = await curva(request, CODE, 12);
    const porHorario = new Map(c.points.map((p) => [Date.parse(p.recordedAt), p]));
    expect(vaso.curva).toHaveLength(36);
    for (const ref of vaso.curva) {
      const p = porHorario.get(ref.quando);
      expect(p, `ponto da curva às ${new Date(ref.quando).toISOString()}`).toBeTruthy();
      bateComArredondamento(p.waterMl, ref.aguaMl, [6], `curva às ${new Date(ref.quando).toISOString()}`);
    }
  });

  await abrirPagina(page, fim);
  await abrirPlanta(page, CODE);

  await test.step('ficha: água, ritmo de perda e próxima irrigação', async () => {
    await esperarAguaNaFicha(page, vaso.percent);
    const doca = await page.locator(`#plist .pbtn[data-id="${CODE}"] .pb-pct`).textContent();
    bateComArredondamento(numero(doca.replace('%', '')), vaso.percent, [1, 0], 'água na lista de plantas');

    const nota = await page.locator('#s-note').textContent();
    const ritmo = nota.match(/perde cerca de ([\d.,]+) mL de água por hora/);
    expect(ritmo, nota).toBeTruthy();
    bateComArredondamento(numero(ritmo[1]), vaso.ritmoMlPorHora, [2, 1], 'ritmo de perda na ficha');

    const proxima = await page.locator('#s-next').textContent();
    expect(proxima).toMatch(/^Próxima irrigação automática: (hoje|amanhã), por volta das \d{2}:\d{2}, se o clima continuar assim\.$/);
    const previsto = horarioPrevisto(proxima, fim);
    const exato = vaso.ultimaLeitura + vaso.horasAteLimite * HORA;
    /* o backend manda as horas com 1 casa (±3 min) e a página arredonda para 10 min (±5 min) */
    expect(Math.abs(previsto - exato) / 60e3, `${proxima} × ${new Date(exato).toISOString()}`).toBeLessThanOrEqual(8);
  });

  await test.step('ficha: tabela do gráfico da água', async () => {
    await page.locator('#s-water details summary').click();
    const celula = page.locator('#s-water tbody tr').first().locator('td').nth(1);
    bateComArredondamento(numero((await celula.textContent()).replace('%', '')), vaso.percent, [0], 'última hora na tabela');
  });
});
