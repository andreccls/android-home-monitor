# ADR 0005 — Estratégia de testes

- **Status:** aceito
- **Data:** 2026-10-06

## Contexto

Queremos confiança nas regras (portão, alertas, validação), no armazenamento (Room), nos ViewModels e nos fluxos de tela,
sem uma suíte lenta nem testes que verificam o framework em vez do código.

## Decisão

- **JUnit 4**, não JUnit 5: Robolectric e o runner do AndroidX dependem de JUnit 4; usar um só framework evita dois mundos.
- **Fakes escritos à mão** (`support/Fakes.kt`) em vez de MockK: testes por estado, legíveis, e os mesmos fakes servem de documentação dos contratos.
  **Turbine** para `Flow`, **kotlinx-coroutines-test** (tempo virtual) para simuladores e ViewModels.
- **Room testado em JVM com Robolectric** (banco em memória, SQLite real) em vez de instrumentado: roda em segundos, no CI, e entra na cobertura.
  Custo: Robolectric reflete em internals do JDK; no JDK 25 foi preciso `--add-exports/--add-opens` (em `app/build.gradle.kts`).
- **Testes de UI Compose instrumentados** no emulador, com o **grafo Hilt real** (`@TestInstallIn` troca só banco e configuração das simulações).
  Cobrem os fluxos principais de ponta a ponta. Não entram na cobertura Kover nem no CI (ver abaixo).
- **Teste de arquitetura (Konsist)** roda como teste unitário: a regra de dependência quebra o build.
- **Cobertura com Kover, gate de 80 % de linhas** em `domain`, `data` e ViewModels. Exclusões justificadas e listadas em docs/TESTING.md.
- **Lint:** Android Lint + ktlint, com avisos tratados como erro (`-Werror` no Kotlin, `warningsAsErrors` no Lint).
- **Instrumentados fora do CI:** exigem emulador (aceleração + boot de minutos nos runners hospedados) — lento e instável. Rodam com `make instrumented`.

## Alternativas consideradas

- **Compose em JVM com Robolectric:** mais rápido que o emulador, mas diverge de um dispositivo real (renderização, toque, foco) — justamente o que queremos ver.
- **MockK:** poderoso, mas induz testes acoplados à implementação (quem chamou quem).
- **Testes de snapshot (Paparazzi/Roborazzi):** valiosos, mas outra dependência; o ganho aqui seria só visual.

## Consequências

- (+) Pirâmide saudável: muitos testes JVM rápidos, poucos de UI no emulador.
- (−) A camada de telas (`*Screen`) não tem número de cobertura: é validada pelos instrumentados e por screenshots, não por uma métrica.
