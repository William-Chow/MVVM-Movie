package com.mvvmmovie.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Videos : Serializable {

    @SerializedName("results")
    var results: List<Video>? = null
}

class Video : Serializable {

    @SerializedName("id")
    var id: String? = null

    @SerializedName("key")
    var key: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("site")
    var site: String? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("official")
    var official: Boolean? = null

    /** Only YouTube keys can be turned into a watchable link by [com.mvvmmovie.Utils]. */
    val isYouTube: Boolean
        get() = site.equals("YouTube", ignoreCase = true) && !key.isNullOrBlank()
}
