# ADR 0004 — Dispositivos simulados atrás de portas (ports)

- **Status:** aceito
- **Data:** 2026-10-06

## Contexto

Não há câmeras, Alexas, portões ou interfones reais neste projeto. Mesmo assim o app precisa se comportar como se houvesse:
estados que mudam com o tempo, latência, falhas ocasionais, ações que demoram. E precisa ser honesto: **nada aqui fala com hardware real.**

## Decisão

- Tudo que tocaria hardware fica atrás de **interfaces em `domain/port`**: `GateDriver`, `IntercomDriver`, `DeviceProbe`, `CameraStream`.
  Cada uma descreve o *contrato* (o que pode falhar, que exceção sobe), não um protocolo.
- Há **uma implementação por porta**, em `data/simulation`, com a classe `SimulationConfig` concentrando os parâmetros
  (latência, taxa de falha, tempo de curso do portão, intervalo entre chamadas…). `Default` parece vivo no celular; `Instant` é
  determinístico para testes. Os simuladores recebem `Clock`/`Random`/`CoroutineScope`, então os testes usam tempo virtual.
- Convenção para demos/testes: um endereço contendo `offline` nunca responde.
- O ponto único de escolha é `PortsModule` (Hilt). Nenhuma outra classe de `ui`/`domain`/`data` importa `data.simulation` — o teste de arquitetura garante.
- A "transmissão" da câmera é um `Flow<CameraFrame>` de contadores; a UI **desenha** cada quadro (Canvas). Não há vídeo.

## Alternativas consideradas

- **Mocks/fakes só nos testes, e no app um "TODO":** deixaria o app sem comportamento para demonstrar e sem exercitar a UI de estados.
- **Backend simulado (servidor HTTP falso):** realista demais para o que se quer mostrar; adiciona um processo a subir.
- **Implementar RTSP/ONVIF/Alexa agora:** YAGNI e impossível de validar sem hardware. Está em "Próximos passos" do README.

## Consequências

- (+) Trocar por integração real é implementar 4 interfaces e mudar um módulo; nada acima muda.
- (+) Os testes dos casos difíceis (falha, timeout de chamada, queda de dispositivo) são determinísticos.
- (−) O simulador é uma *hipótese* de como o hardware se comporta; protocolos reais terão falhas que ele não imita (ex.: respostas parciais, reautenticação).
