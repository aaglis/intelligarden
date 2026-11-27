/*
 * Três dias do protocolo de pesagem (docs/pesagem-diaria.md) num vaso real, tudo pela página:
 * dia 1 vaso cheio; dia 2 pesagem comparada e rega; dia 3 outra pesagem comparada.
 * Confere a prévia antes de salvar, o registro, a água pela balança e a calibração (erro médio e k sugerido).
 */
import { test, expect } from '@playwright/test';
import { clima, DIA } from '../lib/modelo.js';
import { relogio, proximaManha, leitura, leituras, planta, registros, vasoDeReferencia } from '../lib/api.js';
import { abrirPagina, abrirPlanta, preencherPesagem, salvarRegistro, ultimoRegistro, esperarAguaNaFicha } from '../lib/pagina.js';
import { numero, bateComArredondamento, bateComApi } from '../lib/conferir.js';

const CODE = 'manjericao';
const mil = (n) => n.toLocaleString('pt-BR');

/** Pesagem comparada: prévia, registro salvo, ficha e backend, tudo contra a conta. */
async function pesagemComparada(page, request, vaso, peso) {
  const perdaMedida = vaso.ultimoPeso - peso;
  const perdaEstimada = vaso.perdaDesdePesagem;
  bateComApi((await planta(request, CODE)).estimatedLossSinceWeighingMl, perdaEstimada, 1, 'perda estimada desde a pesagem');

  const previa = await preencherPesagem(page, peso, false);
  expect(previa).toContain(`Perdeu ${mil(perdaMedida)} g, ou ${mil(perdaMedida)} mL de água`);
  const est = previa.match(/O sistema estimou ([\d.]+) mL desde a última pesagem · diferença de (\d+) %/);
  expect(est, previa).toBeTruthy();
  bateComArredondamento(numero(est[1]), perdaEstimada, [1, 0], 'perda estimada na prévia');
  /* a diferença da prévia sai dos dois números inteiros que ela mesma mostra */
  expect(numero(est[2])).toBe(Math.round((Math.abs(perdaMedida - numero(est[1])) / perdaMedida) * 100));
  const balanca = previa.match(/Pela balança, o vaso está com (\d+) % de água/);
  const medidaPercent = (100 * (vaso.capacidadeMl - (vaso.pesoCheio - peso))) / vaso.capacidadeMl;
  bateComArredondamento(numero(balanca[1]), medidaPercent, [0], 'água pela balança na prévia');

  await salvarRegistro(page);
  const ref = vaso.pesagem({ peso, cheio: false });

  const linha = await ultimoRegistro(page);
  expect(linha).toContain(`Pesagem: ${mil(peso)} g`);
  const reg = linha.match(/perdeu ([\d.]+) mL, o sistema estimou ([\d.]+) mL · balança: (\d+) % de água/);
  expect(reg, linha).toBeTruthy();
  expect(numero(reg[1])).toBe(perdaMedida);
  bateComArredondamento(numero(reg[2]), ref.perdaEstimada, [1, 0], 'perda estimada no registro');
  bateComArredondamento(numero(reg[3]), ref.aguaMedidaPercent, [1, 0], 'água pela balança no registro');
  await esperarAguaNaFicha(page, vaso.percent, 'água na ficha depois da pesagem');

  const r = (await registros(request, CODE, 1))[0];
  expect(r.type).toBe('weighing');
  expect(r.measuredLossMl).toBe(perdaMedida);
  bateComApi(r.estimatedLossMl, ref.perdaEstimada, 1, 'perda estimada gravada');
  bateComApi(r.measuredCoefficient, ref.kMedido, 3, 'k medido gravado');
  bateComApi(r.measuredWaterPercent, ref.aguaMedidaPercent, 1, 'água medida gravada');
}

