package com.mvvmmovie.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Movies : Serializable {

    @SerializedName("page")
    var page: Int? = null

    @SerializedName("results")
    var results: List<Movie>? = null

    @SerializedName("total_pages")
    var total_pages: Int? = null

    @SerializedName("total_results")
    var total_results: Int? = null
}