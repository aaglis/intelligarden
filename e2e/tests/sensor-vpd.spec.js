/* O VPD é a base de toda a conta da água. Primeiro a fórmula contra a tabela publicada; depois o que a página mostra. */
import { test, expect } from '@playwright/test';
import { TABELA_2_3_FAO56, pressaoSaturacao, vpd, HORA } from '../lib/modelo.js';
import { relogio, proximaManha, leitura, planta } from '../lib/api.js';
import { abrirPagina, abrirSensor, leiturasDoPainel } from '../lib/pagina.js';
import { numero, bateComArredondamento, bateComApi } from '../lib/conferir.js';

test('a pressão de saturação da conta de referência reproduz a Tabela 2.3 da FAO-56', () => {
  for (const [t, publicado] of TABELA_2_3_FAO56) {
    bateComArredondamento(pressaoSaturacao(t), publicado, [3], `e°(${t} °C)`);
  }
});

test('o painel do sensor mostra temperatura, umidade e VPD certos, e acompanha a leitura nova', async ({ page, request }) => {
  const t0 = await proximaManha(request);
  await relogio(request, t0);
  await leitura(request, { t: 25.0, ur: 50.0, quando: t0 });

  await abrirPagina(page, t0);
  await abrirSensor(page);
  let painel = await leiturasDoPainel(page);
  expect(painel.Temperatura).toBe('25,0');
  expect(painel.Umidade).toBe('50');
  /* 25 °C pela tabela: e° = 3,168 kPa; com 50 % de umidade, VPD = 1,584 kPa */
  bateComArredondamento(numero(painel.VPD), 3.168 * 0.5, [2, 3], 'VPD no painel (25 °C, 50 %)');
  bateComApi((await planta(request, 'samambaia')).lastVpdKpa, vpd(25, 50), 3, 'VPD guardado pelo backend');

  /* leitura nova com a página aberta: chega pelo fluxo ao vivo, sem recarregar */
  const t1 = t0 + HORA / 6;
  await relogio(request, t1);
  await leitura(request, { t: 30.0, ur: 40.0, quando: t1 });
  await expect(page.locator('#sensor-t')).toHaveText('30,0 °C');
  await expect(page.locator('#sensor-h')).toHaveText('40 %');
  await expect.poll(async () => (await leiturasDoPainel(page)).VPD).not.toBe(painel.VPD);
  painel = await leiturasDoPainel(page);
  /* 30 °C pela tabela: e° = 4,243 kPa; com 40 %, VPD = 2,546 kPa */
  bateComArredondamento(numero(painel.VPD), 4.243 * 0.6, [2, 3], 'VPD no painel (30 °C, 40 %)');
});
