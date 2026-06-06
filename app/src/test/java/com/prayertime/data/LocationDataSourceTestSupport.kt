package com.prayertime.data

internal object LocationDataSourceTestSupport {
    fun initializeFromTestResource() {
        val json =
            checkNotNull(
                LocationDataSourceTestSupport::class.java.classLoader?.getResourceAsStream("locations.json"),
            ) { "locations.json missing from test classpath — copy from src/main/assets" }
                .bufferedReader()
                .readText()
        LocationDataSource.initializeForTests(LocationCatalogLoader.parse(json))
    }
}
