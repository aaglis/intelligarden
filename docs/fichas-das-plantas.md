# Fichas das plantas: de onde vem cada número

Cada planta do IntelliGarden tem, no sistema:
- uma faixa ideal de temperatura;
- uma faixa ideal de umidade do ar;
- um limite de irrigação, em % da capacidade do vaso;
- a capacidade do vaso;
- a rega recomendada;
- o coeficiente de perda de água `k`, em mL por hora por kPa de VPD.

Esta página mostra o que a literatura sustenta, com a fonte de cada número, e o que é estimativa do projeto e por isso vai ser calibrado pela pesagem diária (veja [`pesagem-diaria.md`](pesagem-diaria.md)).

Pesquisa feita em 26/11/2026. Os trechos citados foram conferidos nas páginas e PDFs indicados. A lista de referências numeradas está no fim. Ficaram de fora fontes que não abriram (Missouri Botanical Garden, por erro de certificado; University of Minnesota Extension, por acesso negado).

## Base do modelo de água

| O que o sistema faz | Fonte |
|---|---|
| Calcula a pressão de saturação do vapor com e°(T) = 0,6108 · exp(17,27·T / (T + 237,3)), em kPa | FAO-56, cap. 3, eq. 11 [1] |
| Calcula a pressão real do vapor pela umidade relativa: ea = e°(T) · UR / 100 | FAO-56, cap. 3, eq. 10: "a ratio of the actual (ea) to the saturation (e°(T)) vapour pressure at the same temperature" [1] |
| Usa o déficit de pressão de vapor, VPD = e°(T) − ea, como medida de quão seco está o ar | FAO-56: "The vapour pressure deficit is the difference between the saturation (es) and actual vapour pressure (ea)" e "an accurate indicator of the actual evaporative capacity of the air" [1] |
| Estima a perda de água do vaso como perda (mL/h) = k × VPD | Grossiord et al. (2020): "when VPDL is low and stomata are fully open, T increases linearly with VPDL" [2] |
| Corrige a estimativa e calibra k pesando o vaso | FAO-56, lisímetros de pesagem: "the water loss is directly measured by the change of mass" [1b]. Vellame et al. (2012) usam vasos de 15 e 50 L que "medem diretamente a evapotranspiração", com resolução de hora em hora [3] |

**Limites do modelo, ditos com honestidade:**
- **A relação linear vale para VPD baixo a moderado.** Acima de um limiar, "transpiration rate increases with high VPD up to a point after which it either remains high or starts decreasing" [2]. Nas leituras de 24 a 26/11/2026, o VPD da sala ficou entre 1,1 e 1,9 kPa, com mediana de 1,4 kPa.
- **O k absorve outros fatores.** A transpiração depende também da energia, ou seja da luz, e do vento [1b]. Por isso cada planta tem o seu k, que inclui o lugar onde ela está (sol no parapeito, sombra no canto).
- **O mandacaru pode fugir da regra.** Cactos costumam abrir os estômatos à noite (metabolismo CAM). Não achamos fonte que confirme isso para a espécie, mas, se valer, a relação diurna com o VPD não se aplica bem a ele.

## Resumo por espécie

| Planta | No sistema | O que as fontes sustentam | Situação |
|---|---|---|---|
| Lírio-da-paz | 18–29 °C · UR 50–80 % · rega a 40 % | 18,3–29,4 °C [10]; 20–29,4 °C [6][8]; umidade "moderada" [9], sem número; substrato úmido, secando só um pouco [6][9] | Temperatura coerente. Umidade sem número na literatura. |
| Samambaia | 20–24 °C · UR 50–80 % · rega a 40 % | Interior: 20–24 °C [17]; umidade "alta" [18][19], sem número; "nunca deixar secar" [18] | Temperatura ajustada às fontes (era 18–27 °C). Umidade sem número na literatura. |
| Manjericão | 18–30 °C · UR 40–70 % · rega a 50 % | Ótimo de 25–30 °C, sobrevive até 10,9 °C [32]; em vaso, "irrigar pelo menos uma vez por dia" [30]; umidade constante no substrato [31] | Temperatura coerente. Limite de rega ajustado (era 30 %). |
| Mandacaru | 18–35 °C · UR 10–50 % · rega a 10 % | Cactos áridos: 18,3–29,4 °C no crescimento [36]; mudas cresceram bem a 31 °C e 46 % de UR [4]; regar a cada 2–3 semanas [37]; deixar secar [36] | Coerente, com a ressalva do CAM. |
| Jiboia | 18–30 °C · UR 50–70 % · rega a 35 % | 18–30 °C [14]; UR preferida 50–70 %, tolera 30–60 % [12]; deixar secar antes de regar [12][14] | Coerente; umidade ajustada (era 40–70 %). |
| Espada-de-são-jorge | 15–24 °C · UR 30–60 % · rega a 15 % | Ideal 15–24 °C [23]; umidade baixa [23][26]; regar a cada 2–4 semanas na estação de crescimento [25] e a cada 1–2 meses no inverno [24] | Temperatura ajustada às fontes (era 15–30 °C). |

