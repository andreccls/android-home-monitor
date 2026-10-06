# ADR 0003 — Injeção de dependência com Hilt

- **Status:** aceito
- **Data:** 2026-10-06

## Contexto

O grafo tem ~25 objetos (DAOs, repositórios, simulações, serviço de monitoramento, 7 ViewModels), e os testes instrumentados
precisam trocar o banco por um em memória e a configuração das simulações sem mexer no código de produção.

## Decisão

Usar **Hilt** (com KSP). Foi testado com a combinação mais nova (AGP 9.4, Kotlin 2.4, KSP 2.3, Hilt 2.60) e **não deu atrito**:
nenhum workaround foi necessário. O que decidiu:

- `@HiltViewModel` + `hiltViewModel()` dão ViewModels com injeção e `SavedStateHandle` (argumento `deviceId` da navegação) sem *factories* escritas à mão.
- Os módulos (`di/Modules.kt`) são o **único** lugar que escolhe as simulações (`PortsModule`). Trocar por integração real é trocar esse módulo.
- `@TestInstallIn` permite que o teste instrumentado substitua só `DatabaseModule` (banco em memória) e `SimulationModule`
  (simulação instantânea, sem falhas aleatórias), mantendo o resto do grafo **de produção**.

## Alternativas consideradas

- **DI manual** (um `AppContainer` + `ViewModelProvider.Factory`): zero processador de anotações e build mais rápido; funciona bem para
  apps pequenos. Teria sido a escolha se o Hilt desse problema com o AGP 9. Descartado porque o ganho de testes acima é real e o custo foi zero.
- **Koin:** *service locator* em tempo de execução, erros de grafo só aparecem rodando.

## Consequências

- (+) Teste de ponta a ponta com o grafo real; erros de ligação aparecem na compilação.
- (−) KSP/Hilt aumentam o tempo de build incremental (poucos segundos neste tamanho).
- (−) Os arquivos gerados (`Hilt_*`, `*_Factory`) ficam fora da cobertura (lista em docs/TESTING.md).
