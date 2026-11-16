# 02 — Arquitetura de Software e Estratégia Offline-First

**Projeto:** Westeros Codex  
**Versão da Arquitetura:** 1.0.0  
**Padrão Arquitetural:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)  
**Fonte Única da Verdade (SSOT):** Room Database  
**Injeção de Dependências:** Hilt  

---

## 1. Princípios Arquiteturais

A arquitetura do **Westeros Codex** segue estritamente a **Clean Architecture** combinada ao padrão **MVVM** e à estratégia **Offline-First**, garantindo baixo acoplamento, alta testabilidade unitária e resiliência completa contra instabilidades de rede.

### Regras Invioláveis de Dependência

```
┌───────────────────────────┐
│       PRESENTATION        │  (Jetpack Compose, ViewModel, UiState, Hilt)
└─────────────┬─────────────┘
              │ depende de
              ▼
┌───────────────────────────┐
│          DOMAIN           │  (Kotlin Puro: Models, UseCases, Repository Interfaces)
└─────────────▲─────────────┘
              │ implementa
┌─────────────┴─────────────┐
│           DATA            │  (Room, Retrofit, Moshi, Mappers, RepositoryImpl)
└───────────────────────────┘
```

1. **Regra da Dependência:** `Presentation` e `Data` dependem de `Domain`. A camada `Domain` **nunca** depende de `Data`, `Presentation` ou do SDK Android (`android.*`, `androidx.*`).
2. **SSOT (Single Source of Truth):** A UI nunca consome respostas de rede diretamente. Toda leitura exibida na tela provém de um `Flow` emitido pelo **Room**. O **Repository** é o único responsável por sincronizar a rede em segundo plano e gravar no Room.
3. **Unidirectional Data Flow (UDF):** A UI dispara intenções (*Intents / Events*) para o `ViewModel`; o `ViewModel` aciona `UseCases` e expõe um único `StateFlow<UiState>` imutável observado com `collectAsStateWithLifecycle()`.

---

## 2. Estrutura de Camadas e Pacotes

```text
com.example/
├── di/                                # Módulos Hilt (NetworkModule, DatabaseModule, RepositoryModule)
├── domain/                            # [KOTLIN PURO] Sem imports Android/Room/Retrofit
│   ├── model/
│   │   ├── House.kt
│   │   ├── HouseDetail.kt
│   │   ├── Character.kt
│   │   ├── MaesterNote.kt
│   │   └── SyncResult.kt
│   ├── repository/
│   │   └── HouseRepository.kt
│   └── usecase/
│       ├── ObserveHousesUseCase.kt
│       ├── ObserveHouseDetailUseCase.kt
│       ├── SyncHousesPageUseCase.kt
│       ├── SyncCharactersUseCase.kt
│       ├── ToggleAllegianceUseCase.kt
│       ├── SaveMaesterNoteUseCase.kt
│       └── DeleteMaesterNoteUseCase.kt
├── data/                              # Implementações de infraestrutura e SSOT
│   ├── remote/
│   │   ├── api/
│   │   │   ├── IceAndFireApiService.kt
│   │   │   └── ThronesApiService.kt
│   │   └── dto/
│   │       ├── HouseDto.kt
│   │       └── ThronesCharacterDto.kt
│   ├── local/
│   │   ├── WesterosDatabase.kt
│   │   ├── dao/
│   │   │   ├── HouseDao.kt
│   │   │   ├── CharacterDao.kt
│   │   │   ├── MaesterNoteDao.kt
│   │   │   └── SyncMetadataDao.kt
│   │   ├── converter/
│   │   │   └── WesterosTypeConverters.kt
│   │   └── entity/
│   │       ├── HouseEntity.kt
│   │       ├── HouseUserStateEntity.kt
│   │       ├── CharacterEntity.kt
│   │       ├── MaesterNoteEntity.kt
│   │       ├── SyncMetadataEntity.kt
│   │       └── HouseWithUserState.kt
│   ├── mapper/
│   │   ├── HouseMappers.kt
│   │   ├── CharacterMappers.kt
│   │   └── MaesterNoteMappers.kt
│   └── repository/
│       └── HouseRepositoryImpl.kt
└── presentation/                      # Jetpack Compose + Material 3 + ViewModels
    ├── navigation/
    │   ├── WesterosDestinations.kt    # Rotas @Serializable (Navigation Compose)
    │   └── WesterosNavHost.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    ├── houses/
    │   ├── HousesUiState.kt           # Loading, Success, Empty, Error + @Immutable models
    │   ├── HousesViewModel.kt
    │   └── HousesScreen.kt
    └── detail/
        ├── HouseDetailUiState.kt
        ├── HouseDetailViewModel.kt
        └── HouseDetailScreen.kt
```

