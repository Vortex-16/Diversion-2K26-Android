package com.example.chaintorquenative.wallet

/**
 * Wallet connection states
 */
sealed class WalletConnectionState {
    object Disconnected : WalletConnectionState()
    object Connecting : WalletConnectionState()
    data class Connected(
        val address: String,
        val chainId: String,
        val balance: String = "0.0"
    ) : WalletConnectionState()
    data class Error(val message: String) : WalletConnectionState()
}
