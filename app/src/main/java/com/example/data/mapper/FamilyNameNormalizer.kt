package com.example.data.mapper

/**
 * Normaliza nomes de Casas (An API of Ice and Fire) e famílias (ThronesAPI)
 * para uma chave canônica em minúsculas, permitindo correlacionar retratos
 * às respectivas Casas no SSOT local.
 */
internal object FamilyNameNormalizer {

  private val familyAliases =
    mapOf(
      "nymeros martell" to "martell",
      "lanister" to "lannister",
    )

  fun normalizeFromHouseName(houseName: String): String {
    val base =
      houseName
        .trim()
        .lowercase()
        .removePrefix("house ")
        .substringBefore(" of ")
        .trim()
    if (base.isEmpty()) return ""
    return familyAliases[base] ?: base
  }

  fun normalizeFromCharacterFamily(family: String): String {
    val base =
      family
        .trim()
        .lowercase()
        .removePrefix("house ")
        .substringBefore(" of ")
        .trim()
    if (base.isEmpty() || base == "unknown" || base == "none") return ""
    return familyAliases[base] ?: base
  }
}
