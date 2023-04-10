package com.mvvmmovie.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
class Movie {

    @SerializedName("adult")
    var adult: Boolean? = null

    @SerializedName("backdrop_path")
    var backdrop_path: String? = null

    @SerializedName("belongs_to_collection")
    var belongs_to_collection: BelongToCollection? = null

    @SerializedName("budget")
    var budget: Long? = null

    @SerializedName("genres")
    var genres: List<Genre>? = null

    @SerializedName("homepage")
    var homepage: String? = null

    @SerializedName("genre_ids")
    var genre_ids: List<Int>? = null

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("imdb_id")
    var imdb_id: String? = null

    @SerializedName("original_language")
    var original_language: String? = null

    @SerializedName("original_title")
    var original_title: String? = null

    @SerializedName("overview")
    var overview: String? = null

    @SerializedName("popularity")
    var popularity: Double? = null

    @SerializedName("poster_path")
    var poster_path: String? = null

    @SerializedName("production_companies")
    var production_companies: List<ProductionCompanies>? = null

    @SerializedName("production_countries")
    var production_countries: List<ProductionCountries>? = null

    @SerializedName("release_date")
    var release_date: String? = null

    @SerializedName("revenue")
    var revenue: Long? = null

    @SerializedName("runtime")
    var runtime: Int? = null

    @SerializedName("spoken_languages")
    var spoken_languages: List<SpokenLanguages>? = null

    @SerializedName("status")
    var status: String? = null

    @SerializedName("tagline")
    var tagline: String? = null

    @SerializedName("title")
    var title: String? = null

    @SerializedName("video")
    var video: Boolean? = null

    @SerializedName("vote_average")
    var vote_average: Double? = null

    @SerializedName("vote_count")
    var vote_count: Int? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class BelongToCollection {
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("poster_path")
    var poster_path: String? = null

    @SerializedName("backdrop_path")
    var backdrop_path: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ProductionCompanies {

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("logo_path")
    var logo_path: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("origin_country")
    var origin_country: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ProductionCountries {

    @SerializedName("iso_3166_1")
    var iso_3166_1: String? = null

    @SerializedName("name")
    var name: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SpokenLanguages {
    @SerializedName("english_name")
    var english_name: String? = null

    @SerializedName("iso_639_1")
    var iso_639_1: String? = null

    @SerializedName("name")
    var name: String? = null
}