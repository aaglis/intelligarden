package com.smartgarden.backend.e2e;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/** Só no perfil e2e: os testes leem e acertam o relógio do servidor por aqui. Não existe em produção. */
@RestController
@Profile("e2e")
@RequestMapping("/api/e2e/clock")
class E2eClockController {

    private final MutableClock clock;

    E2eClockController(MutableClock clock) {
        this.clock = clock;
    }

    @GetMapping
    ClockState get() {
        return new ClockState(OffsetDateTime.now(clock));
    }

    @PutMapping
    ClockState set(@RequestBody ClockState body) {
        clock.set(body.now().toInstant());
        return get();
    }

    record ClockState(OffsetDateTime now) {
    }
}
