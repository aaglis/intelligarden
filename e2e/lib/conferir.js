/*
 * Como comparar o que aparece com a conta exata. A página mostra números arredondados (e o backend já manda
 * alguns arredondados), então a diferença aceita é exatamente a soma desses arredondamentos, nem mais nem menos.
 */
import { expect } from '@playwright/test';

/** "1.180" → 1180, "3,3" → 3.3 */
export const numero = (texto) => Number(String(texto).trim().replace(/\./g, '').replace(',', '.'));

/**
 * O valor mostrado bate com o exato, a menos dos arredondamentos pelo caminho.
 * `casas`: casas decimais de cada arredondamento (ex.: [1, 0] = backend com 1 casa, página sem casas).
 */
export function bateComArredondamento(mostrado, exato, casas, oQue) {
  const folga = casas.reduce((soma, c) => soma + 0.5 * 10 ** -c, 0) + 1e-9;
  expect(Math.abs(mostrado - exato), `${oQue}: mostrado ${mostrado}, conta ${exato.toFixed(4)}, folga ${folga}`)
    .toBeLessThanOrEqual(folga);
}

/** Valor que a API devolve arredondado com `casas` casas. */
export function bateComApi(valorApi, exato, casas, oQue) {
  bateComArredondamento(valorApi, exato, [casas], oQue);
}

/** Arredonda como o backend (Math.round com fator), para montar o texto que a página deve mostrar. */
export const arredondar = (v, casas) => Math.round(v * 10 ** casas) / 10 ** casas;
