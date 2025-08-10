package com.MyApp.Spoonful.repository

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Headers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.Response

/**
 * Data class representing the response structure from Unsplash API.
 * Contains only the essential fields needed for recipe image URLs.
 */
data class UnsplashPhoto(
    val urls: Urls
) {
    data class Urls(val regular: String)
}

/**
 * Retrofit interface for Unsplash API integration.
 * 
 * Provides access to Unsplash's random photo endpoint for fetching
 * recipe-related images. Uses authorization header for API access
 * and supports query-based image search.
 */
interface UnsplashApi {
    @GET("photos/random")
    suspend fun getRandomPhoto(
        @Header("Authorization") authorization: String,
        @Query("query") query: String
    ): Response<UnsplashPhoto>

    companion object {
        fun create(): UnsplashApi {
            return Retrofit.Builder()
                .baseUrl("https://api.unsplash.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UnsplashApi::class.java)
        }
    }
} 