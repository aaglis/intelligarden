-- Ajustes sustentados pelas fontes de docs/fichas-das-plantas.md (pesquisa de 26/11/2026):
-- samambaia: faixa ideal em interior 20–24 °C (UF/IFAS ENH1286; Clemson HGIC 1505; NC State; Cornell, classe "Cool");
-- espada-de-são-jorge: ideal 15–24 °C (RHS, "How to grow sansevierias");
-- jiboia: umidade preferida 50–70 % (Clemson HGIC 1568, "Preferred range: 50 to 70%");
-- manjericão: em vaso, rega pelo menos diária (Embrapa, Documentos 136), então regar a 50 %. A rega recomendada
-- leva do limite ao vaso cheio: 260 mL × 50 % = 130 mL.
update plants set temperature_min_c = 20, temperature_max_c = 24 where plant_code = 'samambaia';
update plants set temperature_max_c = 24 where plant_code = 'espada';
update plants set humidity_min_percent = 50 where plant_code = 'jiboia';
update plants set irrigation_threshold_percent = 50, irrigation_ml = 130,
    needs_water = case when water_capacity_ml > 0 and estimated_water_ml * 100.0 / water_capacity_ml <= 50 then true else false end
where plant_code = 'manjericao';
