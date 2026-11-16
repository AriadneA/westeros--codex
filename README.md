# Westeros Codex

Guia interativo **Offline-First** para Android sobre as Grandes Casas de Westeros, construído com **Kotlin**, **Jetpack Compose**, **Clean Architecture**, **Room (SSOT)**, **Retrofit + Moshi** e **Hilt**.

## Documentação do Projeto (`/docs`)

Todas as informações detalhadas de especificação do produto, requisitos funcionais/não funcionais, critérios de aceite, fluxo Offline-First e arquitetura técnica estão centralizadas na pasta **`docs/`**:

- **[01 — Especificação do Produto e Requisitos (`docs/01-SPECIFICATION.md`)](docs/01-SPECIFICATION.md)**  
  Visão do produto, personas, escopo do MVP, contratos das APIs (*An API of Ice and Fire* e *ThronesAPI*), requisitos funcionais (`RF-01` a `RF-09`), requisitos não funcionais (`RNF-01` a `RNF-08`) e cenários de aceite (BDD).
- **[02 — Arquitetura de Software (`docs/02-ARCHITECTURE.md`)](docs/02-ARCHITECTURE.md)**  
  Estrutura estrita das camadas `domain`, `data` e `presentation`, estratégia **Single Source of Truth (SSOT)** com Room, paginação sob demanda, mapeamento `DTO -> Entity -> Domain -> UiModel`, módulos Hilt e estratégia de testes unitários.

## Stack Tecnológica

- **Linguagem:** Kotlin + Coroutines & `Flow` / `StateFlow`
- **UI:** Jetpack Compose + Material Design 3 (tema escuro medieval da Cidadela)
- **Arquitetura:** Clean Architecture (`domain` puro, `data`, `presentation`) + MVVM + UDF
- **Persistência Local (SSOT):** Room Database (via KSP)
- **Rede & Serialização:** Retrofit + OkHttp + Moshi Codegen (KSP)
- **Injeção de Dependências:** Hilt
- **Testes Unitários:** JUnit 4, MockK, `kotlinx-coroutines-test` e Robolectric

## Executando os Testes Unitários

```bash
gradle :app:testDebugUnitTest
```
