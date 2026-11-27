package com.smartgarden.backend.e2e;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Só no perfil e2e (testes de ponta a ponta da pasta e2e/): troca o relógio do sistema por um controlado,
 * para os testes simularem dias de leituras e pesagens em segundos.
 */
@Configuration
@Profile("e2e")
class E2eClockConfig {

    @Bean
    @Primary
    MutableClock e2eClock() {
        return new MutableClock(Instant.now(), ZoneId.of("America/Fortaleza"));
    }

    /** Roda antes de qualquer bean (banco, Flyway, MQTT): os testes nunca podem tocar no banco ou no broker reais. */
    @Bean
    static SafetyCheck e2eSafetyCheck() {
        return new SafetyCheck();
    }

    static final class SafetyCheck implements BeanFactoryPostProcessor, EnvironmentAware {

        private Environment environment;

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            if (!environment.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:mem:")) {
                throw new IllegalStateException(
                        "Perfil e2e recusado: o banco precisa ser H2 em memória (confira SPRING_DATASOURCE_URL).");
            }
            if (environment.getProperty("app.mqtt.enabled", Boolean.class, true)) {
                throw new IllegalStateException(
                        "Perfil e2e recusado: o MQTT precisa estar desligado (confira SMARTGARDEN_MQTT_ENABLED).");
            }
        }
    }
}
