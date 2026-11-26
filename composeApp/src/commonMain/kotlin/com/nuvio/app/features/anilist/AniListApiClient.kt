package com.nuvio.app.features.anilist

import com.nuvio.app.core.network.createApiHttpClient
import com.nuvio.app.core.network.readBoundedResponseBody
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentLength
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val ANILIST_GRAPHQL_ENDPOINT = "https://graphql.anilist.co"

private val aniListJson = Json { ignoreUnknownKeys = true }

internal class AniListApiException(
    val status: Int? = null,
    override val message: String,
) : Exception(message)

internal class AniListApiClient(
    private val httpClient: HttpClient = createApiHttpClient(),
) {
    suspend fun fetchViewer(accessToken: String): AniListUser {
        val query = "query { Viewer { id name avatar { medium } } }"
        val responseBody = postGraphQl(
            accessToken = accessToken,
            query = query,
        )
        val parsed = aniListJson.decodeFromString<GraphQlResponse<ViewerData>>(responseBody)
        ensureNoFatalErrors(parsed.errors, parsed.data?.viewer)
        val viewer = parsed.data?.viewer
            ?: throw AniListApiException(message = "AniList viewer response was empty")
        return AniListUser(
            id = viewer.id,
            name = viewer.name,
            avatarUrl = viewer.avatar?.medium,
        )
    }

    suspend fun fetchAnimeLibrary(accessToken: String, userId: Int): List<AniListMediaEntry> {
        val query =
            "query (\$userId: Int) { MediaListCollection(userId: \$userId, type: ANIME) { " +
                "lists { status entries { id status score(format: POINT_100) progress updatedAt " +
                "media { id idMal title { romaji english } format status episodes duration seasonYear " +
                "coverImage { extraLarge large } averageScore } } } } }"
        val variables = buildJsonObject {
            put("userId", userId)
        }
        val responseBody = postGraphQl(
            accessToken = accessToken,
            query = query,
            variables = variables,
        )
        return parseMediaListCollectionResponse(responseBody)
    }

    suspend fun saveMediaListEntry(
        accessToken: String,
        mediaId: Int,
        status: AniListListStatus? = null,
        progress: Int? = null,
        scoreRaw: Int? = null,
    ): AniListMediaEntry? {
        val mutation =
            "mutation (\$mediaId: Int, \$status: MediaListStatus, \$progress: Int, \$scoreRaw: Int) { " +
                "SaveMediaListEntry(mediaId: \$mediaId, status: \$status, progress: \$progress, scoreRaw: \$scoreRaw) { " +
                "id status score(format: POINT_100) progress updatedAt " +
                "media { id idMal title { romaji english } format status episodes duration seasonYear " +
                "coverImage { extraLarge large } averageScore } } }"
        val variables = buildJsonObject {
            put("mediaId", mediaId)
            status?.let { put("status", it.name) }
            progress?.let { put("progress", it) }
            scoreRaw?.let { put("scoreRaw", it) }
        }
        val responseBody = postGraphQl(
            accessToken = accessToken,
            query = mutation,
            variables = variables,
        )
        val parsed = aniListJson.decodeFromString<GraphQlResponse<SaveMediaListEntryData>>(responseBody)
        ensureNoFatalErrors(parsed.errors, parsed.data?.saveMediaListEntry)
        val entry = parsed.data?.saveMediaListEntry ?: return null
        return entry.toAniListMediaEntry(fallbackStatus = status)
    }

    suspend fun deleteMediaListEntry(accessToken: String, entryId: Int): Boolean {
        val mutation =
            "mutation (\$id: Int) { DeleteMediaListEntry(id: \$id) { deleted } }"
        val variables = buildJsonObject {
            put("id", entryId)
        }
        val responseBody = postGraphQl(
            accessToken = accessToken,
            query = mutation,
            variables = variables,
        )
        val parsed = aniListJson.decodeFromString<GraphQlResponse<DeleteMediaListEntryData>>(responseBody)
        ensureNoFatalErrors(parsed.errors, parsed.data?.deleteMediaListEntry)
        return parsed.data?.deleteMediaListEntry?.deleted == true
    }

    suspend fun searchMediaIdByTitleOrMal(
        accessToken: String?,
        title: String?,
        malId: Int?,
    ): Int? {
        if (malId != null && malId > 0) {
            val malQuery = "query (\$idMal: Int) { Media(idMal: \$idMal, type: ANIME) { id } }"
            val malVars = buildJsonObject { put("idMal", malId) }
            val malIdResult = querySingleMediaId(accessToken, malQuery, malVars)
            if (malIdResult != null) return malIdResult
        }

        val normalizedTitle = title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val titleQuery = "query (\$search: String) { Media(search: \$search, type: ANIME) { id } }"
        val titleVars = buildJsonObject { put("search", normalizedTitle) }
        return querySingleMediaId(accessToken, titleQuery, titleVars)
    }

    private suspend fun querySingleMediaId(
        accessToken: String?,
        query: String,
        variables: JsonObject,
    ): Int? {
        return try {
            val responseBody = postGraphQl(
                accessToken = accessToken,
                query = query,
                variables = variables,
            )
            val parsed = aniListJson.decodeFromString<GraphQlResponse<MediaLookupData>>(responseBody)
            parsed.data?.media?.id
        } catch (error: AniListApiException) {
            if (error.status == 404) null else throw error
        }
    }

    private suspend fun postGraphQl(
        accessToken: String?,
        query: String,
        variables: JsonObject? = null,
    ): String {
        val requestPayload = aniListJson.encodeToString(
            GraphQlRequest(
                query = query,
                variables = variables,
            ),
        )
        return httpClient.preparePost(ANILIST_GRAPHQL_ENDPOINT) {
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            accessToken?.trim()?.takeIf { it.isNotBlank() }?.let { token ->
                header("Authorization", "Bearer $token")
            }
            setBody(requestPayload)
        }.execute { response ->
            val body = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
            val statusCode = response.status.value
            if (statusCode !in 200..299) {
                val errorMessage = runCatching {
                    aniListJson.decodeFromString<GraphQlResponse<JsonObject>>(body)
                        .errors
                        ?.firstOrNull()
                        ?.message
                }.getOrNull()
                throw AniListApiException(
                    status = statusCode,
                    message = errorMessage ?: "AniList request failed with HTTP $statusCode",
                )
            }
            body
        }
    }
}

