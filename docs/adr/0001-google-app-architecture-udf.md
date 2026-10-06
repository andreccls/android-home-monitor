# ADR 0001 — Arquitetura de referência do Google, com fluxo de dados unidirecional (UDF)

- **Status:** aceito
- **Data:** 2026-10-06

## Contexto

Um app Android de monitoramento tem estado que muda por vários motivos ao mesmo tempo: o usuário toca em algo, um portão
termina de abrir sozinho, uma chamada de interfone chega. Sem uma regra clara de quem escreve o quê, a tela e o banco divergem.

## Decisão

Seguir o [Guide to app architecture](https://developer.android.com/topic/architecture) do Google:

- **Três camadas:** `ui` (Compose + ViewModels), `domain` e `data`. A camada `domain` existe aqui **porque agrega valor**
  (máquina de estados do portão, regras de alerta, validação do cadastro, contratos dos repositórios e das portas) — o guia a trata como opcional.
- **UDF:** o estado desce (`ViewModel` → `StateFlow<UiState>` → tela), os eventos sobem (tela → métodos do `ViewModel`).
  As telas são funções **sem estado** (`XScreen(state, callbacks)`); só o `XRoute` conhece o `ViewModel`.
- **Eventos de uma vez só** (mensagens, "terminei, pode navegar") viram **estado** (`message`, `isDone`) e são consumidos
  (`onMessageShown`), como o guia recomenda, em vez de canais/`SharedFlow` que perdem eventos na rotação.
- **Regra de dependência:** `ui → domain ← data`. `domain` não importa nada de Android nem de `data`/`ui`; `data` não conhece `ui`;
  `ui` só fala com `domain` (repositórios e portas são interfaces lá). É verificada por teste (**Konsist**, ver ADR 0005).
- **Módulo único** com pacotes separados (KISS): multi-módulo traria ganho de tempo de build e isolamento que um app deste tamanho
  não precisa; o Konsist dá o mesmo efeito de "falha ao violar" sem o custo de 4 `build.gradle`.

## Alternativas consideradas

- **MVI com reducer/intents:** mais cerimônia; o UDF com ViewModel + `StateFlow` já dá previsibilidade.
- **Multi-módulo (`:domain`, `:data`, `:app`):** o compilador imporia a regra, mas o custo (convention plugins, 3 módulos de Hilt/Room) não se paga aqui.
  Fica como próximo passo se o app crescer.
- **Sem camada `domain`:** as regras iriam parar nos ViewModels ou nos repositórios, misturadas com Android/Room.

## Consequências

- (+) Cada regra de negócio é Kotlin puro e testável em JVM sem Robolectric.
- (+) Trocar a fonte de dados (simulador → hardware real) não toca `ui` nem `domain`.
- (−) Um pouco de código de mapeamento (`Entity ↔ domain`) e de interfaces; aceito, é o preço da separação.
