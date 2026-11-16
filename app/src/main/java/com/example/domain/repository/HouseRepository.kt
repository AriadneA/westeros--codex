package com.example.domain.repository

import com.example.domain.model.House
import com.example.domain.model.HouseDetail
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositório da camada Domain para o Westeros Codex.
 * Segue o princípio Offline-First (SSOT): toda leitura expõe Flows reativos
 * provenientes da fonte local, enquanto operações de sincronização tratam
 * atualizações em segundo plano de forma resiliente.
 */
interface HouseRepository {

  /**
   * Observa continuamente a lista de Casas filtrada por busca textual,
   * região e estado de lealdade.
   */
  fun observeHouses(
    query: String = "",
    region: String? = null,
    onlySworn: Boolean = false,
  ): Flow<List<House>>

  /**
   * Observa continuamente os detalhes completos de uma Casa específica,
   * incluindo personagens vinculados e anotações do Meistre.
   */
  fun observeHouseDetail(houseId: String): Flow<HouseDetail?>

  /**
   * Observa a lista distinta e ordenada de regiões disponíveis no cache local.
   */
  fun observeAvailableRegions(): Flow<List<String>>

  /**
   * Sincroniza uma página específica da An API of Ice and Fire e persiste no banco local.
   * Retorna [Result.success] com `true` se houver potencial próxima página, ou `false` caso
   * o catálogo remoto tenha chegado ao fim.
   */
  suspend fun syncHousesPage(page: Int, pageSize: Int): Result<Boolean>

  /**
   * Executa a carga inicial paginada (página 1) e dos personagens apenas quando
   * o cache local ainda estiver vazio.
   */
  suspend fun syncInitialDataIfEmpty(): Result<Unit>

  /**
   * Atualiza o catálogo de personagens a partir da ThronesAPI.
   */
  suspend fun syncCharacters(): Result<Unit>

  /**
   * Alterna o estado de Juramento de Lealdade (favorito) de uma Casa localmente.
   */
  suspend fun toggleAllegiance(houseId: String): Result<Unit>

  /**
   * Cria (quando [noteId] é nulo) ou atualiza uma anotação do Meistre vinculada à Casa.
   */
  suspend fun saveMaesterNote(
    houseId: String,
    noteId: Long?,
    content: String,
  ): Result<Unit>

  /**
   * Remove uma anotação do Meistre pelo seu identificador local.
   */
  suspend fun deleteMaesterNote(noteId: Long): Result<Unit>
}
