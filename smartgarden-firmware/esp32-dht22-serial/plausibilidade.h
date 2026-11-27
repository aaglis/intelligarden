// Releitura do DHT22 quando a leitura não parece verdadeira.
//
// O DHT22 às vezes devolve uma leitura errada isolada (por exemplo, metade do valor real) ou falha (NaN).
// Antes de publicar, o ESP32 confere a leitura contra a última aceita; se for falha ou um salto impossível
// para uma sala (mais de 5 °C ou 20 pontos de umidade em menos de 2 minutos), espera o sensor e lê de novo,
// até 3 tentativas. Os limites são os mesmos que o backend usa para descartar leituras.
//
// Sem dependência do Arduino, para ser testado no computador: teste/teste_plausibilidade.cpp.
#pragma once

#include <cmath>

struct Leitura {
  float temperatura;
  float umidade;
};

enum class Problema { Nenhum, Falha, Salto };

struct Resultado {
  bool aceita;
  Leitura leitura;        // a leitura aceita, ou a última lida se nenhuma serviu
  int tentativas;         // quantas leituras foram feitas
  Problema problema;      // o que levou à última releitura (Nenhum se a primeira serviu)
};

const float MAX_SALTO_TEMPERATURA_C = 5.0f;
const float MAX_SALTO_UMIDADE = 20.0f;
const unsigned long JANELA_SALTO_MS = 120000UL;     // como no backend: depois disso, não compara mais
const unsigned long ESPERA_RELEITURA_MS = 2100UL;   // o DHT22 mede no máximo a cada 2 s
const int TENTATIVAS_LEITURA = 3;

// Faixa de medida do DHT22 (e a que o backend aceita).
inline bool leituraValida(Leitura l) {
  return !std::isnan(l.temperatura) && !std::isnan(l.umidade) &&
         l.temperatura >= -40.0f && l.temperatura <= 80.0f &&
         l.umidade >= 0.0f && l.umidade <= 100.0f;
}

inline bool saltoPlausivel(Leitura anterior, Leitura nova) {
  return std::fabs(nova.temperatura - anterior.temperatura) <= MAX_SALTO_TEMPERATURA_C &&
         std::fabs(nova.umidade - anterior.umidade) <= MAX_SALTO_UMIDADE;
}

// ler(): faz uma leitura do sensor. esperar(ms): espera sem travar o resto (MQTT, LED).
// compararCom: a última leitura aceita, ou nullptr se não há uma recente (menos de JANELA_SALTO_MS).
template <typename Ler, typename Esperar>
Resultado lerComReleitura(Ler ler, Esperar esperar, const Leitura* compararCom) {
  Resultado r{false, Leitura{NAN, NAN}, 0, Problema::Nenhum};
  for (int i = 0; i < TENTATIVAS_LEITURA; i++) {
    if (i > 0) {
      esperar(ESPERA_RELEITURA_MS);
    }
    r.leitura = ler();
    r.tentativas = i + 1;
    if (!leituraValida(r.leitura)) {
      r.problema = Problema::Falha;
      continue;
    }
    if (compararCom != nullptr && !saltoPlausivel(*compararCom, r.leitura)) {
      r.problema = Problema::Salto;
      continue;
    }
    r.aceita = true;
    return r;
  }
  return r;
}
