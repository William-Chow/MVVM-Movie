package com.mvvmmovie.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Genre : Serializable {

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("name")
    var name: String? = null
}