---

## 3. Detalhamento das Camadas

### 3.1 Camada `domain` (Kotlin Puro)

Responsável por encapsular as regras de negócio do domínio de Westeros e os contratos de acesso a dados.

- **Modelos de Domínio (`domain.model`):**
  - `House`: `id`, `name`, `region`, `coatOfArms`, `words`, `seats`, `titles`, `ancestralWeapons`, `isSworn` (lealdade), `notesCount`.
  - `HouseDetail`: Estende informações completas com `founded`, `diedOut`, `overlordId`, `characters: List<Character>` e `notes: List<MaesterNote>`.
  - `Character`: `id`, `firstName`, `lastName`, `fullName`, `title`, `family`, `imageUrl`.
  - `MaesterNote`: `id`, `houseId`, `content`, `updatedAtMillis`.
- **Contrato de Repositório (`HouseRepository`):**
  - `observeHouses(query: String, region: String?, onlySworn: Boolean): Flow<List<House>>`
  - `observeHouseDetail(houseId: String): Flow<HouseDetail?>`
  - `observeAvailableRegions(): Flow<List<String>>`
  - `syncHousesPage(page: Int, pageSize: Int): Result<Boolean>` (retorna se há próxima página disponível)
  - `syncInitialDataIfEmpty(): Result<Unit>`
  - `syncCharacters(): Result<Unit>`
  - `toggleAllegiance(houseId: String): Result<Unit>`
  - `saveMaesterNote(houseId: String, noteId: Long?, content: String): Result<Unit>`
  - `deleteMaesterNote(noteId: Long): Result<Unit>`
- **UseCases:** Cada ação relevante possui um caso de uso focado com operador `invoke`, validando invariantes de negócio (ex.: impedir criação de anotação do Meistre vazia após `trim()`, padronizar extração de regiões válidas).

### 3.2 Camada `data` (Retrofit + Room + Mappers)

Responsável pela comunicação HTTP, serialização JSON via Moshi, persistência local no SQLite via Room e orquestração Offline-First.

- **Redes (`remote`):**
  - `IceAndFireApiService`: Configurado com Retrofit para `https://anapioficeandfire.com/api/`, expondo `@GET("houses") suspend fun getHouses(@Query("page") page: Int, @Query("pageSize") pageSize: Int): List<HouseDto>`.
  - `ThronesApiService`: Configurado com Retrofit separado (ou `@Url`/instância nomeada Hilt `@Named("ThronesRetrofit")`) para `https://thronesapi.com/api/v2/`, expondo `@GET("Characters") suspend fun getCharacters(): List<ThronesCharacterDto>`.
  - Interceptores OkHttp com timeouts explícitos e `HttpLoggingInterceptor` apenas em `BuildConfig.DEBUG`.
- **Persistência (`local`):**
  - O Room utiliza **KSP** (jamais KAPT).
  - Estratégia de separação de tabelas para **proteção absoluta de dados autorais** (detalhada na Seção 5).
  - Queries de filtragem (`LIKE`, filtro regional e `isSworn = 1`) executadas diretamente em SQL para garantir baixa latência e reatividade instantânea via `Flow`.

### 3.3 Camada `presentation` (Jetpack Compose + ViewModels)

Responsável por traduzir fluxos de domínio em estados de interface previsíveis e renderizá-los com Material Design 3.

- **Navigation Compose:** Navegação *type-safe* utilizando objetos/classes `@Serializable` (ex.: `HousesListRoute`, `HouseDetailRoute(houseId: String)`).
- **Prevenção de Recomposições Desnecessárias:**
  - Todos os modelos entregues à UI (`HouseUiModel`, `CharacterUiModel`, `NoteUiModel`) são anotados com `@Immutable`.
  - Uso obrigatório de `key = { it.id }` e `contentType` em `LazyColumn` e `LazyRow`.
  - Lambdas de evento estabilizadas via referências ao `ViewModel` (`viewModel::onToggleAllegiance`).
- **Gerenciamento Explícito dos 4 Estados de UI:**