## Fichas

### Lírio-da-paz (*Spathiphyllum wallisii*)
- **Temperatura:** "between 68 and 85 °F and nighttime temperatures about 10 °F cooler" (20–29,4 °C) [6]. Evitar menos de 15,6 °C [6]. A Cornell classifica a espécie como "Warm", de 65 a 85 °F (18,3–29,4 °C) [10]. A RHS aceita mais frio: "anywhere between 12–24°C" [9].
- **Umidade:** "Peace lilies prefer moderate humidity" [9]; pontas marrons nas folhas indicam umidade baixa [5]. Nenhuma fonte dá um número.
- **Rega:** "kept moist but not soggy and allowed to dry out slightly between waterings" [6]. A RHS manda regar "once the top few centimetres of compost have become dry" [9]. "The most common problem is underwatering" [8]; o encharcamento causa doenças na raiz [6]. A Cornell coloca a espécie na classe "Moist", que exige rega frequente [10].
- **Luz:** "tolerate low light conditions but grow best in bright indirect light" [6]; "No direct sun" [7].

### Samambaia (*Nephrolepis exaltata*)
- **Temperatura:** em interior, "Temperatures of 68°F to 75°F are most appropriate" (20–23,9 °C) [17]. Na produção comercial, 65–86 °F (18,3–30 °C) [17]. Outras fontes: dia de 20–22 °C e noite de 10–13 °C [19]; 15,6–21 °C [18]; mínima de 10 °C [22]. A Cornell põe a samambaia na classe "Cool", de 50 a 70 °F (10–21 °C) [10].
- **Umidade:** "This fern needs high humidity and moist soil that should never be allowed to dry out" [18]; "Needs high humidity to thrive" [19]; "moderate to high humidity" [21]. Nenhuma fonte dá um número para a espécie.
- **Rega:** "Keep the soil barely moist at all times" [19]. Se secar demais, a folhagem fica acinzentada e o crescimento desacelera [17]. No inverno, regar menos, porque o excesso apodrece a raiz [18][21]. Uma fonte diverge e a chama de "the most drought tolerant of the commonly cultivated ferns" [20].
- **Luz:** clara e indireta, sem sol direto [19][22]. Em interior, de 50 a 200 foot-candles, cerca de 540 a 2.150 lux [17].

### Manjericão (*Ocimum basilicum*)
- **Temperatura:** "The optimum temperature for basil growth is in the range of 25 and 30 °C, while the minimum temperature at which basil can survive is 10.9 °C" [32]. Uma revisão de 2023 dá ótimo entre 25 e 35 °C [33]. O manjericão quer noites acima de 10 °C [31].
- **Umidade:** não achamos faixa ideal publicada. O único número é um teto ligado ao míldio, doença causada por fungo: manter a umidade abaixo de 85 % reduz a produção de esporos [34].
- **Rega:** "O plantio pode ser feito em vasos… Irrigar pelo menos uma vez por dia" [30, p. 20]. O substrato precisa de "a relatively constant supply of soil moisture", sem encharcar e sem água parada no pratinho [31].
- **Luz:** sol pleno ("Put plants in a sunny window") [31]; "Recomenda-se o cultivo em locais ensolarados" [30].

