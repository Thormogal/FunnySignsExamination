package com.example.funnysignsexamination

data class Sign(
    val id: String,
    val name: String,
    val imageUrl: String,
    val location: String,
    val rating: Double,
    val isFavourite: Boolean)