```kotlin
@Immutable
sealed interface HousesUiState {
    data object Loading : HousesUiState

    @Immutable
    data class Success(
        val houses: List<HouseUiModel>,
        val selectedRegion: String?,
        val availableRegions: List<String>,
        val searchQuery: String,
        val onlySwornFilter: Boolean,
        val isRefreshing: Boolean = false,
        val isPaginating: Boolean = false,
        val hasMorePages: Boolean = true,
        val nonBlockingError: String? = null
    ) : HousesUiState

    @Immutable
    data class Empty(
        val reason: EmptyReason,
        val searchQuery: String,
        val selectedRegion: String?,
        val onlySwornFilter: Boolean
    ) : HousesUiState

    @Immutable
    data class Error(
        val message: String
    ) : HousesUiState
}
```

- **Semântica dos 4 Estados:**
  - `Loading`: Exibido exclusivamente quando não há registros exibíveis no cache e a primeira sincronização está em andamento.
  - `Success`: Exibido sempre que houver casas correspondentes no Room. Erros de atualização ou paginação subsequentes são sinalizados via `nonBlockingError` (ex.: `Snackbar`), preservando os dados offline na tela.
  - `Empty`: Exibido quando a busca/filtro/aba de lealdade não retorna correspondências locais ou quando a API retorna lista vazia sem registros locais.
  - `Error`: Exibido somente quando o cache local está vazio e a tentativa inicial de sincronização falha. Fornece ação direta *"Tentar novamente"*.

---

## 4. Fluxo Offline-First e Estratégia de Paginação Sob Demanda

```
┌────────────┐          ┌───────────────┐          ┌──────────────────┐          ┌───────────┐
│  Compose   │          │ HousesViewModel│         │ HouseRepository  │          │ Room DAO  │
│     UI     │          │               │          │       Impl       │          │  (SSOT)   │
└─────┬──────┘          └───────┬───────┘          └────────┬─────────┘          └─────┬─────┘
      │                         │                           │                          │
      │ coleta StateFlow        │ observeHouses()           │ query reativa Flow       │
      │◄────────────────────────┼──────────────────────────◄┼──────────────────────────┤
      │                         │                           │                          │
      │ (Cache vazio na 1ª vez) │ syncInitialDataIfEmpty()  │                          │
      │                         ├──────────────────────────►│ GET /houses?page=1       │
      │                         │                           ├─────► [IceAndFire API]   │
      │                         │                           │◄───── List<HouseDto>     │
      │                         │                           │                          │
      │                         │                           │ upsertHousesPreservingUserState()
      │                         │                           ├─────────────────────────►│
      │                         │                           │                          │
      │ UI atualiza via Flow!   │◄──────────────────────────┼──────────────────────────┤ (Emite nova lista)
      │◄────────────────────────┤                           │                          │
      │                         │                           │                          │
      │ Usuário rola até o fim  │ onLoadNextPage()          │                          │
      ├────────────────────────►├──────────────────────────►│ Lê última página no Room │
      │                         │                           │ GET /houses?page=N+1     │
      │                         │                           ├─────► [IceAndFire API]   │
      │                         │                           │◄───── List<HouseDto>     │
      │                         │                           │ upsert + atualiza cursor │
      │                         │                           ├─────────────────────────►│
      │ Nova página aparece!    │◄──────────────────────────┼──────────────────────────┤
```

### Regras da Sincronização Paginada Sob Demanda

1. **Controle Persistido (`SyncMetadataEntity`):** Armazena `lastSyncedPage` (inteiro), `hasMorePages` (booleano) e `lastSyncedAtMillis` no Room.
2. **Primeira Execução:** Caso `HouseDao.countHouses() == 0`, o repositório sincroniza `page = 1` (`pageSize = 25`) de `An API of Ice and Fire` e simultaneamente (em `async` paralelo dentro de `supervisorScope`) busca a lista compacta da `ThronesAPI` (`Characters`). A falha na `ThronesAPI` **não** deve impedir o sucesso da carga principal de casas.
3. **Carga Sob Demanda (*On-Demand Pagination*):** Ao atingir os últimos itens da `LazyColumn` sem filtro ativo (ou quando `hasMorePages == true` e nenhuma sincronização estiver em curso), o `ViewModel` solicita a página `lastSyncedPage + 1`.
4. **Tratamento de Exceções de Rede:**
   - `IOException` / `UnknownHostException` / `SocketTimeoutException` → capturadas no `RepositoryImpl`, mapeadas para falha amigável (*"Sem conexão com a Cidadela. Exibindo registros locais."*), sem lançar exceções não tratadas para o `ViewModel`.
   - `HttpException` (4xx / 5xx) → capturada e registrada, mantendo o estado local intacto.

