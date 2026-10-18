package com.mvvmmovie.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class MovieSourceTest {

    /** SavedStateHandle writes these into a Bundle, which serializes them across process death. */
    @Test
    fun everySource_survivesJavaSerialization() {
        val sources = listOf(
            MovieSource.Category(MovieCategory.TOP_RATED),
            MovieSource.Search("dune"),
            MovieSource.Favorites
        )

        sources.forEach { source -> assertEquals(source, roundTrip(source)) }
    }

    @Test
    fun searchCacheKey_ignoresCase() {
        assertEquals(MovieSource.Search("dune").cacheKey, MovieSource.Search("DUNE").cacheKey)
    }

    private fun roundTrip(source: MovieSource): MovieSource {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(source) }
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() as MovieSource }
    }
}
