// Testes da releitura do DHT22, no computador (sem placa). Da pasta smartgarden-firmware:
//   g++ -std=c++17 -Wall -Wextra -I esp32-dht22-serial teste/teste_plausibilidade.cpp -o /tmp/teste && /tmp/teste
// Sem g++ instalado, veja o comando com Docker no README do firmware.
#include "plausibilidade.h"

#include <cstdio>
#include <vector>

static int falhas = 0;

static void confere(bool ok, const char* oQue) {
  std::printf("%s %s\n", ok ? "ok  " : "FALHOU", oQue);
  if (!ok) falhas++;
}

// Sensor de mentira: devolve as leituras da lista, em ordem, e anota as esperas.
struct Roteiro {
  explicit Roteiro(std::vector<Leitura> lista) : leituras(lista) {}
  std::vector<Leitura> leituras;
  size_t proxima = 0;
  std::vector<unsigned long> esperas;
  Resultado rodar(const Leitura* anterior) {
    return lerComReleitura([this] { return leituras.at(proxima++); },
                           [this](unsigned long ms) { esperas.push_back(ms); }, anterior);
  }
};

static bool igual(Leitura a, Leitura b) {
  return a.temperatura == b.temperatura && a.umidade == b.umidade;
}

int main() {
  const Leitura anterior{27.0f, 55.0f};

  {
    Roteiro s({{27.3f, 54.0f}});
    Resultado r = s.rodar(&anterior);
    confere(r.aceita && r.tentativas == 1 && s.esperas.empty() && igual(r.leitura, {27.3f, 54.0f}),
            "leitura normal: aceita na primeira, sem esperar");
  }
  {
    Roteiro s({{13.5f, 27.5f}, {27.1f, 55.2f}});
    Resultado r = s.rodar(&anterior);
    confere(r.aceita && r.tentativas == 2 && igual(r.leitura, {27.1f, 55.2f}) && r.problema == Problema::Salto,
            "metade do valor (glitch): lê de novo e fica com a leitura certa");
    confere(s.esperas.size() == 1 && s.esperas[0] == ESPERA_RELEITURA_MS, "espera 2,1 s antes de ler de novo");
  }
  {
    Roteiro s({{NAN, NAN}, {NAN, 55.0f}, {27.2f, 55.1f}});
    Resultado r = s.rodar(&anterior);
    confere(r.aceita && r.tentativas == 3 && r.problema == Problema::Falha, "duas falhas (NaN) e depois a leitura certa");
  }
  {
    Roteiro s({{13.5f, 27.5f}, {13.6f, 27.4f}, {13.4f, 27.6f}});
    Resultado r = s.rodar(&anterior);
    confere(!r.aceita && r.tentativas == 3 && r.problema == Problema::Salto && s.esperas.size() == 2,
            "salto nas três tentativas: descarta, não publica");
  }
  {
    Roteiro s({{27.0f, 76.0f}, {27.0f, 76.0f}, {27.0f, 76.0f}});
    Resultado r = s.rodar(&anterior);
    confere(!saltoPlausivel(anterior, {27.0f, 76.0f}) && saltoPlausivel(anterior, {32.0f, 75.0f}),
            "limites iguais aos do backend: 5 °C e 20 pontos de umidade valem, mais que isso não");
    confere(!r.aceita && r.problema == Problema::Salto, "umidade 21 pontos acima nas três tentativas: descarta");
  }
  {
    Roteiro s({{34.0f, 30.0f}});
    Resultado r = s.rodar(nullptr);
    confere(r.aceita && r.tentativas == 1, "sem leitura recente para comparar: aceita a primeira válida");
  }
  {
    Roteiro s({{85.0f, 50.0f}, {-41.0f, 50.0f}, {27.0f, 101.0f}});
    Resultado r = s.rodar(nullptr);
    confere(!r.aceita && r.problema == Problema::Falha, "fora da faixa do DHT22 (-40 a 80 °C, 0 a 100 %): descarta");
  }

  std::printf("\n%s\n", falhas ? "HÁ FALHAS" : "todos os testes passaram");
  return falhas ? 1 : 0;
}
