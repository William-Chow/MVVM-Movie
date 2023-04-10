package com.mvvmmovie.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
class Movies {

    @SerializedName("page")
    var page: Int? = null

    @SerializedName("results")
    var results: List<Movie>? = null

    @SerializedName("total_pages")
    var total_pages: Int? = null

    @SerializedName("total_results")
    var total_results: Int? = null
}