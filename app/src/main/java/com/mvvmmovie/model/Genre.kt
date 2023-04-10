package com.mvvmmovie.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
class Genre {

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("name")
    var name: String? = null
}