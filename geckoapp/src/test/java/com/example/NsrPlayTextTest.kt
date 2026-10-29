package com.example

import com.example.data.nsrplay.NsrPlayRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class NsrPlayTextTest {

    @Test
    fun cleanTitle_removesProviderSeoPrefixAndSuffix() {
        assertEquals(
            "Biker",
            NsrPlayRepository.cleanTitle("VER Biker Online Gratis HD")
        )
        assertEquals(
            "Sociedad de La Virtud: La Serie",
            NsrPlayRepository.cleanTitle("VER Sociedad de La Virtud: La Serie Online Gratis HD")
        )
    }

    @Test
    fun cleanTitle_keepsNormalTitles() {
        assertEquals("Dune: Parte Dos", NsrPlayRepository.cleanTitle("Dune: Parte Dos"))
    }

    @Test
    fun fixMojibake_repairsDoubleEncodedLatinText() {
        assertEquals("corazón", NsrPlayRepository.fixMojibake("corazÃ³n"))
        assertEquals("Español Latino", NsrPlayRepository.fixMojibake("EspaÃ±ol Latino"))
    }

    @Test
    fun languageLabel_normalizesProviderLanguages() {
        assertEquals("Latino", NsrPlayRepository.languageLabel("Español Latino"))
        assertEquals("Subtitulado", NsrPlayRepository.languageLabel("Subtitulado"))
    }
}
