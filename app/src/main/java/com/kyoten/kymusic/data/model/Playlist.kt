package com.kyoten.kymusic.data.model

import java.io.Serializable

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>  // ← Cambiado de MutableList a List
) : Serializable