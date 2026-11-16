# 01 — Especificação do Produto e Requisitos (SDD)

**Projeto:** Westeros Codex  
**Versão da Especificação:** 1.0.0  
**Metodologia:** Spec-Driven Development (SDD)  
**Plataforma Alvo:** Android (API 24+ / Target SDK 36)  
**Idioma da Interface:** Português (PT-BR)  

---

## 1. Visão Geral do Produto

O **Westeros Codex** é um aplicativo Android nativo, desenvolvido com arquitetura **Offline-First**, concebido como um compêndio heráldico e histórico interativo sobre o universo de *As Crônicas de Gelo e Fogo* / *Game of Thrones*.

O aplicativo permite que o usuário explore as **Grandes Casas e Casas Nobres de Westeros**, filtre por regiões dos Sete Reinos, inspecione detalhes heráldicos (brasão, lema, sedes, títulos, armas ancestrais e retratos de personagens associados), faça o gesto simbólico de **"Jurar Lealdade"** (favoritar casas localmente) e redija **"Anotações do Meistre"** (notas pessoais persistidas offline e vinculadas a cada casa).

### 1.1 Proposta de Valor

- **Disponibilidade Contínua (Offline-First):** Todo conteúdo consultado permanece disponível sem conexão à internet, com o banco de dados local (**Room**) atuando como Fonte Única da Verdade (*Single Source of Truth — SSOT*).
- **Preservação Autoral:** Juramentos de lealdade e anotações pessoais pertencem exclusivamente ao usuário e nunca são sobrescritos por sincronizações remotas.
- **Identidade Visual Temática:** Interface em **tema escuro medieval editorial** (inspirado nos arquivos da Cidadela e em aço valiriano), construída em **Jetpack Compose + Material Design 3**.

### 1.2 Personas

| Persona | Perfil | Necessidade Principal |
| :--- | :--- | :--- |
| **Leitor / Espectador** | Fã dos livros ou da série que acompanha capítulos e episódios em trânsito. | Consultar rapidamente brasões, lemas, sedes e membros famosos mesmo sem sinal de rede. |
| **Colecionador de Lore** | Entusiasta de *worldbuilding* que compara linhagens, títulos e armas ancestrais. | Filtrar casas por região, favoritar casas juradas e registrar teorias ou resumos nas Anotações do Meistre. |

---

## 2. Escopo do Projeto

### 2.1 Escopo do MVP (Versão 1.0)

1. **Catálogo Paginado de Casas:** Listagem das casas consumidas da *An API of Ice and Fire*, com sincronização inicial da primeira página e carregamento sob demanda (*pagination on demand*) ao rolar até o fim da lista ou via *pull-to-refresh*.
2. **Busca e Filtro Regional:** Busca textual local (nome e lema) e filtros por região (ex.: *The North*, *The Westerlands*, *The Reach*, *Dorne*, *The Stormlands*, *The Riverlands*, *The Vale*, *Iron Islands*, *Crownlands*).
3. **Visão de Casas Juradas:** Filtro ou aba dedicada às casas às quais o usuário jurou lealdade localmente.
4. **Detalhes da Casa:** Exibição completa de nome, região, descrição do brasão (*Coat of Arms*), lema (*Words*), sedes (*Seats*), títulos (*Titles*), fundação, armas ancestrais (*Ancestral Weapons*) e galeria de retratos de personagens associados.
5. **Enriquecimento com Retratos (ThronesAPI no MVP):** Consumo do catálogo de personagens da *ThronesAPI* para associar retratos e títulos de personagens conhecidos às respectivas casas na tela de detalhes.
6. **Jurar Lealdade:** Ação persistente offline para marcar/desmarcar uma casa como juramentada (favorita), acessível tanto na listagem quanto no detalhe.
7. **Anotações do Meistre:** Editor de notas pessoais por casa (criar, editar, visualizar e excluir anotações) armazenadas integralmente offline no Room.
8. **Tratamento Explícito de Estados de UI:** Suporte explícito aos 4 estados (`Loading`, `Success`, `Empty`, `Error`) sem falhas silenciosas ou telas quebradas.

