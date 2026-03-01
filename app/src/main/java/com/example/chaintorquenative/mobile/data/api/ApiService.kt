// API Service for ChainTorque Mobile App (Kotlin/Android)
// This mirrors the web app's apiService.ts but in Kotlin

package com.example.chaintorquenative.mobile.data.api

import retrofit2.Response
import retrofit2.http.*

// Data Classes (Models) - Matching Backend API Response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val error: String?
)

// Backend returns seller as a String (wallet address), not an object
// Also some fields may be null from the API
data class MarketplaceItem(
    val tokenId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val priceETH: Double? = null,
    val seller: String? = null,  // Backend returns wallet address as String
    val owner: String? = null,
    val images: List<String>? = null,
    val imageUrl: String? = null,  // Some items have imageUrl instead of images
    val modelUrl: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val views: Int? = null,
    val likes: Int? = null,
    val name: String? = null,
    val createdAt: String? = null,
    val blockchain: String? = null,
    val format: String? = null,
    val status: String? = null,
    val transactionHash: String? = null,
    val username: String? = null // Added to match backend
) {
    // Helper to get first image URL
    fun getDisplayImage(): String {
        return images?.firstOrNull() ?: imageUrl ?: ""
    }

    // Helper to get display price
    fun getDisplayPrice(): Double {
        return priceETH ?: price ?: 0.0
    }

    // Show Username if available, otherwise "Creator"
    fun getShortSeller(): String {
        return username ?: "Creator"
    }
}

data class UserNFT(
    val tokenId: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val imageUrl: String? = null,
    val modelUrl: String? = null,
    val owner: String? = null,
    val createdAt: String? = null
) {
    fun getDisplayImage(): String {
        return image ?: imageUrl ?: ""
    }
}

data class UserProfile(
    val address: String? = null,
    val walletAddress: String? = null,
    val name: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val verified: Boolean? = null,
    val totalNFTs: Int? = null,
    val totalSales: Int? = null,
    val totalPurchased: Int? = null,
    val memberSince: String? = null
)

data class Web3Status(
    val connected: Boolean,
    val account: String?,
    val network: String?,
    val balance: String?
)

data class PurchaseRequest(
    val tokenId: Int,
    val buyerAddress: String,
    val price: Double
)

data class PurchaseResponse(
    val transactionHash: String
)

data class SyncPurchaseRequest(
    val tokenId: Int,
    val transactionHash: String,
    val buyerAddress: String,
    val price: Double
)

// Retrofit API Interface
interface ChainTorqueApiService {

    // Health Check
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>

    // Marketplace Endpoints - matches backend routes/marketplace.js
    @GET("api/marketplace")
    suspend fun getMarketplaceItems(): Response<ApiResponse<List<MarketplaceItem>>>

    @GET("api/marketplace/{tokenId}")
    suspend fun getMarketplaceItem(@Path("tokenId") tokenId: Int): Response<ApiResponse<MarketplaceItem>>

    @GET("api/marketplace/stats")
    suspend fun getMarketplaceStats(): Response<ApiResponse<Any>>

    // User Endpoints - matches backend routes/user.js
    @GET("api/user/{address}/nfts")
    suspend fun getUserNFTs(@Path("address") address: String): Response<ApiResponse<List<UserNFT>>>

    @GET("api/user/{address}/purchases")
    suspend fun getUserPurchases(@Path("address") address: String): Response<ApiResponse<List<MarketplaceItem>>>

    @GET("api/user/{address}/sales")
    suspend fun getUserSales(@Path("address") address: String): Response<ApiResponse<List<MarketplaceItem>>>

    @POST("api/user/register")
    suspend fun registerUser(@Body request: Map<String, String>): Response<ApiResponse<UserProfile>>

    // Sync purchase after blockchain transaction
    @POST("api/marketplace/sync-purchase")
    suspend fun syncPurchase(@Body request: SyncPurchaseRequest): Response<ApiResponse<MarketplaceItem>>

    // Web3 Endpoints
    @GET("api/web3/status")
    suspend fun getWeb3Status(): Response<ApiResponse<Web3Status>>
}
