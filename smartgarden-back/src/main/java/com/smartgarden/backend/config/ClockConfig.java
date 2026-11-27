package com.smartgarden.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relógio usado nas contas da água e nas leituras. Em produção é o relógio do sistema; os testes de ponta a ponta
 * (perfil e2e, em src/test) colocam no lugar um relógio controlado, para simular dias de clima em segundos.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