/** Rodapé da ficha e backend: quantas pesagens, erro médio e k sugerido. */
async function conferirCalibracao(page, request, vaso) {
  const cal = vaso.calibracao;
  const d = await planta(request, CODE);
  expect(d.calibrationSamples).toBe(cal.amostras);
  bateComApi(d.suggestedCoefficient, cal.kSugerido, 3, 'k sugerido');
  bateComApi(d.meanErrorPercent, cal.erroMedioPercent, 1, 'erro médio');

  const n = cal.amostras;
  const fino = page.locator('#s-fine');
  await expect(fino).toContainText(`Conferida com ${n} ${n === 1 ? 'pesagem' : 'pesagens'}`);
  const m = (await fino.textContent()).match(/erro médio de ([\d.,]+) %\. Coeficiente de perda sugerido ([\d.,]+) \(em uso: ([\d.,]+)\)/);
  expect(m, await fino.textContent()).toBeTruthy();
  bateComArredondamento(numero(m[1]), cal.erroMedioPercent, [1, 0], 'erro médio na ficha');
  bateComArredondamento(numero(m[2]), cal.kSugerido, [3, 2], 'k sugerido na ficha');
  expect(numero(m[3])).toBe(vaso.k);
}

test('três dias de pesagem: prévia, registro, água pela balança e calibração batem com a conta', async ({ page, request }) => {
  const antes = await registros(request, CODE, 100);
  expect(antes.filter((r) => r.type === 'weighing' && r.measuredLossMl != null),
    'o servidor de teste começa sem pesagens comparadas').toHaveLength(0);

  const dia1 = await proximaManha(request), dia2 = dia1 + DIA, dia3 = dia2 + DIA;
  let vaso;

  await test.step('dia 1: vaso cheio', async () => {
    const ancora = { t: 25.0, ur: 60.0, quando: dia1 };
    await relogio(request, dia1);
    await leitura(request, ancora);
    await abrirPagina(page, dia1);
    await abrirPlanta(page, CODE);
    expect(await preencherPesagem(page, 1180, true)).toContain('Vira o peso de vaso cheio: 1.180 g');
    await salvarRegistro(page);
    expect(await ultimoRegistro(page)).toContain('Pesagem: 1.180 g');
    expect(await ultimoRegistro(page)).toContain('vaso cheio, virou a referência');
    await expect(page.locator('#s-wnum')).toHaveText('100');
    vaso = vasoDeReferencia(await planta(request, CODE), { ...ancora, peso: 1180 });
  });

  await test.step('dia 2: pesagem comparada e rega', async () => {
    const dia = clima({ inicio: dia1, horas: 24, passoMin: 15, t: [24, 31], ur: [42, 65] });
    await relogio(request, dia2);
    await leituras(request, dia);
    dia.forEach((l) => vaso.leitura(l));
    /* a balança mostra 15 % menos perda que a estimativa: um k real menor que o 3,3 da ficha */
    const peso = 1180 - Math.round(vaso.perdaDesdePesagem * 0.85);
    await abrirPagina(page, dia2);
    await abrirPlanta(page, CODE);
    await pesagemComparada(page, request, vaso, peso);
    await conferirCalibracao(page, request, vaso);

    /* dia de regar: regou até escorrer e pesou de novo, marcando vaso cheio */
    expect(await preencherPesagem(page, 1185, true)).toContain('Vira o peso de vaso cheio: 1.185 g');
    await salvarRegistro(page);
    vaso.pesagem({ peso: 1185, cheio: true });
    await expect(page.locator('#s-wnum')).toHaveText('100');
  });

  await test.step('dia 3: outra pesagem comparada, calibração com duas', async () => {
    const dia = clima({ inicio: dia2, horas: 24, passoMin: 15, t: [26, 33], ur: [35, 55] });
    await relogio(request, dia3);
    await leituras(request, dia);
    dia.forEach((l) => vaso.leitura(l));
    const peso = 1185 - Math.round(vaso.perdaDesdePesagem * 0.9);
    await abrirPagina(page, dia3);
    await abrirPlanta(page, CODE);
    await pesagemComparada(page, request, vaso, peso);
    await conferirCalibracao(page, request, vaso);

    /* a tabela do gráfico marca a pesagem de agora */
    await page.locator('#s-water details summary').click();
    const celula = page.locator('#s-water tbody tr').first().locator('td').nth(2);
    bateComArredondamento(numero((await celula.textContent()).replace('%', '')), vaso.percent, [1, 0], 'pesagem na tabela do gráfico');
  });
});
