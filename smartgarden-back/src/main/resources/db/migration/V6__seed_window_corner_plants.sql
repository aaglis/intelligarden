-- Plantas do protótipo 3D (cantinho da janela), ligadas ao sensor do jardim.
-- Os valores de k (mL por hora por kPa de VPD) são estimativas iniciais, tiradas do intervalo típico entre regas
-- de cada espécie com VPD de 1,5 kPa. Eles devem ser recalibrados com as pesagens dos vasos reais.
-- Capacidade = água que o vaso guarda entre "cheio e escorrido" e "seco"; a rega recomendada leva do limite ao cheio.

insert into devices (device_code, name, active, online, created_at, updated_at)
select 'esp32-jardim-bloco-a', 'ESP32 Jardim Bloco A', true, false, current_timestamp, current_timestamp
where not exists (select 1 from devices where device_code = 'esp32-jardim-bloco-a');

insert into plants (plant_code, name, scientific_name, spot, device_id, water_capacity_ml, irrigation_threshold_percent,
    irrigation_ml, loss_coefficient, auto_irrigation, temperature_min_c, temperature_max_c, humidity_min_percent,
    humidity_max_percent, estimated_water_ml, estimated_at, created_at, updated_at)
select 'lirio', 'Lírio-da-paz', 'Spathiphyllum wallisii', 'No chão, no canto · sombra clara', d.id, 330, 40, 200, 1.8, true, 18, 29, 50, 80,
    155.1, current_timestamp, current_timestamp, current_timestamp
from devices d where d.device_code = 'esp32-jardim-bloco-a';

insert into plants (plant_code, name, scientific_name, spot, device_id, water_capacity_ml, irrigation_threshold_percent,
    irrigation_ml, loss_coefficient, auto_irrigation, temperature_min_c, temperature_max_c, humidity_min_percent,
    humidity_max_percent, estimated_water_ml, estimated_at, created_at, updated_at)
select 'samambaia', 'Samambaia', 'Nephrolepis exaltata', 'Pendurada ao lado da janela · luz clara, sem sol direto', d.id, 400, 40, 240, 3.3, true, 18, 27, 50, 80,
    184.0, current_timestamp, current_timestamp, current_timestamp
from devices d where d.device_code = 'esp32-jardim-bloco-a';

insert into plants (plant_code, name, scientific_name, spot, device_id, water_capacity_ml, irrigation_threshold_percent,
    irrigation_ml, loss_coefficient, auto_irrigation, temperature_min_c, temperature_max_c, humidity_min_percent,
    humidity_max_percent, estimated_water_ml, estimated_at, created_at, updated_at)
select 'manjericao', 'Manjericão', 'Ocimum basilicum', 'No parapeito da janela · sol direto', d.id, 260, 30, 180, 3.3, true, 18, 30, 40, 70,
    88.4, current_timestamp, current_timestamp, current_timestamp
from devices d where d.device_code = 'esp32-jardim-bloco-a';

insert into plants (plant_code, name, scientific_name, spot, device_id, water_capacity_ml, irrigation_threshold_percent,
    irrigation_ml, loss_coefficient, auto_irrigation, temperature_min_c, temperature_max_c, humidity_min_percent,
    humidity_max_percent, estimated_water_ml, estimated_at, created_at, updated_at)
select 'mandacaru', 'Mandacaru', 'Cereus jamacaru', 'No parapeito da janela · sol direto', d.id, 130, 10, 120, 0.16, true, 18, 35, 10, 50,
    105.3, current_timestamp, current_timestamp, current_timestamp
from devices d where d.device_code = 'esp32-jardim-bloco-a';

insert into plants (plant_code, name, scientific_name, spot, device_id, water_capacity_ml, irrigation_threshold_percent,
    irrigation_ml, loss_coefficient, auto_irrigation, temperature_min_c, temperature_max_c, humidity_min_percent,
    humidity_max_percent, estimated_water_ml, estimated_at, created_at, updated_at)
select 'jiboia', 'Jiboia', 'Epipremnum aureum', 'No alto da estante · luz clara, sem sol direto', d.id, 230, 35, 150, 0.83, true, 18, 30, 40, 70,
    220.8, current_timestamp, current_timestamp, current_timestamp
from devices d where d.device_code = 'esp32-jardim-bloco-a';

insert into plants (plant_code, name, scientific_name, spot, device_id, water_capacity_ml, irrigation_threshold_percent,
    irrigation_ml, loss_coefficient, auto_irrigation, temperature_min_c, temperature_max_c, humidity_min_percent,
    humidity_max_percent, estimated_water_ml, estimated_at, created_at, updated_at)
select 'espada', 'Espada-de-são-jorge', 'Dracaena trifasciata', 'No chão, ao lado da estante · meia-sombra', d.id, 350, 15, 300, 0.6, true, 15, 30, 30, 60,
    252.0, current_timestamp, current_timestamp, current_timestamp
from devices d where d.device_code = 'esp32-jardim-bloco-a';
