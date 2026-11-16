package com.example.data.local.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WesterosTypeConvertersTest {

  private val converter = WesterosTypeConverters()

  @Test
  fun `round trip preserves string lists and filters empty strings`() {
    val original = listOf("King in the North", "Lord of Winterfell")
    val serialized = converter.fromStringList(original)
    val restored = converter.toStringList(serialized)

    assertEquals(original, restored)
  }

  @Test
  fun `empty list serializes to empty string and restores to empty list`() {
    val serialized = converter.fromStringList(emptyList())
    assertEquals("", serialized)
    assertTrue(converter.toStringList(serialized).isEmpty())
  }
}
