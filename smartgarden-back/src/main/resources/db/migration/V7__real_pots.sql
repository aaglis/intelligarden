-- Vasos reais: a equipe pesa todo dia e rega de verdade, então a irrigação simulada fica desligada nessas plantas.
-- Escolha: perda de água que aparece numa balança de cozinha (1 g) e ritmos diferentes, para testar a estimativa
-- do rápido ao lento: manjericão (~125 mL/dia), lírio-da-paz (~70 mL/dia) e jiboia (~30 mL/dia).
-- Ficaram de fora o mandacaru (perde ~6 mL/dia, menos que o erro da balança) e a samambaia (pendurada e solta folhas).
-- Passo a passo da pesagem: docs/pesagem-diaria.md. Para trocar depois: PATCH /api/plants/{codigo} {"autoIrrigation": ...}.
update plants set auto_irrigation = false where plant_code in ('manjericao', 'lirio', 'jiboia');
