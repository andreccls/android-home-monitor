# Testes

Números medidos em 2026-10-06, Mac Apple Silicon, JDK 25 (JBR do Android Studio), emulador `Medium_Phone` (API 37).

```bash
make test          # 66 testes unitários (JVM, sem emulador)
make coverage      # testes + Kover: relatório, gate de 80 % e tabela por camada
make lint          # ktlint + Android Lint (avisos = erro)
make instrumented  # 7 testes de UI no emulador/dispositivo conectado
```

## Pirâmide

| Nível | Onde | O que verifica | Como |
|---|---|---|---|
| Regras de domínio | `domain/DomainTest.kt` (13) | máquina de estados do portão, validação do cadastro, geração de alertas | Kotlin puro, sem Android |
| Simuladores | `data/simulation/SimulationTest.kt` (12) | portão percorre OPENING→OPEN, reversão, falha de comando, visitantes/quedas autônomos, chamada perdida, quedas de dispositivo, stream sem sinal | tempo virtual (`runTest`) + Turbine |
| Room + repositórios | `data/repository/*Test.kt` (7) | CRUD, comandos validados pela máquina de estados, atender chamada, ordenação de alertas, seed de demonstração | **Room real em memória via Robolectric** |
| Monitoramento | `data/monitoring/MonitoringServiceTest.kt` (8) | portas → Room, alerta único por abertura, chamada vira perdida, dispositivo cai/volta, `start()` ponta a ponta | Room real + fakes das portas |
| ViewModels | `ui/ViewModelsTest.kt` (20) | estado combinado, filtros, formulário (erros ao vivo, criar/editar/excluir), confirmação antes de enviar comando, atender + abrir portão, câmera sem sinal/retry | fakes escritos à mão, Turbine |
| Arquitetura | `architecture/ArchitectureTest.kt` (6) | `domain` sem Android, `data` sem `ui`, `ui` sem `data`, ViewModels sem Compose/Room, só `di` vê os simuladores | Konsist sobre o código-fonte |
| UI (instrumentado) | `androidTest/AppFlowTest.kt` (7) | cadastrar câmera, cadastrar Alexa, erros do formulário, portão com confirmação (cancelar e confirmar), atender interfone e abrir portão, alertas, alvo de toque ≥ 48 dp | Compose test rule + **grafo Hilt de produção** (só banco e simulação trocados) |

Total: **66 unitários + 7 instrumentados = 73 testes, todos verdes.**

## Cobertura (Kover, linhas dos testes unitários)

| Camada | Linhas | Branches |
|---|---|---|
| `data` | 282/282 = **100 %** | 80/84 = 95,2 % |
| `domain` | 88/88 = **100 %** | 26/27 = 96,3 % |
| `ui` (ViewModels e estados) | 174/174 = **100 %** | 53/58 = 91,4 % |
| **Total medido** | 544/544 = **100 %** | 159/169 = 94,1 % |

**Gate:** `koverVerify` falha o build abaixo de **80 % de linhas** sobre o que está medido. 100 % de linhas não prova ausência
de bugs; há 10 ramos de 169 não cobertos. Um deles é conhecido: o `else throw` do stream de câmera para erro inesperado
(uma exceção dentro de `stateIn` vai para o `viewModelScope`, difícil de testar de forma útil). Os demais não foram catalogados um a um
(abra o relatório HTML para vê-los).

### O que fica FORA da medição, e por quê

| Exclusão | Motivo |
|---|---|
| `*Screen*` (telas Compose), `ui.components`, `ui.theme`, `ui.navigation` | UI declarativa; é validada pelos testes **instrumentados** e por screenshots. O Kover não mede o que roda no emulador |
| `di.*` (módulos Hilt), `MainActivity`, `HomeMonitorApp` | "cola" de configuração; coberta indiretamente pelo `AppFlowTest`, que sobe o grafo real |
| `*_Impl*`, `*_Factory*`, `Hilt_*`, `*_HiltModules*`, `hilt_aggregated_deps`, `dagger.hilt.*` | código gerado por Room/Hilt |
| `BuildConfig`, `R`, `ComposableSingletons` | gerados |

A lista está em `app/build.gradle.kts` (bloco `kover`). Relatório HTML: `app/build/reports/kover/html/index.html`.

## Lint

- **ktlint 1.8.0** (CLI via `JavaExec`, `.editorconfig` com `ktlint_official`): sem violações.
- **Android Lint**: `warningsAsErrors = true`; `lintDebug` limpo. Desligados com justificativa em `app/lint.xml`: verificações de "versão mais nova" (dependem de rede; versões ficam no catálogo) e `Typos` (só conhece inglês e acusa português).
- Kotlin com `allWarningsAsErrors`.

## Por que Room em Robolectric e não instrumentado

Roda em segundos e no CI, usa SQLite real e entra na cobertura. No JDK 25 o Robolectric precisa de `--add-exports/--add-opens`
(configurado em `app/build.gradle.kts`). Robolectric roda em **API 36** (`robolectric.properties`); o emulador é API 37 — a diferença só
afeta o ambiente simulado do SQLite, não o código do app.

## Por que os instrumentados não estão no CI

Precisam de emulador: em runners hospedados exigem aceleração de hardware e boot de vários minutos, o que deixa a pipeline lenta e instável.
O CI roda lint, unitários + gate de cobertura e `assembleDebug`. Os instrumentados rodam com `make instrumented` (emulador ligado).

## Acessibilidade (verificação)

- Ícones decorativos têm `contentDescription = null` e o texto ao lado; ações têm descrição própria (`Abrir Portão da garagem`, `Ver câmera X`, `Adicionar dispositivo`).
- Cartões da lista de dispositivos descrevem nome, tipo, cômodo e status numa frase só (TalkBack).
- Estado nunca é só cor: toda *chip* traz o texto (Aberto, Offline, Perdida).
- Alvo de toque ≥ 48 dp verificado por teste (barra inferior e botão de adicionar); botões Material usam o mínimo de 48 dp por padrão.
- Contraste: paleta Material 3 com pares `container/onContainer`; **não** foi medido com ferramenta automática (ver Limitações do README). **TalkBack não foi exercitado em dispositivo** — só a semântica foi revisada.
