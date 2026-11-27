package com.smartgarden.backend.plant.dto;

final class Numbers {

    private Numbers() {
    }

    static Double round(Double value, int decimals) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
