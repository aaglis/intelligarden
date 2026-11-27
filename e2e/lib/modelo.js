/*
 * Conta de referência: o modelo da água refeito aqui, à parte do backend, a partir do que está documentado.
 *
 *   FAO-56 (Allen et al., 1998), eq. 11   pressão de saturação  e°(T) = 0,6108 · exp(17,27·T / (T + 237,3))  kPa
 *   VPD (déficit de pressão de vapor)     VPD = e°(T) · (1 − UR/100)
 *   perda do vaso                         mL = k · VPD médio do intervalo (trapézio) · horas, lacuna máxima de 12 h
 *   pesagem (1 g = 1 mL)                  k medido = perda medida / soma de VPD·h desde a pesagem anterior (≥ 3 kPa·h)
 *   calibração                            k sugerido = mediana das últimas 10; erro = |estimado − medido| / medido
 *
 * Fontes: docs/fichas-das-plantas.md e docs/pesagem-diaria.md. O teste da Tabela 2.3 da FAO-56 ancora a
 * fórmula em valores publicados, que não dependem de nenhuma das duas implementações.
 */

/** FAO-56, Anexo 2, Tabela 2.3: e°(T) em kPa. */
export const TABELA_2_3_FAO56 = [[10, 1.228], [15, 1.705], [20, 2.338], [25, 3.168], [30, 4.243], [35, 5.623]];

export const HORA = 3600e3;
export const DIA = 24 * HORA;
const LACUNA_MAX_H = 12;
const INTERVALO_IRRIGACAO_MS = 30 * 60e3;
const MIN_VPD_HORAS_CALIBRACAO = 3;
const JANELA_CALIBRACAO = 10;

const limitar = (v, min, max) => Math.min(max, Math.max(min, v));
const ms = (quando) => (typeof quando === 'number' ? quando : Date.parse(quando));

export const pressaoSaturacao = (t) => 0.6108 * Math.exp((17.27 * t) / (t + 237.3));
export const vpd = (t, ur) => pressaoSaturacao(t) * (1 - limitar(ur, 0, 100) / 100);

export function mediana(ordenados) {
  if (!ordenados.length) return null;
  const meio = Math.floor(ordenados.length / 2);
  return ordenados.length % 2 ? ordenados[meio] : (ordenados[meio - 1] + ordenados[meio]) / 2;
}

/** Um vaso acompanhado pela conta de referência, leitura a leitura. */
export class Vaso {
  constructor({ capacidadeMl, limitePercent, k, irrigacaoMl = 0, automatico = false }) {
    Object.assign(this, { capacidadeMl, limitePercent, k, irrigacaoMl, automatico });
    this.aguaMl = null;
    this.vpdAnterior = null;
    this.ultimaLeitura = null;
    this.ultimaRega = null;
    this.pesoCheio = null;
    this.ultimoPeso = null;
    this.perdaDesdePesagem = 0;
    this.vpdHorasDesdePesagem = 0;
    this.regouDesdePesagem = false;
    this.comparacoes = [];
    this.irrigacoes = [];
    this.curva = [];
  }

  get percent() { return (100 * this.aguaMl) / this.capacidadeMl; }
  get ritmoMlPorHora() { return this.k * this.vpdAnterior; }
  get vpdAtual() { return this.vpdAnterior; }
  /** Horas até o limite com o clima da última leitura (0 se já chegou). */
  get horasAteLimite() {
    const limiteMl = (this.capacidadeMl * this.limitePercent) / 100;
    return this.aguaMl <= limiteMl ? 0 : (this.aguaMl - limiteMl) / this.ritmoMlPorHora;
  }

  /** Primeira leitura conhecida: só marca o horário e o VPD, sem perda. */
  ancorar({ t, ur, quando, ultimaRega = null }) {
    this.vpdAnterior = vpd(t, ur);
    this.ultimaLeitura = ms(quando);
    this.ultimaRega = ultimaRega;
  }