### Mandacaru (*Cereus jamacaru*)
- **Temperatura:** cactos de clima árido preferem "temperatures from 65 to 85 F during the growing season" (18,3–29,4 °C) [36]. Mudas de mandacaru se desenvolveram bem em estufa com "average temperature of 31 °C and relative humidity of 46%" [4]. Para mudas, a Embrapa recomenda viveiro "em temperatura entre 25 °C e 33 °C" [35]. As sementes não germinam a 15 °C nem a 35 °C [38].
- **Umidade:** suculentas "usually thrive in the low humidity found in most homes" [37]. No estudo com mudas, a umidade era de 46 % [4]. Não achamos fonte para o mínimo de 10 %.
- **Rega:** "Typically once every two or three weeks works well" [37]; deixar secar entre regas, e o encharcamento apodrece a raiz [36]. Com mudas em estufa, o melhor regime foi encher até a capacidade do vaso uma vez por semana [4]. Na natureza, ocupa "áreas mais secas do Semiárido nordestino, em solos rasos e/ou em cima de rochas" [35].
- **Luz:** sol direto por várias horas [36]. A fonte é do hemisfério norte e indica janelas voltadas ao sul ou ao oeste; no Brasil, o equivalente são janelas ao norte ou ao oeste.

### Jiboia (*Epipremnum aureum*)
- **Temperatura:** "keep plants between 18–30°C (65–86°F)" [14]. Outras faixas: noite de 15,6–21 °C e dia de 21–29,4 °C [12]; 21–26,7 °C [13]; 15,6–26,7 °C [16].
- **Umidade:** "Preferred range: 50 to 70%"; a umidade média das casas, de 30 a 60 %, também é tolerada [12]. Outra fonte diz que ela não precisa de umidade alta [14]; uma terceira diverge e recomenda umidade alta [15].
- **Rega:** "Allow the soil to dry completely before watering" [12]; "Only water once the compost is approaching dryness" [14]. "Better kept too dry than too wet" [16]. Regar demais causa podridão na raiz [11].
- **Luz:** "prefers bright, indirect light and survives for long periods in low light" [11]; evitar sol pleno [14].

### Espada-de-são-jorge (*Dracaena trifasciata*)
- **Temperatura:** "ideally between 15–24°C (60–75°F)" [23]. Evitar menos de 12,8 °C [28]. É "very heat and drought resistant", mas a fonte não dá um número [29].
- **Umidade:** "Sansevierias like low humidity" [23]; "Keep moderate to warm temperatures and low humidity" [26].
- **Rega:** "allow the soil to dry completely between watering" [26]. Regar "every 2 to 4 weeks during the growing season" [25] e, no inverno, "only water every one to two months" [24]. Aguenta "a month or so" sem rega [27]. O excesso de água é a principal causa de perda da planta [26][23].
- **Luz:** clara e levemente sombreada; também aguenta pouca luz [23].

## O que não tem fonte numérica: estimativas do projeto

- **Limite de irrigação em % da capacidade.** Nenhuma fonte dá esse número. Ele traduz as orientações das fontes:
  - plantas que querem substrato sempre úmido (lírio, samambaia) são regadas cedo (40 %);
  - plantas que devem secar antes da rega (jiboia, espada, mandacaru) são regadas tarde (35 %, 15 % e 10 %).
- **Umidade em %.** Só a jiboia tem número na literatura [12]. Nas outras plantas, a faixa traduz "moderada", "alta" ou "baixa".
- **Coeficiente k e intervalo entre regas.** As fontes dão intervalo em dias só para a espada (2–4 semanas [25]), o mandacaru (2–3 semanas [37]) e o manjericão (diária, em vaso [30]). O k inicial de cada planta foi tirado de um intervalo típico com VPD de 1,5 kPa:

  k = capacidade × (1 − limite) / (1,5 × horas entre regas)

  **É justamente o número que a pesagem diária calibra** nos três vasos reais (manjericão, lírio e jiboia).
- **Capacidade do vaso.** Depende do vaso de verdade. Mede-se pela diferença entre o peso cheio e escorrido e o peso seco ([`pesagem-diaria.md`](pesagem-diaria.md)). Cassimiro et al. mediram a capacidade de forma parecida: água aplicada menos água drenada [4].

## Ajustes feitos a partir das fontes (migração V9, 26/11/2026)

| Planta | Antes | Agora | Por quê |
|---|---|---|---|
| Samambaia | 18–27 °C | 20–24 °C | Faixa de interior da UF/IFAS [17]; as outras fontes também ficam abaixo de 27 °C [10][18][19] |
| Espada-de-são-jorge | 15–30 °C | 15–24 °C | Faixa ideal da RHS [23]; nenhuma fonte dá número acima de 24 °C |
| Jiboia | UR 40–70 % | UR 50–70 % | "Preferred range: 50 to 70%" [12]; 40 % está só na faixa tolerada |
| Manjericão | regar a 30 % (rega de 180 mL) | regar a 50 % (rega de 130 mL) | Embrapa: em vaso, pelo menos uma rega por dia [30]; umidade constante no substrato [31]. Com 50 %, o intervalo cai para cerca de 1 dia |

