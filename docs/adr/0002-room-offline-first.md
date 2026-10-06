# ADR 0002 — Room como fonte da verdade (offline-first)

- **Status:** aceito
- **Data:** 2026-10-06

## Contexto

O app precisa abrir instantaneamente com o último estado conhecido da casa (dispositivos, estado dos portões, histórico do
interfone, alertas) e continuar útil se a rede ou um dispositivo falhar.

## Decisão

- **Room é a única fonte da verdade da UI.** Os repositórios expõem `Flow` sobre as consultas do Room; a tela nunca lê direto de um dispositivo.
- **Quem escreve o estado do mundo é um só:** o `MonitoringService` escuta as portas (`GateDriver`, `IntercomDriver`, `DeviceProbe`),
  grava o resultado no Room e dispara alertas via `AlertRules`. Um comando (abrir portão) **não** altera o banco: ele vai à porta e
  a *confirmação* volta como relatório do portão, que o serviço grava. Assim a UI mostra o que o portão **disse**, não o que o usuário **pediu**.
- **Dados de demonstração** entram uma vez, no `onCreate` do banco (`DemoSeed`), para a instalação nova não ser uma tela vazia.
  Portões vêm da *descoberta* da porta, não do seed.
- Esquema exportado em `app/schemas/` (versão 1). Não há migrações ainda (YAGNI): ao evoluir o esquema, a primeira
  migração entra junto com a mudança.

## Alternativas consideradas

- **Estado em memória nos ViewModels, lendo direto das portas:** perde o histórico ao fechar o app e acopla a UI ao hardware.
- **DataStore/SharedPreferences:** não serve para listas relacionais com consultas e ordenação.
- **Atualização otimista (mostrar "aberto" ao tocar):** engana o usuário se o portão falhar; descartada para um portão físico.

## Consequências

- (+) A UI é reativa e consistente por construção; testes de repositório rodam com Room real em memória.
- (+) Alerta de "portão aberto há muito tempo" é uma consulta ao banco, sobrevive a reiniciar o app.
- (−) Há um instante entre "confirmar" e o estado mudar (latência do dispositivo); a UI mostra o progresso (indicador no botão) em vez de esconder.
- (−) O `MonitoringService` só roda enquanto o processo está vivo; monitorar em segundo plano de verdade pediria WorkManager/serviço em primeiro plano (ver Próximos passos).
