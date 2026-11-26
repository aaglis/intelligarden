package com.smartgarden.backend.plant;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PlantRecordType {
    /** Pesagem do vaso (gramas). */
    @JsonProperty("weighing")
    WEIGHING,
    /** Rega feita pela equipe (mL). */
    @JsonProperty("watering")
    WATERING,
    /** Irrigação simulada pelo sistema (automática ou pelo botão). */
    @JsonProperty("irrigation")
    IRRIGATION,
    /** Observação livre (folhas amareladas, praga, etc.). */
    @JsonProperty("observation")
    OBSERVATION
}
