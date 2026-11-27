# Pesagem diária dos vasos reais

A água de cada vaso é **estimada** pelo clima que o sensor mede, sem sensor no solo. As pesagens mostram se essa estimativa acerta e ajustam o ritmo de perda de cada planta (o coeficiente `k`). Por isso elas são os dados mais importantes do projeto.

## Os vasos

| Planta | Código | Perde por dia (estimativa inicial) |
|---|---|---|
| Manjericão | `manjericao` | ~125 mL |
| Lírio-da-paz | `lirio` | ~70 mL |
| Jiboia | `jiboia` | ~30 mL |

Esses três têm a irrigação simulada desligada: a equipe rega de verdade. As outras plantas do 3D continuam virtuais.

Os vasos foram escolhidos porque perdem água num ritmo que dá para ver numa balança de cozinha, e cada um num ritmo diferente. Se a equipe tiver outras plantas, dá para trocar (veja o fim da página).

## Material

- Balança de cozinha com precisão de 1 g, **sempre a mesma**.
- Vaso com furo no fundo.
- O celular ou o computador com a página aberta: `http://IP-da-máquina:8080` na mesma rede, ou o link do túnel.

## Dia 1 (uma vez por vaso)

1. *(Opcional, mas vale muito.)* Se a terra estiver seca, pese o vaso **antes** de regar e anote o peso.
2. Regue até a água sair pelo furo. Deixe escorrer 15 minutos, **sem o pratinho**.
3. Pese.
4. Na página, abra a planta, toque em **Registrar** e escolha **Pesagem**. Digite o peso e marque **"Acabei de regar e escorrer: vaso cheio"**. Salve.

Esse peso vira a referência de vaso cheio.

Se você fez o passo 1, a diferença entre o vaso cheio e o vaso seco é a capacidade real do vaso, em mL. Passe esse número para quem cuida do backend, para ajustar a planta (veja "Depois de alguns dias").

## Todo dia

1. Pese **no mesmo horário** (por exemplo, 9h), **antes de qualquer rega** e **sem o pratinho**.
2. Na página: **Registrar**, **Pesagem**, digite o peso **sem** marcar "vaso cheio" e salve.

A prévia, e depois o histórico da planta, mostram "perdeu X mL, o sistema estimou Y mL". É essa comparação que interessa.

## Dia de regar

Regue quando a ficha da planta disser **"Chegou ao limite: regue e registre a rega"**, ou quando a terra estiver seca a dois dedos de profundidade.

1. Pese e registre a pesagem normal, como em todo dia.
2. Regue até escorrer pelo furo e espere 15 minutos.
3. Pese de novo e registre marcando **vaso cheio**.

Não precisa registrar a rega à parte: a pesagem de vaso cheio já reinicia a conta.

Se em algum dia regar sem pesar depois, registre em **Registrar**, **Rega**, com a quantidade e se escorreu. A pesagem seguinte então não entra na comparação, porque houve água no meio.

## Cuidados

- Não deixe água no pratinho na hora de pesar.
- Não mude os vasos de lugar: o sensor mede o ar daquele canto.
- Esqueceu um dia? Só continue no dia seguinte. A conta usa o tempo real entre as pesagens.
- Duas pesagens muito próximas (menos de 2 a 3 horas, conforme o clima) não entram na calibração.

## Depois de alguns dias

Desde a primeira comparação, a ficha da planta mostra o **erro médio** da estimativa e um **coeficiente sugerido** (a mediana das últimas pesagens). Espere umas 5 comparações antes de adotar o sugerido, para um dia atípico não puxar o número. Para passar a usar o sugerido, e corrigir a capacidade se ela foi medida no dia 1, alguém com acesso ao backend roda:

```bash
curl -X PATCH http://localhost:8080/api/plants/manjericao \
  -H 'Content-Type: application/json' \
  -d '{"lossCoefficient": 2.9, "waterCapacityMl": 240}'
```

Os números acima são só exemplos: use os da ficha e os da balança.

## Trocar um vaso

Para transformar outra planta em vaso real, ou devolver uma para a irrigação simulada:

```bash
curl -X PATCH http://localhost:8080/api/plants/samambaia \
  -H 'Content-Type: application/json' -d '{"autoIrrigation": false}'
```
