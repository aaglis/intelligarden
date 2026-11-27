/* Planta virtual secando por 40 horas: a irrigação simulada tem de disparar na leitura certa, uma vez, com a quantidade certa. */
import { test, expect } from '@playwright/test';
import { clima } from '../lib/modelo.js';
import { relogio, proximaManha, leituras, planta, registros, comecarComVasoCheio } from '../lib/api.js';
import { abrirPagina, abrirPlanta, gravarAvisos, avisos, ultimoRegistro, esperarAguaNaFicha } from '../lib/pagina.js';
import { numero, bateComArredondamento, bateComApi } from '../lib/conferir.js';

const CODE = 'samambaia';

test('a irrigação automática dispara quando a água chega ao limite, e a página avisa na hora', async ({ page, request }) => {
  const t0 = await proximaManha(request);
  const vaso = await comecarComVasoCheio(request, CODE, { quando: t0, t: 30.0, ur: 40.0, peso: 1500 });
  const lista = clima({ inicio: t0, horas: 40, passoMin: 20, t: [29, 33], ur: [35, 45] });
  const fim = lista.at(-1).quando;
  lista.forEach((l) => vaso.leitura(l));
  expect(vaso.irrigacoes, 'o cenário cruza o limite uma vez').toHaveLength(1);
  const esperada = vaso.irrigacoes[0];

  /* página aberta antes das leituras: a irrigação chega pelo fluxo ao vivo */
  await relogio(request, fim);
  await abrirPagina(page, fim);
  await abrirPlanta(page, CODE);
  await gravarAvisos(page);
  await leituras(request, lista);

  await test.step('aviso na tela', async () => {
    await expect.poll(async () => (await avisos(page)).find((a) => a.startsWith('Irrigação automática · Samambaia')))
      .toBeTruthy();
    const aviso = (await avisos(page)).find((a) => a.startsWith('Irrigação automática · Samambaia'));
    expect(aviso).toContain(`${esperada.ml} mL`);
  });

  await test.step('backend: um registro, na leitura em que cruzou o limite', async () => {
    const novas = (await registros(request, CODE, 50)).filter((r) => r.type === 'irrigation' && Date.parse(r.recordedAt) > t0);
    expect(novas).toHaveLength(1);
    expect(new Date(novas[0].recordedAt).toISOString()).toBe(new Date(esperada.quando).toISOString());
    expect(novas[0].automatic).toBe(true);
    expect(novas[0].waterMl).toBe(esperada.ml);
    bateComApi(novas[0].estimatedWaterPercent, esperada.percentAntes, 1, 'água quando disparou (%)');
    expect(esperada.percentAntes).toBeLessThanOrEqual(vaso.limitePercent);
    bateComApi((await planta(request, CODE)).estimatedWaterMl, vaso.aguaMl, 1, 'água no fim (mL)');
  });

  await test.step('ficha: registro e água de agora', async () => {
    await expect(page.locator('#s-log li').first()).toContainText(`Irrigação automática: ${esperada.ml} mL`);
    const linha = await ultimoRegistro(page);
    const m = linha.match(/simulada, chegou ao limite \((\d+) %\)/);
    expect(m, linha).toBeTruthy();
    bateComArredondamento(numero(m[1]), esperada.percentAntes, [1, 0], 'água quando disparou, na ficha');
    await esperarAguaNaFicha(page, vaso.percent, 'água na ficha no fim');
  });
});
