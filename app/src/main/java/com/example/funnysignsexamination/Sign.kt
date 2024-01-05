package com.example.funnysignsexamination

data class Sign(
    val id: String,
    val name: String,
    val imageUrl: String,
    val location: String,
    val rating: Double,
    val isFavourite: Boolean
) {

    @Suppress("unused")
    constructor() : this("", "", "", "",0.0, false)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "imageUrl" to imageUrl,
            "location" to location,
            "rating" to rating,
            "isFavourite" to isFavourite
        )
    }
}
