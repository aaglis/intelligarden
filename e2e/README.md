# Testes de ponta a ponta

Conferem as contas que o sistema faz para mostrar as métricas das plantas: o Playwright abre a página 3D, injeta leituras do sensor, registra pesagens pela própria página e compara cada número que aparece com uma conta de referência feita à parte.

## O que é conferido

| Teste | Situação | Números conferidos |
|---|---|---|
| `sensor-vpd` | Uma leitura de 25 °C / 50 % e outra de 30 °C / 40 %, com a página aberta | A fórmula da pressão de saturação contra a Tabela 2.3 da FAO-56; temperatura, umidade e VPD no painel do sensor; a atualização ao vivo, sem recarregar |
| `agua-estimada` | Samambaia (planta virtual), 6 h de clima, uma leitura a cada 10 min | Água estimada (mL e %), VPD, ritmo de perda, horas até o limite, os 36 pontos da curva da água; na ficha: porcentagem, lista de plantas, "perde cerca de X mL por hora", horário da próxima irrigação e a tabela do gráfico |
| `pesagem` | Manjericão (vaso real), 3 dias do protocolo de [`docs/pesagem-diaria.md`](../docs/pesagem-diaria.md), tudo pela página | Prévia antes de salvar (perda medida, estimada, diferença, água pela balança); registro salvo; água corrigida pela balança; k medido; calibração com 1 e 2 pesagens (erro médio e k sugerido) |
| `irrigacao` | Samambaia secando por 40 h, com a página aberta | A irrigação automática dispara uma vez, na leitura em que a água chega ao limite, com 240 mL; o aviso na tela; o registro na ficha; a água depois |

A diferença aceita entre o que aparece e a conta é só a dos arredondamentos pelo caminho. Exemplo: o backend manda a água com 1 casa e a página mostra sem casas, então a folga é de 0,55 ponto.

Para conferir se os testes pegam erro de verdade, foi colocado de propósito um erro sutil no backend: usar o VPD só da última leitura, em vez da média entre duas leituras. Isso muda a água em 0,3 mL, e 3 dos 5 testes falharam apontando o número errado.

## O que a conta de referência é, e o que ela não prova

[`lib/modelo.js`](lib/modelo.js) refaz o modelo documentado em [`docs/fichas-das-plantas.md`](../docs/fichas-das-plantas.md): VPD pela FAO-56, perda = k × VPD por hora, pesagem com 1 g = 1 mL. É o mesmo modelo, escrito de novo e de forma independente. Por isso os testes mostram que o backend e a página fazem a conta descrita, do jeito descrito, e mostram o resultado certo.

Eles não mostram que o modelo acerta a água real do vaso. Isso só as pesagens de verdade mostram, com o erro médio da ficha de cada planta.

## Como rodar

Pré-requisitos: Node 20 ou mais novo, Java 21 e Maven.

```bash
cd e2e
npm install
npx playwright install chromium   # só na primeira vez
npm test
npm run relatorio                 # relatório com fotos e rastro de cada teste que falhou
```

O `npm test` sobe sozinho um backend só para os testes e o desliga no fim. A bateria toda leva uns 2 minutos.

## O backend de teste

O Playwright roda `mvn spring-boot:test-run` com o perfil `e2e`:

- **Banco:** H2 em memória, que começa vazio a cada execução, só com as migrações. Nada vai para o PostgreSQL real.
- **MQTT:** desligado, para não disputar a conexão com o backend de verdade nem receber as leituras do ESP32.
- **Porta:** 18080 (troque com `E2E_PORT`). Pode rodar com o backend normal ligado na 8080.
- **Relógio:** parado num instante que o teste escolhe pela rota `/api/e2e/clock`, que só existe nesse perfil. É assim que um dia de leituras cabe em segundos. O relógio do navegador acompanha o do servidor.

Se alguma variável de ambiente apontar para outro banco ou ligar o MQTT, o backend de teste se recusa a subir. O código do perfil fica em `smartgarden-back/src/test/java/com/smartgarden/backend/e2e`.

## Sem placa de vídeo

O 3D desenha a cena o tempo todo. Por padrão os testes usam a placa de vídeo da máquina. Numa máquina sem GPU, rode `E2E_GL=software npm test`: o processador desenha a cena, e a bateria fica bem mais lenta.