---

## 5. Estratégia de SSOT e Preservação de Dados do Usuário

Um dos maiores riscos em aplicativos Offline-First é sobrescrever dados criados pelo usuário quando uma nova carga da API chega e faz `INSERT ... ON CONFLICT REPLACE` na mesma tabela.

### Solução Arquitetural: Separação de Responsabilidades no Schema Room

```
┌──────────────────────────────┐        1 : 0..1       ┌──────────────────────────────┐
│        houses (API)          │◄─────────────────────►│    house_user_state (Local)  │
├──────────────────────────────┤                       ├──────────────────────────────┤
│ PK id: String (ex: "362")    │                       │ PK houseId: String           │
│    name: String              │                       │    isSworn: Boolean          │
│    region: String            │                       │    swornAtMillis: Long?      │
│    coatOfArms: String        │                       └──────────────────────────────┘
│    words: String             │
│    seatsRaw: String          │        1 : 0..N       ┌──────────────────────────────┐
│    titlesRaw: String         │◄─────────────────────►│     maester_notes (Local)    │
│    ancestralWeaponsRaw       │                       ├──────────────────────────────┤
│    founded: String           │                       │ PK id: Long (autoGenerate)   │
│    diedOut: String           │                       │ FK houseId: String (CASCADE) │
│    pageIndex: Int            │                       │    content: String           │
│    normalizedFamilyKey       │                       │    updatedAtMillis: Long     │
└──────────────────────────────┘                       └──────────────────────────────┘
```

- **Por que funciona:**
  - Quando o `HouseRepositoryImpl` sincroniza dados da *An API of Ice and Fire*, ele executa `@Upsert` **apenas** na tabela `houses`.
  - Como `house_user_state` e `maester_notes` são tabelas independentes vinculadas por `houseId`, um `@Upsert` (ou `INSERT OR REPLACE`) em `houses` **nunca** apaga `isSworn` nem as anotações do Meistre.
  - Atenção importante: Caso seja usado `@Insert(onConflict = OnConflictStrategy.REPLACE)` em `houses` tendo `CASCADE` na FK de `maester_notes`, o SQLite internamente executa `DELETE + INSERT` e dispararia o `CASCADE`! **Regra obrigatória:** Usar `@Upsert` do Room 2.5+ (que faz `INSERT` seguido de `UPDATE` sem deletar a linha) ou não usar `ON DELETE CASCADE` para dados do usuário. **Padrão adotado neste projeto: `@Upsert` na tabela `houses`!**

### Correspondência entre Casas e Personagens (ThronesAPI)

Como a *An API of Ice and Fire* e a *ThronesAPI* não compartilham IDs numéricos idênticos, a vinculação no SSOT utiliza uma chave normalizada `normalizedFamilyKey`:
- Exemplo na `HouseEntity`: `"House Stark of Winterfell"` → remove prefixo `"House "` e sufixo `" of ..."` → `"stark"`.
- Exemplo na `CharacterEntity`: `"House Stark"` ou `"Stark"` → remove `"House "` e normaliza espaços/caixa → `"stark"`.
- A consulta Room ou o mapper relacional associa os retratos cujo `normalizedFamilyKey` coincide com o da casa (com regras de normalização para casos compostos como `"House Targaryen"` / `"Targaryen"`, `"House Lannister"` / `"Lannister"`, `"Greyjoy"`, `"Baratheon"`, `"Tyrell"`, `"Martell"`, `"Bolton"`, `"Tully"`).

---

## 6. Mapeamento de Dados: `DTO -> Entity -> Domain -> UiModel`

Cada fronteira arquitetural possui modelos dedicados para impedir vazamento de anotações de infraestrutura (`@JsonClass`, `@Entity`, `@Immutable`).

### Fluxo Completo de Transformação

```
[JSON Remoto] ──(Moshi)──► [HouseDto] ──(Data Mapper)──► [HouseEntity] ──(Room Flow + Relation)──► [House (Domain)] ──(UI Mapper)──► [HouseUiModel]
```

### Exemplo Concreto do Pipeline de Mapeamento