  leitura({ t, ur, quando }) {
    const agora = ms(quando), v = vpd(t, ur);
    const horas = limitar((agora - this.ultimaLeitura) / HORA, 0, LACUNA_MAX_H);
    const vpdHoras = ((this.vpdAnterior + v) / 2) * horas;
    const perda = Math.min(this.aguaMl, this.k * vpdHoras);
    this.aguaMl -= perda;
    this.perdaDesdePesagem += perda;
    this.vpdHorasDesdePesagem += vpdHoras;
    this.vpdAnterior = v;
    this.ultimaLeitura = agora;

    let irrigacao = null;
    const podeIrrigar = this.ultimaRega == null || agora - this.ultimaRega >= INTERVALO_IRRIGACAO_MS;
    if (this.automatico && this.percent <= this.limitePercent && podeIrrigar) {
      irrigacao = { quando: agora, percentAntes: this.percent, ml: this.irrigacaoMl };
      this.aguaMl = Math.min(this.capacidadeMl, this.aguaMl + this.irrigacaoMl);
      this.ultimaRega = agora;
      this.regouDesdePesagem = true;
      this.irrigacoes.push(irrigacao);
    }
    this.curva.push({ quando: agora, aguaMl: this.aguaMl, percent: this.percent });
    return { perda, irrigacao };
  }

  pesagem({ peso, cheio }) {
    let perdaEstimada = null, perdaMedida = null, kMedido = null;
    if (!cheio && this.ultimoPeso != null && !this.regouDesdePesagem) {
      perdaEstimada = this.perdaDesdePesagem;
      perdaMedida = this.ultimoPeso - peso;
      if (perdaMedida > 0 && this.vpdHorasDesdePesagem >= MIN_VPD_HORAS_CALIBRACAO) {
        kMedido = perdaMedida / this.vpdHorasDesdePesagem;
      }
    }
    let aguaMedidaPercent = null;
    if (cheio) {
      this.pesoCheio = peso;
      this.aguaMl = this.capacidadeMl;
      aguaMedidaPercent = 100;
    } else if (this.pesoCheio != null) {
      this.aguaMl = limitar(this.capacidadeMl - (this.pesoCheio - peso), 0, this.capacidadeMl);
      aguaMedidaPercent = this.percent;
    }
    const vpdHoras = this.vpdHorasDesdePesagem;
    this.ultimoPeso = peso;
    this.perdaDesdePesagem = 0;
    this.vpdHorasDesdePesagem = 0;
    this.regouDesdePesagem = false;
    if (perdaMedida != null) this.comparacoes.push({ perdaEstimada, perdaMedida, kMedido });
    return { perdaEstimada, perdaMedida, kMedido, aguaMedidaPercent, vpdHoras };
  }

  get calibracao() {
    const ultimas = this.comparacoes.slice(-JANELA_CALIBRACAO);
    const ks = ultimas.map((c) => c.kMedido).filter((k) => k != null).sort((a, b) => a - b);
    const erros = ultimas.filter((c) => c.perdaMedida > 0)
      .map((c) => (Math.abs(c.perdaEstimada - c.perdaMedida) / c.perdaMedida) * 100);
    return {
      amostras: ks.length,
      kSugerido: mediana(ks),
      erroMedioPercent: erros.length ? erros.reduce((a, b) => a + b, 0) / erros.length : null
    };
  }
}

/**
 * Clima de um dia de apartamento: mais quente e mais seco às 15h, valores com 0,1 de resolução como o DHT22.
 * Devolve as leituras de `inicio` (exclusive) até `inicio + horas` (inclusive), a cada `passoMin` minutos.
 */
export function clima({ inicio, horas, passoMin, t: [tMin, tMax], ur: [urMin, urMax] }) {
  const out = [];
  for (let m = passoMin; m <= horas * 60; m += passoMin) {
    const quando = ms(inicio) + m * 60e3;
    const horaLocal = new Date(quando - 3 * HORA).getUTCHours() + new Date(quando).getUTCMinutes() / 60;
    const calor = (1 + Math.cos(((horaLocal - 15) / 24) * 2 * Math.PI)) / 2; /* 1 às 15h, 0 às 3h */
    out.push({
      quando,
      t: Math.round((tMin + (tMax - tMin) * calor) * 10) / 10,
      ur: Math.round((urMax - (urMax - urMin) * calor) * 10) / 10
    });
  }
  return out;
}
