package com.mvvmmovie.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiKeyInterceptorTest {

    @Test
    fun redact_hidesTheKeyInARequestLine() {
        val line = "--> GET https://api.themoviedb.org/3/movie/popular?page=1&api_key=abc123"

        assertEquals(
            "--> GET https://api.themoviedb.org/3/movie/popular?page=1&api_key=REDACTED",
            ApiKeyInterceptor.redact(line)
        )
    }

    @Test
    fun redact_hidesTheKeyInAResponseLineWithParametersAfterIt() {
        val line = "<-- 200 https://api.themoviedb.org/3/movie/603?api_key=abc123&append_to_response=credits (85ms)"

        assertEquals(
            "<-- 200 https://api.themoviedb.org/3/movie/603?api_key=REDACTED&append_to_response=credits (85ms)",
            ApiKeyInterceptor.redact(line)
        )
    }

    @Test
    fun redact_leavesLinesWithoutAKeyAlone() {
        val line = "<-- END HTTP"

        assertEquals(line, ApiKeyInterceptor.redact(line))
    }
}