### 2.2 Fora do Escopo do MVP (Extensões Futuras Pós-MVP)

- Sincronização em nuvem / autenticação de conta entre múltiplos dispositivos.
- Árvore genealógica gráfica interativa de *cadet branches* e *overlords* com navegação recursiva profunda.
- Consumo detalhado individual de centenas de URLs `/api/characters/{id}` na *An API of Ice and Fire* (evitado no MVP para prevenir rajadas de requisições N+1).
- Tradução automática dinâmica dos textos originais em inglês retornados pelas APIs públicas (mantendo rótulos e UX da interface em Português PT-BR e termos canônicos/lore preservados).

---

## 3. Fontes de Dados Externas (Contratos REST)

### 3.1 API Primária — An API of Ice and Fire

- **Base URL:** `https://anapioficeandfire.com/api/`
- **Endpoint Principal:** `GET houses?page={page}&pageSize={pageSize}`
- **Comportamento da API:** Retorna lista paginada de casas (~444 registros totais; `pageSize` máximo de 50). Campos ausentes frequentemente chegam como string vazia `""` ou array com string vazia `[""]`.
- **Campos Relevantes no Contrato JSON:**
  - `url` (`String`): URI única do recurso (ex.: `https://anapioficeandfire.com/api/houses/362`), usada para extrair o `houseId` canônico (`"362"`).
  - `name` (`String`): Nome oficial da casa (ex.: `"House Stark of Winterfell"`).
  - `region` (`String`): Região geográfica em Westeros (ex.: `"The North"`).
  - `coatOfArms` (`String`): Descrição textual heráldica do brasão.
  - `words` (`String`): Lema oficial da casa (ex.: `"Winter is Coming"`).
  - `titles` (`List<String>`): Títulos nobiliárquicos detidos pela casa.
  - `seats` (`List<String>`): Fortalezas ou sedes tradicionais.
  - `founded` (`String`), `diedOut` (`String`): Marcos históricos quando disponíveis.
  - `ancestralWeapons` (`List<String>`): Armas lendárias (ex.: `["Ice"]`).
  - `overlord` (`String`), `cadetBranches` (`List<String>`), `swornMembers` (`List<String>`): Referências relacionais.

### 3.2 API Complementar — ThronesAPI

- **Base URL:** `https://thronesapi.com/api/v2/`
- **Endpoint Principal:** `GET Characters`
- **Comportamento da API:** Retorna um array JSON compacto (~53 personagens principais) com URLs diretas de imagens.
- **Campos Relevantes no Contrato JSON:**
  - `id` (`Int`): Identificador numérico do personagem.
  - `firstName` (`String`), `lastName` (`String`), `fullName` (`String`): Identificação nominal.
  - `title` (`String`): Título do personagem (ex.: `"Mother of Dragons"`).
  - `family` (`String`): Nome da casa/família (ex.: `"House Targaryen"`, `"Stark"`).
  - `imageUrl` (`String`): URL absoluta da imagem para renderização e cache via Coil.

---

## 4. Requisitos Funcionais (RF)