#### 1. Camada Remota (`HouseDto`)
```kotlin
@JsonClass(generateAdapter = true)
data class HouseDto(
    @Json(name = "url") val url: String,
    @Json(name = "name") val name: String,
    @Json(name = "region") val region: String,
    @Json(name = "coatOfArms") val coatOfArms: String,
    @Json(name = "words") val words: String,
    @Json(name = "titles") val titles: List<String>,
    @Json(name = "seats") val seats: List<String>,
    @Json(name = "founded") val founded: String,
    @Json(name = "diedOut") val diedOut: String,
    @Json(name = "ancestralWeapons") val ancestralWeapons: List<String>
)
```

#### 2. `DTO -> Entity` (Sanitização e Extração de Chave)
- Extrai `id` a partir do último segmento da `url` (ex.: `"https://anapioficeandfire.com/api/houses/362"` → `"362"`).
- Filtra listas contendo apenas strings em branco (`titles.filter { it.isNotBlank() }`), pois a API frequentemente retorna `[""]`.
- Calcula `normalizedFamilyKey` (ex.: `"stark"`) para correlação rápida com `CharacterEntity`.
- Normaliza `region` vazia para `"Unknown Region"` ou mantém vazio para exibição tratada na UI.

#### 3. `Entity + Relation -> Domain`
- O DAO retorna `Flow<List<HouseWithUserState>>`, combinando `HouseEntity`, `HouseUserStateEntity?` e contagem de `MaesterNoteEntity`.
- O mapper converte o objeto composto para o modelo `House` (Kotlin puro):
  - Listas serializadas (via `TypeConverter` ou separador controlado) voltam a `List<String>` imutável.
  - `isSworn = userState?.isSworn ?: false`.

#### 4. `Domain -> UiModel` (Camada `presentation`)
- Converte `House` em `HouseUiModel` anotado com `@Immutable`, aplicando textos amigáveis em Português para campos vazios (ex.: lema vazio → `"Sem lema registrado"`) mantendo o `House` de domínio livre de formatação visual.

---

## 7. Injeção de Dependências com Hilt

| Módulo Hilt | Escopo | Componentes Providos |
| :--- | :--- | :--- |
| `NetworkModule` | `SingletonComponent` | `OkHttpClient`, `Moshi`, instâncias `Retrofit` (`@Named("IceAndFire")` e `@Named("Thrones")`), `IceAndFireApiService`, `ThronesApiService`. |
| `DatabaseModule` | `SingletonComponent` | `WesterosDatabase` (Room), `HouseDao`, `CharacterDao`, `MaesterNoteDao`, `SyncMetadataDao`. |
| `DispatcherModule` | `SingletonComponent` | Qualifiers `@IoDispatcher` e `@DefaultDispatcher` (`CoroutineDispatcher`) para facilitar substituição em testes unitários. |
| `RepositoryModule` | `SingletonComponent` | `@Binds` ligando `HouseRepositoryImpl` à interface `HouseRepository`. |

---

## 8. Estratégia de Testes Unitários

Todos os testes unitários residem em `src/test/` (JVM local, sem dependência de emulador) utilizando **JUnit4**, **MockK** e **kotlinx-coroutines-test** (`StandardTestDispatcher` / `runTest`):

1. **Testes de Mappers (`HouseMappersTest`, `CharacterMappersTest`):**
   - Garantem extração correta do `id` da URL, limpeza de `[""]`, cálculo de `normalizedFamilyKey` e preservação de campos opcionais.
2. **Testes de UseCases (`SaveMaesterNoteUseCaseTest`, `ToggleAllegianceUseCaseTest`, `ObserveHousesUseCaseTest`):**
   - Validam regras puras de negócio (ex.: rejeição de nota em branco, delegação correta para o `HouseRepository` mockado com MockK).
3. **Testes de Repositório (`HouseRepositoryImplTest`):**
   - Verificam o comportamento Offline-First: quando o Retrofit lança `IOException`, o método de sincronização retorna `Result.failure` sem quebrar, e os dados existentes no DAO continuam sendo emitidos.
   - Verificam que a falha na `ThronesAPI` não invalida a persistência das casas da *An API of Ice and Fire*.
4. **Testes de ViewModel (`HousesViewModelTest`, `HouseDetailViewModelTest`):**
   - Validam todas as transições entre os 4 estados (`Loading`, `Success`, `Empty`, `Error`), aplicação de filtros/busca, acionamento de paginação sob demanda e execução de juramento/anotações com `MainDispatcherRule`.
