package com.pallas.launcher

import com.pallas.launcher.domain.atlas.AtlasRepository
import com.pallas.launcher.domain.atlas.AtlasSearchHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AtlasDataTests {

    private fun resolveAtlasDir(): File {
        val candidates = listOf(
            File("src/main/assets/atlas"),
            File("app/src/main/assets/atlas")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate assets/atlas from ${File(".").absolutePath}")
    }

    @Test
    fun atlasBundleIsCompactAndClean() {
        val dir = resolveAtlasDir()
        val landFile = File(dir, "land.json")
        val countriesFile = File(dir, "countries.json")
        val citiesFile = File(dir, "cities.json")

        assertTrue(landFile.isFile)
        assertTrue(countriesFile.isFile)
        assertTrue(citiesFile.isFile)

        val totalBytes = landFile.length() + countriesFile.length() + citiesFile.length()
        assertTrue("Atlas bundle grew unexpectedly: $totalBytes bytes", totalBytes < 6 * 1024 * 1024L)

        val citiesJson = citiesFile.readText(Charsets.UTF_8)
        assertFalse("Population data should be stripped", citiesJson.contains("\"pop\""))
        assertFalse("Population data should be stripped", citiesJson.contains("\"population\""))

        val landJson = landFile.readText(Charsets.UTF_8)
        val countriesJson = countriesFile.readText(Charsets.UTF_8)
        val data = AtlasRepository.parseFromStrings(landJson, countriesJson, citiesJson)

        assertTrue(data.landRings.isNotEmpty())
        assertTrue(data.countries.size >= 240)
        assertTrue(data.cities.size >= 400)
    }

    @Test
    fun surveyOfIndiaPalestineAndMicroStateEnclavesArePreserved() {
        val dir = resolveAtlasDir()
        val data = AtlasRepository.parseFromStrings(
            landJson = File(dir, "land.json").readText(Charsets.UTF_8),
            countriesJson = File(dir, "countries.json").readText(Charsets.UTF_8),
            citiesJson = File(dir, "cities.json").readText(Charsets.UTF_8)
        )

        val indiaPoints = listOf(
            "Srinagar" to (34.0837 to 74.7973),
            "Muzaffarabad" to (34.37 to 73.47),
            "Gilgit" to (35.92 to 74.31),
            "Siachen" to (35.42 to 77.11),
            "Aksai Chin" to (35.10 to 79.10)
        )
        for ((label, coords) in indiaPoints) {
            val matches = AtlasRepository.findAllCountriesAtLatLon(data, coords.first, coords.second)
            val ids = matches.map { it.id }.toSet()
            assertTrue("$label should be inside IND, got $ids", "IND" in ids)
            assertFalse("$label should NOT be inside PAK, got $ids", "PAK" in ids)
            assertFalse("$label should NOT be inside CHN, got $ids", "CHN" in ids)
        }

        val palestine = data.countries.firstOrNull { it.id == "PSE" }
        assertNotNull("Palestine (PSE) entry must exist", palestine)
        assertEquals("PSE", AtlasRepository.hitTestCountryByLatLon(data, 31.9038, 35.2034)?.id)
        assertEquals("PSE", AtlasRepository.hitTestCountryByLatLon(data, 31.5017, 34.4668)?.id)

        // Micro-states & interior enclave holes
        assertEquals(listOf("VAT"), AtlasRepository.findAllCountriesAtLatLon(data, 41.9029, 12.4534).map { it.id })
        assertEquals(listOf("SMR"), AtlasRepository.findAllCountriesAtLatLon(data, 43.9366, 12.4463).map { it.id })
        assertEquals(listOf("LSO"), AtlasRepository.findAllCountriesAtLatLon(data, -29.31, 27.48).map { it.id })
        assertEquals("SGP", AtlasRepository.hitTestCountryByLatLon(data, 1.3521, 103.8198)?.id)

        val byId = data.countries.associateBy { it.id }
        for (code in listOf("VAT", "MCO", "SMR", "LIE", "AND", "MLT", "SGP", "BHR")) {
            assertNotNull("Expected country $code to be present", byId[code])
        }
        assertTrue("Expected Maldives to retain >= 100 islands", (byId["MDV"]?.polygons?.size ?: 0) >= 100)
        assertTrue("Expected India to retain >= 25 polygons", (byId["IND"]?.polygons?.size ?: 0) >= 25)
    }

    @Test
    fun atlasSearchFindsMajorCountriesAndCities() {
        val dir = resolveAtlasDir()
        val data = AtlasRepository.parseFromStrings(
            landJson = File(dir, "land.json").readText(Charsets.UTF_8),
            countriesJson = File(dir, "countries.json").readText(Charsets.UTF_8),
            citiesJson = File(dir, "cities.json").readText(Charsets.UTF_8)
        )

        val indiaHits = AtlasRepository.searchAtlas(data, "India")
        assertTrue(indiaHits.filterIsInstance<AtlasSearchHit.Country>().any { it.country.id == "IND" })

        val palestineHits = AtlasRepository.searchAtlas(data, "Palestine")
        assertTrue(palestineHits.filterIsInstance<AtlasSearchHit.Country>().any { it.country.id == "PSE" })

        val vaticanHits = AtlasRepository.searchAtlas(data, "Vatican")
        assertTrue(vaticanHits.filterIsInstance<AtlasSearchHit.Country>().any { it.country.id == "VAT" })

        val delhiHits = AtlasRepository.searchAtlas(data, "New Delhi")
        assertTrue(delhiHits.filterIsInstance<AtlasSearchHit.City>().any { it.city.name == "New Delhi" })

        val tokyoHits = AtlasRepository.searchAtlas(data, "Tokyo")
        assertTrue(tokyoHits.filterIsInstance<AtlasSearchHit.City>().any { it.city.name == "Tokyo" })
    }
}
