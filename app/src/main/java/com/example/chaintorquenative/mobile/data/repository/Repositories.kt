package com.example.chaintorquenative.mobile.data.repository

import com.example.chaintorquenative.mobile.data.api.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for marketplace-related operations
 */
@Singleton
class MarketplaceRepository @Inject constructor(
    private val apiService: ChainTorqueApiService
) {
    suspend fun getMarketplaceItems(): Result<List<MarketplaceItem>> {
        return try {
            val response = apiService.getMarketplaceItems()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to load items"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMarketplaceItem(tokenId: Int): Result<MarketplaceItem> {
        return try {
            val response = apiService.getMarketplaceItem(tokenId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Item not found"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPurchase(
        tokenId: Int,
        transactionHash: String,
        buyerAddress: String,
        price: Double
    ): Result<MarketplaceItem> {
        return try {
            val request = SyncPurchaseRequest(tokenId, transactionHash, buyerAddress, price)
            val response = apiService.syncPurchase(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Sync failed"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Repository for user-related operations
 */
@Singleton
class UserRepository @Inject constructor(
    private val apiService: ChainTorqueApiService
) {
    suspend fun getUserNFTs(address: String): Result<List<UserNFT>> {
        return try {
            val response = apiService.getUserNFTs(address)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to load NFTs"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPurchases(address: String): Result<List<MarketplaceItem>> {
        return try {
            val response = apiService.getUserPurchases(address)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to load purchases"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSales(address: String): Result<List<MarketplaceItem>> {
        return try {
            val response = apiService.getUserSales(address)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to load sales"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(walletAddress: String): Result<UserProfile> {
        return try {
            val response = apiService.registerUser(mapOf("walletAddress" to walletAddress))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Registration failed"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Repository for Web3/wallet-related operations
 * Note: Actual blockchain transactions would be handled by WalletConnect/Web3
 * This repository handles backend API calls related to Web3
 */
@Singleton
class Web3Repository @Inject constructor(
    private val apiService: ChainTorqueApiService
) {
    suspend fun getWeb3Status(): Result<Web3Status> {
        return try {
            val response = apiService.getWeb3Status()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: "Failed to get Web3 status"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Wallet address validation (client-side)
    fun isValidEthereumAddress(address: String): Boolean {
        return address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
    }

    // Format address for display
    fun formatAddress(address: String): String {
        return if (address.length > 10) {
            "${address.take(6)}...${address.takeLast(4)}"
        } else {
            address
        }
    }
}