internal fun parseMediaListCollectionResponse(jsonText: String): List<AniListMediaEntry> {
    val parsed = aniListJson.decodeFromString<GraphQlResponse<MediaListCollectionData>>(jsonText)
    ensureNoFatalErrors(parsed.errors, parsed.data?.mediaListCollection)
    val lists = parsed.data?.mediaListCollection?.lists.orEmpty()
    val entries = mutableListOf<AniListMediaEntry>()
    for (list in lists) {
        val listStatus = parseAniListListStatus(list.status)
        for (rawEntry in list.entries.orEmpty()) {
            val mapped = rawEntry.toAniListMediaEntry(fallbackStatus = listStatus) ?: continue
            entries += mapped
        }
    }
    return entries.distinctBy { it.mediaId }
}

private fun ensureNoFatalErrors(errors: List<GraphQlError>?, data: Any?) {
    if (!errors.isNullOrEmpty() && data == null) {
        val first = errors.first()
        throw AniListApiException(
            status = first.status,
            message = first.message ?: "AniList GraphQL error",
        )
    }
}

private fun parseAniListListStatus(raw: String?): AniListListStatus? {
    val normalized = raw?.trim()?.uppercase() ?: return null
    return AniListListStatus.entries.firstOrNull { it.name == normalized }
}

private fun MediaListEntryNode.toAniListMediaEntry(
    fallbackStatus: AniListListStatus?,
): AniListMediaEntry? {
    val mediaNode = media ?: return null
    val romaji = mediaNode.title?.romaji?.takeIf { it.isNotBlank() }
        ?: mediaNode.title?.english?.takeIf { it.isNotBlank() }
        ?: return null
    return AniListMediaEntry(
        entryId = id,
        mediaId = mediaNode.id,
        malId = mediaNode.idMal,
        titleRomaji = romaji,
        titleEnglish = mediaNode.title?.english,
        format = mediaNode.format,
        status = mediaNode.status,
        episodes = mediaNode.episodes,
        duration = mediaNode.duration,
        seasonYear = mediaNode.seasonYear,
        coverImageUrl = mediaNode.coverImage?.extraLarge ?: mediaNode.coverImage?.large,
        averageScore = mediaNode.averageScore,
        userStatus = parseAniListListStatus(status) ?: fallbackStatus,
        progress = progress ?: 0,
        userScore = score?.toInt() ?: 0,
        updatedAtSeconds = updatedAt ?: 0L,
    )
}

@Serializable
private data class GraphQlRequest(
    val query: String,
    val variables: JsonObject? = null,
)

@Serializable
private data class GraphQlResponse<T>(
    val data: T? = null,
    val errors: List<GraphQlError>? = null,
)

@Serializable
private data class GraphQlError(
    val message: String? = null,
    val status: Int? = null,
)

@Serializable
private data class ViewerData(
    @SerialName("Viewer")
    val viewer: ViewerNode? = null,
)

@Serializable
private data class ViewerNode(
    val id: Int,
    val name: String,
    val avatar: ViewerAvatarNode? = null,
)

@Serializable
private data class ViewerAvatarNode(
    val medium: String? = null,
)

@Serializable
private data class MediaListCollectionData(
    @SerialName("MediaListCollection")
    val mediaListCollection: MediaListCollectionNode? = null,
)

@Serializable
private data class MediaListCollectionNode(
    val lists: List<MediaListGroupNode>? = null,
)

@Serializable
private data class MediaListGroupNode(
    val status: String? = null,
    val entries: List<MediaListEntryNode>? = null,
)

@Serializable
private data class MediaListEntryNode(
    val id: Int = 0,
    val status: String? = null,
    val score: Double? = null,
    val progress: Int? = null,
    val updatedAt: Long? = null,
    val media: MediaNode? = null,
)

@Serializable
private data class MediaNode(
    val id: Int,
    val idMal: Int? = null,
    val title: MediaTitleNode? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val seasonYear: Int? = null,
    val coverImage: MediaCoverImageNode? = null,
    val averageScore: Int? = null,
)

@Serializable
private data class MediaTitleNode(
    val romaji: String? = null,
    val english: String? = null,
)

@Serializable
private data class MediaCoverImageNode(
    val extraLarge: String? = null,
    val large: String? = null,
)

@Serializable
private data class SaveMediaListEntryData(
    @SerialName("SaveMediaListEntry")
    val saveMediaListEntry: MediaListEntryNode? = null,
)

@Serializable
private data class DeleteMediaListEntryData(
    @SerialName("DeleteMediaListEntry")
    val deleteMediaListEntry: DeleteResultNode? = null,
)

@Serializable
private data class DeleteResultNode(
    val deleted: Boolean = false,
)

@Serializable
private data class MediaLookupData(
    @SerialName("Media")
    val media: MediaIdNode? = null,
)

@Serializable
private data class MediaIdNode(
    val id: Int,
)
