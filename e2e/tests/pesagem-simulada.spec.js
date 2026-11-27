/* As pesagens do simulador (smartgarden-infra/simular-pesagens.py) aparecem marcadas como simuladas na ficha. */
import { test, expect } from '@playwright/test';
import { clima, DIA } from '../lib/modelo.js';
import { relogio, proximaManha, leitura, leituras, registrar } from '../lib/api.js';
import { abrirPagina, abrirPlanta } from '../lib/pagina.js';

const CODE = 'jiboia';
/* as mesmas notas que o simulador grava */
const NOTA_CHEIO = 'Pesagem simulada: regado até escorrer, vaso cheio (teste, sem vaso real)';
const NOTA = 'Pesagem simulada (teste, sem vaso real)';

test('pesagens do simulador aparecem como simuladas no registro, no gráfico e na calibração', async ({ page, request }) => {
  const dia1 = await proximaManha(request), dia2 = dia1 + DIA;
  await relogio(request, dia1);
  await leitura(request, { t: 25.0, ur: 60.0, quando: dia1 });
  await registrar(request, CODE, { type: 'weighing', weightGrams: 960, fullPot: true, note: NOTA_CHEIO });
  const dia = clima({ inicio: dia1, horas: 24, passoMin: 30, t: [24, 31], ur: [42, 65] });
  await relogio(request, dia2);
  await leituras(request, dia);
  await registrar(request, CODE, { type: 'weighing', weightGrams: 930, fullPot: false, note: NOTA });

  await abrirPagina(page, dia2);
  await abrirPlanta(page, CODE);
  const ultimo = page.locator('#s-log li').first();
  await expect(ultimo).toContainText('Pesagem simulada: 930 g');
  await expect(ultimo).toContainText('balança simulada:');
  await expect(page.locator('#s-log li').nth(1)).toContainText('Pesagem simulada: 960 g');
  await expect(page.locator('#s-fine')).toContainText('Conferida com 1 pesagem simulada');
  await expect(page.locator('#s-water .w-legend')).toContainText('pesagem simulada');
});