| ID | Requisito | Descrição Detalhada | Prioridade |
| :--- | :--- | :--- | :--- |
| **RF-01** | **Listagem Inicial Offline-First** | Ao iniciar o app, a UI deve observar continuamente o banco local Room. Se o banco estiver vazio, dispara a sincronização da página 1 (`pageSize = 25`) da *An API of Ice and Fire* e exibe estado `Loading`; havendo cache prévio, exibe imediatamente os dados locais (`Success`) enquanto revalida em segundo plano. | Alta (MVP) |
| **RF-02** | **Paginação Sob Demanda** | Quando o usuário rolar a lista de casas próximo ao final dos itens carregados (sem filtro restritivo local ativo), o app deve requisitar a próxima página remota, persistir no Room e atualizar a lista reativamente. Falhas na paginação exibem aviso discreto sem apagar os itens já visíveis. | Alta (MVP) |
| **RF-03** | **Filtro por Região e Busca Local** | O usuário deve poder filtrar as casas por região (chips horizontais com opção "Todas") e buscar por nome ou lema em tempo real diretamente sobre o banco local (`Flow`), sem depender de rede. | Alta (MVP) |
| **RF-04** | **Visualização de Detalhes da Casa** | Ao selecionar uma casa, o app navega para a tela de detalhes exibindo: nome, região, lema destacado, descrição do brasão, sedes, títulos, armas ancestrais (com tratamento visual adequado quando vazios, ex.: *"Nenhuma registrada"*), status de lealdade e notas do Meistre. | Alta (MVP) |
| **RF-05** | **Retratos de Personagens (ThronesAPI)** | O app deve sincronizar os personagens da *ThronesAPI* no cache local e exibi-los na tela de detalhes da casa correspondente através de normalização do campo `family` (ex.: `"Stark"` / `"House Stark"` associado a `"House Stark of Winterfell"`), com placeholder temático quando offline ou sem imagem. | Média (MVP) |
| **RF-06** | **Jurar Lealdade (Favoritar Localmente)** | O usuário pode alternar o estado de "Juramento de Lealdade" de qualquer casa na listagem ou nos detalhes. A alteração deve persistir imediatamente no Room e sobreviver a futuras sincronizações da API. | Alta (MVP) |
| **RF-07** | **Filtro / Aba de Casas Juradas** | O usuário deve possuir acesso dedicado (aba na `NavigationBar` ou alternância principal) para visualizar apenas as casas às quais jurou lealdade, além de ver suas anotações associadas. Caso não haja casas juradas, exibir estado `Empty` orientativo. | Alta (MVP) |
| **RF-08** | **Anotações do Meistre (CRUD Local)** | Na tela de detalhes de uma casa, o usuário deve poder adicionar, editar e excluir anotações pessoais em texto livre vinculadas ao `houseId`, com registro de data/hora da última atualização, 100% persistidas no Room. | Alta (MVP) |
| **RF-09** | **Atualização Manual (*Pull-to-Refresh*)** | O usuário pode acionar a atualização manual na lista principal para ressincronizar a primeira página de casas e o catálogo da ThronesAPI. Se não houver rede, o cache local permanece intacto e um feedback amigável é exibido. | Média (MVP) |

---

## 5. Requisitos Não Funcionais (RNF)

| ID | Categoria | Requisito | Critério de Verificação |
| :--- | :--- | :--- | :--- |
| **RNF-01** | **Arquitetura** | Separação estrita em Clean Architecture (`domain`, `data`, `presentation`). | A camada `domain` não pode importar classes de `android.*`, `androidx.*`, `retrofit2.*` ou `androidx.room.*`. |
| **RNF-02** | **Offline-First & SSOT** | O Room é a única fonte da verdade para toda exibição de dados na UI. | ViewModels nunca consomem Retrofit/DTOs diretamente; toda leitura UI deriva de `Flow` do Repository alimentado pelo DAO. |
| **RNF-03** | **Injeção de Dependências** | Padronização exclusiva com **Hilt**. | Todos os grafos de dependência de rede, banco, repositórios e ViewModels providos via módulos Hilt (`@Module`, `@InstallIn`). |
| **RNF-04** | **Concorrência** | Uso exclusivo de Kotlin Coroutines, `Flow` e `StateFlow`. | Nenhuma chamada de rede ou escrita em disco na `Main` thread; `Dispatchers.IO` encapsulado na camada `data`. |
| **RNF-05** | **Performance de UI** | Otimização de recomposições no Jetpack Compose. | Listas em `LazyColumn` usam `key` únicas (`house.id`); modelos de estado de UI são anotados com `@Immutable` ou `@Stable`; coleta lifecycle-aware com `collectAsStateWithLifecycle()`. |
| **RNF-06** | **Resiliência de Rede** | Degradação graciosa em falhas HTTP, timeout ou ausência de internet. | Exceções (`IOException`, `HttpException`) capturadas no `RepositoryImpl`, convertidas em erros de domínio sem crash (*zero unhandled exceptions*). |
| **RNF-07** | **Design & Acessibilidade** | Tema escuro medieval em Material 3, `enableEdgeToEdge()`, targets >= `48.dp` e `contentDescription`. | Uso exclusivo de tokens de `MaterialTheme.colorScheme` (sem hex hardcoded nos Composables), suporte a insets (`WindowInsets`) e navegação segura. |
| **RNF-08** | **Qualidade & Testes** | Cobertura de testes unitários para regras, dados e apresentação. | Testes automatizados com **JUnit4**, **MockK** e **kotlinx-coroutines-test** para todos os `UseCases`, `RepositoryImpl` e `ViewModels`. |

