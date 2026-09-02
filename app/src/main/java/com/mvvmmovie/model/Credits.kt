package com.mvvmmovie.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Credits : Serializable {

    @SerializedName("cast")
    var cast: List<Cast>? = null
}

class Cast : Serializable {

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("character")
    var character: String? = null

    @SerializedName("profile_path")
    var profile_path: String? = null
}
