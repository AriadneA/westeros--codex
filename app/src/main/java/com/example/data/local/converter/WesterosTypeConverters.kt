package com.example.data.local.converter

import androidx.room.TypeConverter

/**
 * Conversores de tipo do Room para persistir listas de strings nas entidades locais
 * sem dependências externas de serialização em tempo de consulta SQL.
 */
class WesterosTypeConverters {

  @TypeConverter
  fun fromStringList(list: List<String>): String {
    if (list.isEmpty()) return ""
    return list.joinToString(separator = DELIMITER) { item ->
      item.replace(ESCAPE_CHAR, ESCAPED_ESCAPE).replace(DELIMITER, ESCAPED_DELIMITER)
    }
  }

  @TypeConverter
  fun toStringList(value: String): List<String> {
    if (value.isEmpty()) return emptyList()
    return value
      .split(DELIMITER)
      .map { token ->
        token.replace(ESCAPED_DELIMITER, DELIMITER).replace(ESCAPED_ESCAPE, ESCAPE_CHAR)
      }
      .filter { it.isNotBlank() }
  }

  private companion object {
    const val ESCAPE_CHAR = "\\"
    const val ESCAPED_ESCAPE = "\\\\"
    const val DELIMITER = "\u001F"
    const val ESCAPED_DELIMITER = "\\u001F"
  }
}