---

## 6. Critérios de Aceite (Cenários BDD)

### Cenário 1: Primeiro Acesso Online e Persistência SSOT
- **Dado** que o banco local Room está vazio e o dispositivo possui acesso à internet,
- **Quando** o usuário abre o *Westeros Codex*,
- **Então** a tela exibe o estado `Loading`, sincroniza a primeira página de casas e os personagens da ThronesAPI em background, persiste no Room e transiciona automaticamente para o estado `Success` renderizando as casas.

### Cenário 2: Acesso Subsequente Totalmente Offline
- **Dado** que o usuário já sincronizou pelo menos uma página de casas e encontra-se em Modo Avião,
- **Quando** o usuário abre o aplicativo, busca por `"Stark"`, jura lealdade à casa e salva uma Anotação do Meistre,
- **Então** o aplicativo exibe os dados do cache instantaneamente sem erro bloqueante, filtra corretamente, grava o juramento e salva a anotação no Room sem necessidade de conexão.

### Cenário 3: Preservação de Lealdade e Notas Após Re-sincronização
- **Dado** que o usuário jurou lealdade à *"House Targaryen of King's Landing"* e registrou uma nota do Meistre vinculada a ela,
- **Quando** uma nova sincronização remota da página correspondente ocorre com sucesso,
- **Então** os metadados heráldicos da casa são atualizados no Room sem apagar ou resetar o status de lealdade nem as anotações existentes.

### Cenário 4: Filtro Sem Resultados (Estado Empty)
- **Dado** que existem casas em cache,
- **Quando** o usuário digita `"ValyriaInexistente"` na busca ou abre a aba de Casas Juradas sem nenhuma casa favoritada,
- **Então** a UI exibe explicitamente o estado `Empty` com mensagem clara em Português e botão/ação para limpar filtros ou retornar à exploração.

### Cenário 5: Falha de Rede Sem Cache Prévio (Estado Error)
- **Dado** que o banco Room está vazio e o dispositivo está sem conexão,
- **Quando** o usuário abre o aplicativo,
- **Então** a UI exibe o estado `Error` com mensagem compreensível em Português e botão *"Tentar novamente"* que reaciona a sincronização ao ser pressionado.

---

## 7. Convenções de Interface e Identidade Visual

- **Idioma:** Todos os rótulos, botões, mensagens de estado (`Loading`, `Empty`, `Error`), filtros e placeholders devem ser redigidos em **Português (PT-BR)**. Campos históricos literais provenientes das APIs externas (ex.: nome da casa, descrição original do brasão e lema) devem ser exibidos com formatação limpa, substituindo strings vazias por textos orientativos em português (ex.: *"Lema não registrado nos arquivos"*).
- **Tema Escuro Medieval (Cidadela):**
  - Superfícies em tons de obsidiana (`#0F1115`) e ardósia (`#171A21`).
  - Acentos em ouro envelhecido (`#C9A24D`) para destaques, selos e lemas.
  - Realce em carmesim real (`#8C2D2D`) para ações/badges de juramento de lealdade ativo.
  - Tipografia display serifada para títulos nobiliárquicos pareada com tipografia legível sem serifa para corpo de texto e anotações.
