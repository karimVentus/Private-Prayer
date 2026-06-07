package com.prayertime.data

internal object LocationDataSourceTestSupport {
    fun initializeFromTestResource() {
        val loader =
            checkNotNull(LocationDataSourceTestSupport::class.java.classLoader) {
                "test classloader unavailable"
            }
        val json =
            checkNotNull(loader.getResourceAsStream("locations.json")) {
                "locations.json missing from test classpath — copy from src/main/assets"
            }.bufferedReader()
                .readText()
        val citiesAr =
            loader.getResourceAsStream("cities_ar.json")?.use { stream ->
                LocationCatalogLoader.parseCitiesAr(stream.bufferedReader().readText())
            } ?: emptyMap()
        LocationDataSource.initializeForTests(LocationCatalogLoader.parse(json, citiesAr))
    }
}
