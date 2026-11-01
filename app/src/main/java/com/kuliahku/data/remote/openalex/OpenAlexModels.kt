package com.kuliahku.data.remote.openalex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAlexWorksResponse(
    val meta: OpenAlexMeta = OpenAlexMeta(),
    val results: List<OpenAlexWork> = emptyList()
)

@Serializable
data class OpenAlexMeta(
    val count: Int = 0,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20
)

@Serializable
data class OpenAlexWork(
    val id: String = "",
    val doi: String? = null,
    val title: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("publication_year") val publicationYear: Int? = null,
    @SerialName("publication_date") val publicationDate: String? = null,
    @SerialName("cited_by_count") val citedByCount: Int? = 0,
    @SerialName("open_access") val openAccess: OpenAlexOa? = OpenAlexOa(),
    val authorships: List<OpenAlexAuthorship>? = emptyList(),
    @SerialName("primary_topic") val primaryTopic: OpenAlexTopic? = null,
    @SerialName("primary_location") val primaryLocation: OpenAlexLocation? = null,
    val locations: List<OpenAlexLocation>? = emptyList(),
    val type: String? = null
)

@Serializable
data class OpenAlexLocation(
    val source: OpenAlexSource? = null
)

@Serializable
data class OpenAlexSource(
    val id: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class OpenAlexOa(
    @SerialName("is_oa") val isOpenAccess: Boolean = false,
    @SerialName("oa_url") val oaUrl: String? = null
)

@Serializable
data class OpenAlexAuthorship(
    val author: OpenAlexAuthor? = null
)

@Serializable
data class OpenAlexAuthor(
    val id: String? = null,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class OpenAlexTopic(
    val id: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val field: OpenAlexField? = null
)

@Serializable
data class OpenAlexField(
    val id: String? = null,
    @SerialName("display_name") val displayName: String? = null
)
