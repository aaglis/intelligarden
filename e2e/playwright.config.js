import { defineConfig, devices } from '@playwright/test';

const PORT = Number(process.env.E2E_PORT || 18080);
/*
 * O 3D desenha a cena o tempo todo. Com a placa de vídeo da máquina fica leve; sem ela (E2E_GL=software,
 * para máquinas sem GPU), o processador desenha e cada clique demora bem mais.
 */
const GL_ARGS = process.env.E2E_GL === 'software'
  ? ['--use-angle=swiftshader', '--enable-unsafe-swiftshader']
  : ['--use-gl=angle', '--use-angle=gl-egl', '--ignore-gpu-blocklist'];

export default defineConfig({
  testDir: './tests',
  /* um servidor só, com estado: os testes rodam em fila */
  workers: 1,
  fullyParallel: false,
  timeout: 180_000,
  expect: { timeout: 20_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://127.0.0.1:${PORT}`,
    locale: 'pt-BR',
    timezoneId: 'America/Fortaleza',
    reducedMotion: 'reduce',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  },
  projects: [{
    name: 'chromium',
    use: {
      ...devices['Desktop Chrome'],
      channel: 'chromium',
      launchOptions: { args: GL_ARGS }
    }
  }],
  /*
   * Sobe um backend só para os testes: classpath de teste + perfil e2e = banco H2 em memória, MQTT desligado,
   * relógio controlado. Começa vazio a cada execução (só as migrações), por isso não reaproveita servidor aberto.
   */
  webServer: {
    command: 'mvn -q -f ../smartgarden-back/pom.xml spring-boot:test-run',
    url: `http://127.0.0.1:${PORT}/api/e2e/clock`,
    env: { SPRING_PROFILES_ACTIVE: 'e2e', E2E_PORT: String(PORT), SMARTGARDEN_MQTT_ENABLED: 'false' },
    reuseExistingServer: false,
    timeout: 240_000,
    stdout: 'ignore',
    stderr: 'pipe'
  }
});
