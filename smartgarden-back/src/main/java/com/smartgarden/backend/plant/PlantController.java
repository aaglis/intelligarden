package com.smartgarden.backend.plant;

import com.smartgarden.backend.common.ApiErrorResponse;
import com.smartgarden.backend.plant.dto.CreatePlantRecordRequest;
import com.smartgarden.backend.plant.dto.CreatePlantRequest;
import com.smartgarden.backend.plant.dto.PlantRecordResponse;
import com.smartgarden.backend.plant.dto.PlantResponse;
import com.smartgarden.backend.plant.dto.PlantWaterHistoryResponse;
import com.smartgarden.backend.plant.dto.UpdatePlantRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plants")
@Tag(name = "Plants", description = "Jardim virtual: água estimada, registro diário e irrigação simulada")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @GetMapping
    @Operation(summary = "Listar plantas", description = "Plantas com a água estimada agora, opcionalmente só as de um sensor.")
    public List<PlantResponse> listPlants(@RequestParam(required = false) String deviceCode) {
        return plantService.listPlants(deviceCode);
    }

    @GetMapping("/{plantCode}")
    @Operation(summary = "Consultar planta")
    @ApiResponse(responseCode = "404", description = "Planta nao encontrada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public PlantResponse getPlant(@PathVariable String plantCode) {
        return plantService.getPlant(plantCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar planta")
    @ApiResponse(responseCode = "409", description = "Ja existe planta com o codigo",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public PlantResponse createPlant(@Valid @RequestBody CreatePlantRequest request) {
        return plantService.createPlant(request);
    }

    @PatchMapping("/{plantCode}")
    @Operation(summary = "Alterar planta", description = "Só os campos enviados mudam. Mudar a capacidade mantém a porcentagem de água.")
    public PlantResponse updatePlant(@PathVariable String plantCode, @Valid @RequestBody UpdatePlantRequest request) {
        return plantService.updatePlant(plantCode, request);
    }

    @GetMapping("/{plantCode}/records")
    @Operation(summary = "Últimos registros", description = "Pesagens, regas, irrigações e observações, do mais novo ao mais antigo.")
    public List<PlantRecordResponse> listRecords(
            @PathVariable String plantCode,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return plantService.listRecords(plantCode, limit);
    }

    @GetMapping("/{plantCode}/water-history")
    @Operation(summary = "Curva da água estimada",
            description = "Água estimada no período (um ponto a cada 10 min, mais um antes e um depois de cada rega, irrigação ou pesagem).")
    public PlantWaterHistoryResponse waterHistory(
            @PathVariable String plantCode,
            @RequestParam(defaultValue = "72") int hours,
            @RequestParam(defaultValue = "500") int limit
    ) {
        return plantService.waterHistory(plantCode, hours, limit);
    }

    @PostMapping("/{plantCode}/records")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registro diário",
            description = "weighing (peso em g; fullPot=true logo depois de regar e escorrer), "
                    + "watering (mL; drained=true se escorreu) ou observation (note). "
                    + "A pesagem compara a perda medida com a estimada e corrige a estimativa."
    )
    public PlantRecordResponse addRecord(
            @PathVariable String plantCode,
            @Valid @RequestBody CreatePlantRecordRequest request
    ) {
        return plantService.addRecord(plantCode, request);
    }

    @PostMapping("/{plantCode}/irrigations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Simular irrigação",
            description = "Coloca a rega recomendada na água estimada e acende o LED do sensor. "
                    + "Recusado (409) para vasos reais (autoIrrigation=false)."
    )
    public PlantRecordResponse irrigate(@PathVariable String plantCode) {
        return plantService.irrigate(plantCode);
    }
}