O lírio e o mandacaru ficaram como estavam. O intervalo de rega da espada (cerca de 14 dias) já batia com as fontes [25].

Com as faixas corrigidas, a samambaia e a espada aparecem como "clima fora do ideal" quando a sala passa de 24 °C. Isso acontece com frequência: em 26/11/2026 a sala chegou a 30,8 °C. É o resultado honesto, porque o cantinho está mais quente do que essas duas plantas preferem.

## Referências

1. Allen, R. G.; Pereira, L. S.; Raes, D.; Smith, M. *Crop evapotranspiration — Guidelines for computing crop water requirements*. FAO Irrigation and Drainage Paper 56. Roma: FAO, 1998. Cap. 3, umidade do ar (eq. 10 e 11): https://www.fao.org/4/x0490e/x0490e07.htm
   - 1b. O mesmo, cap. 1, transpiração e lisímetros de pesagem: https://www.fao.org/4/x0490e/x0490e04.htm
2. Grossiord, C. et al. Plant responses to rising vapor pressure deficit. *New Phytologist*, 226(6):1550–1566, 2020. doi:10.1111/nph.16485. Manuscrito aceito: https://sperry.biology.utah.edu/publications/Grossiord_et_al_2020.pdf
3. Vellame, L. M.; Coelho Filho, M. A.; Coelho, E. F.; Fraga Júnior, E. F. Lisímetro de pesagem e de lençol freático de nível constante para uso em ambiente protegido. *Revista Caatinga*, 25(1):153–159, 2012. https://www.alice.cnptia.embrapa.br/alice/handle/doc/937887
4. Cassimiro, C. A. L. et al. Irrigation level and substrate type on the acclimatization and development of mandacaru (*Cereus jamacaru* DC.): an emblematic cactus from Brazilian semiarid region. *Scientific Reports*, 13:20547, 2023. https://www.nature.com/articles/s41598-023-47929-5
5. NC State Extension Gardener Plant Toolbox. *Spathiphyllum*. https://plants.ces.ncsu.edu/plants/spathiphyllum/
6. Clemson Cooperative Extension (HGIC 1512). *Peace Lily*. Rev. 2022. https://hgic.clemson.edu/factsheet/peace-lily/
7. Henny, R. J.; Chen, J. *Florida Foliage House Plant Care: Spathiphyllum* (ENH1216). UF/IFAS Extension. https://edis.ifas.ufl.edu/publication/EP477/pdf
8. Chen, J. et al. *Cultural Guidelines for Commercial Production of Interiorscape Spathiphyllum* (ENH958). UF/IFAS Extension. https://edis.ifas.ufl.edu/publication/EP161/pdf
9. Royal Horticultural Society. *How to grow peace lilies*. https://www.rhs.org.uk/plants/peace-lilies/how-to-grow-peace-lilies
10. Cornell Cooperative Extension, Suffolk County. *Growing Conditions for Indoor Plants*. https://s3.amazonaws.com/assets.cce.cornell.edu/attachments/75539/Growing_Conditions_for_Indoor_Plants.pdf
11. NC State Extension Gardener Plant Toolbox. *Epipremnum aureum*. https://plants.ces.ncsu.edu/plants/epipremnum-aureum/
12. Clemson Cooperative Extension (HGIC 1568). *How to Grow Pothos Indoors (Epipremnum spp.)*. https://hgic.clemson.edu/factsheet/how-to-grow-pothos-indoors-epipremnum-spp-care-cultivars-and-common-problems/
13. McConnell, D. B. et al. *Cultural Guidelines for Commercial Production of Interiorscape Epipremnum* (ENH894). UF/IFAS Extension. https://ask.ifas.ufl.edu/publication/EP151/pdf
14. Royal Horticultural Society. *How to grow epipremnum*. https://www.rhs.org.uk/plants/epipremnum/growing-guide
15. University of Wisconsin–Madison Extension. *Pothos, Epipremnum aureum*. https://hort.extension.wisc.edu/articles/pothos-epipremmum-aureum/
16. Penn State Extension. *Pothos as a Houseplant*. https://extension.psu.edu/pothos-as-a-houseplant
17. UF/IFAS Extension. *Cultural Guidelines for Commercial Production of Boston Fern* (ENH1286). 2018, rev. 2021. https://ask.ifas.ufl.edu/publication/EP550/pdf
18. NC State Extension Gardener Plant Toolbox. *Nephrolepis exaltata*. https://plants.ces.ncsu.edu/plants/nephrolepis-exaltata/
19. Clemson Cooperative Extension (HGIC 1505). *Indoor Ferns*. https://hgic.clemson.edu/factsheet/indoor-ferns/
20. University of Wisconsin–Madison Extension. *Boston Fern*. https://hort.extension.wisc.edu/articles/boston-fern-nephrolepis-exaltata-bostoniensis/
21. Royal Horticultural Society. *Nephrolepis exaltata*. https://www.rhs.org.uk/plants/11508/nephrolepis-exaltata/details
22. Texas A&M AgriLife Extension. *Boston Fern*. https://aggie-horticulture.tamu.edu/ornamental/a-reference-guide-to-plant-care-handling-and-merchandising/selected-foliage-flowering-plants/foliage-plants/boston-fern
23. Royal Horticultural Society. *How to grow sansevierias*. https://www.rhs.org.uk/plants/sansevieria/growing-guide
24. NC State Extension Gardener Plant Toolbox. *Dracaena trifasciata*. https://plants.ces.ncsu.edu/plants/dracaena-trifasciata/
25. Clemson Cooperative Extension. *Exciting Houseplant Selections for Beginners*. https://hgic.clemson.edu/exciting-houseplant-selections-for-beginners/
26. University of Illinois Extension. *Mother-in-Law Tongue*. https://extension.illinois.edu/houseplants/mother-law-tongue
27. Penn State Extension. *Snake Plant: A Forgiving, Low-maintenance Houseplant*. https://extension.psu.edu/snake-plant-a-forgiving-low-maintenance-houseplant
28. UConn Home & Garden Education Center. *Sensational Sansevieria*. 2024. https://homegarden.cahnr.uconn.edu/2024/03/28/sensational-sansevieria
29. Texas A&M AgriLife Extension. *Mother-In-Law's Tongue*. https://aggie-horticulture.tamu.edu/ornamental/a-reference-guide-to-plant-care-handling-and-merchandising/selected-foliage-flowering-plants/foliage-plants/mother-in-laws-tongue/
30. Pereira, R. C. A.; Moreira, M. R. *Manjericão: cultivo e utilização*. Documentos 136. Fortaleza: Embrapa Agroindústria Tropical, 2011. https://www.infoteca.cnptia.embrapa.br/bitstream/doc/900892/1/DOC11004.pdf
31. Clemson Cooperative Extension (HGIC 1327). *Basil*. https://hgic.clemson.edu/factsheet/basil/
32. Barickman, T. C. et al. Yield, Physiological Performance, and Phytochemistry of Basil (*Ocimum basilicum* L.) under Temperature Stress and Elevated CO2 Concentrations. *Plants*, 10(6):1072, 2021. doi:10.3390/plants10061072
33. Walters, K. J.; Tarr, S. T.; Lopez, R. G. Modeling purple basil, sage, spearmint, and sweet basil responses to daily light integral and mean daily temperature. *PLOS ONE*, 18(11):e0294905, 2023. doi:10.1371/journal.pone.0294905
34. Wyenandt, A.; Simon, J. *Controlling Basil Downy Mildew in the Greenhouse*. Rutgers Plant & Pest Advisory, 2020. https://plant-pest-advisory.rutgers.edu/controlling-basil-downy-mildew-in-the-greenhouse/
35. Correia, D. et al. *Produção de Mudas de Mandacaru*. Circular Técnica 39. Fortaleza: Embrapa Agroindústria Tropical, 2012. https://www.infoteca.cnptia.embrapa.br/bitstream/doc/951853/1/CIT12002.pdf
36. Clemson Cooperative Extension (HGIC 1502). *Indoor Cacti*. https://hgic.clemson.edu/factsheet/indoor-cacti/
37. Steil, A. *Growing Succulents Indoors*. Iowa State University Extension. https://yardandgarden.extension.iastate.edu/how-to/growing-succulents-indoors
38. Alencar, N. L. M.; Gomes-Filho, E.; Innecco, R. Cereus jamacaru seed germination and initial seedling establishment as a function of light and temperature conditions. *Scientia Agricola*, 69(1), 2012. doi:10.1590/S0103-90162012000100010
