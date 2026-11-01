package com.kuliahku.data.remote.openalex

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenAlexApi {

    @GET("works")
    suspend fun searchWorks(
        @Query("search") query: String?,
        @Query("filter") filter: String?,
        @Query("sort") sort: String?,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("mailto") mailto: String? = "iijo8362@gmail.com"
    ): OpenAlexWorksResponse
}