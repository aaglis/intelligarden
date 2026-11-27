/* Atalhos para a página 3D. O relógio do navegador fica parado no mesmo instante do relógio do servidor. */
import { expect } from '@playwright/test';

export async function abrirPagina(page, quando) {
  await page.clock.setFixedTime(new Date(quando));
  await page.goto('/');
  await expect(page.locator('#app'), 'a página terminou de carregar').not.toHaveClass(/is-loading/, { timeout: 90_000 });
  await expect(page.locator('#demo-tag'), 'dados do servidor, não os de exemplo').toBeHidden();
}

export async function abrirPlanta(page, code) {
  await page.locator(`#plist .pbtn[data-id="${code}"]`).click();
  await expect(page.locator('#sheet')).toHaveClass(/open/);
  await expect(page.locator('#s-wnum')).toBeVisible();
}

export async function abrirSensor(page) {
  await page.locator('#sensor-btn').click();
  await expect(page.locator('#s-reads')).toBeVisible();
}

/** Lê "Temperatura 25,0 °C", "Umidade 50 %", "VPD 1,58 kPa" do painel do sensor. */
export async function leiturasDoPainel(page) {
  const blocos = page.locator('#s-reads > div');
  const out = {};
  for (let i = 0; i < await blocos.count(); i++) {
    const nome = (await blocos.nth(i).locator('.lbl').textContent()).trim();
    const valor = (await blocos.nth(i).locator('.read').evaluate((el) => el.firstChild.textContent)).trim();
    out[nome] = valor;
  }
  return out;
}

/** Guarda todos os avisos (toasts) que aparecerem, para conferir depois mesmo que já tenham sumido. */
export async function gravarAvisos(page) {
  await page.evaluate(() => {
    window.__avisos = [];
    const t1 = document.getElementById('toast-t1'), t2 = document.getElementById('toast-t2');
    new MutationObserver(() => window.__avisos.push(`${t1.textContent} | ${t2.textContent}`))
      .observe(t2, { childList: true, characterData: true, subtree: true });
  });
}
export const avisos = (page) => page.evaluate(() => window.__avisos);

/** Abre Registrar → Pesagem, digita o peso e devolve o texto da prévia que aparece antes de salvar. */
export async function preencherPesagem(page, peso, cheio) {
  await page.locator('#sheet [data-act="registro"]').click();
  await expect(page.locator('#reg')).toBeVisible();
  await page.locator('#tab-pesagem').click();
  await page.locator('#peso').fill(String(peso));
  if (cheio) await page.locator('#peso-cheio').check();
  await expect(page.locator('#peso-cmp')).toBeVisible();
  return (await page.locator('#peso-cmp').innerText()).replace(/\s+/g, ' ');
}

export async function salvarRegistro(page) {
  await page.locator('#reg-save').click();
  await expect(page.locator('#reg')).toBeHidden();
}

/** Primeiro item de "Últimos registros", em uma linha. */
export async function ultimoRegistro(page) {
  return (await page.locator('#s-log li').first().innerText()).replace(/\s+/g, ' ');
}

/** "amanhã, por volta das 13:30" → instante (Fortaleza, UTC−3), a partir do "agora" parado da página. */
export function horarioPrevisto(texto, agora) {
  const m = texto.match(/(hoje|amanhã), por volta das (\d{2}):(\d{2})/);
  if (!m) return null;
  const hoje = new Date(agora - 3 * 3600e3);
  const dia = Date.UTC(hoje.getUTCFullYear(), hoje.getUTCMonth(), hoje.getUTCDate()) + (m[1] === 'amanhã' ? 864e5 : 0);
  return dia + (Number(m[2]) + 3) * 3600e3 + Number(m[3]) * 60e3;
}

/**
 * A porcentagem da ficha anima até o valor novo; espera assentar e confere com a conta
 * (backend com 1 casa, página sem casas: folga de 0,55 ponto).
 */
export async function esperarAguaNaFicha(page, exato, oQue = 'água na ficha') {
  await expect.poll(async () => Math.abs(Number(await page.locator('#s-wnum').textContent()) - exato), {
    message: `${oQue}: conta ${exato.toFixed(2)} %`
  }).toBeLessThanOrEqual(0.55 + 1e-9);
}
