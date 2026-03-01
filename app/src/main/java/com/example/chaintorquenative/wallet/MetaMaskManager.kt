package com.example.chaintorquenative.wallet

import android.content.Context
import android.util.Log
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.DappMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MetaMask"

@Singleton
class MetaMaskManager @Inject constructor() {

    private val _connectionState = MutableStateFlow<WalletConnectionState>(WalletConnectionState.Disconnected)
    val connectionState: StateFlow<WalletConnectionState> = _connectionState.asStateFlow()

    private lateinit var ethereum: Ethereum

    fun initialize(context: Context) {
        val dappMetadata = DappMetadata(
            "ChainTorque",
            "https://chaintorque.com",
            "https://chaintorque.com/logo.png"
        )

        ethereum = Ethereum(context, dappMetadata)

        Log.d(TAG, "MetaMask SDK Initialized")
    }

    fun connect() {
        Log.d(TAG, "Connecting to MetaMask...")
        _connectionState.value = WalletConnectionState.Connecting

        ethereum.connect { result ->
            when (result) {
                is io.metamask.androidsdk.Result.Success.Item -> {
                    val address = result.value
                    Log.d(TAG, "Connected: $address. Switching to Sepolia...")
                    
                    // Force switch to Sepolia (0xaa36a7)
                    switchChain("0xaa36a7", 
                        onSuccess = {
                            Log.d(TAG, "Switched to Sepolia successfully")
                            _connectionState.value = WalletConnectionState.Connected(
                                address = address,
                                chainId = "11155111"
                            )
                        },
                        onError = { error ->
                            Log.e(TAG, "Failed to switch chain: $error")
                            // Connect anyway but warn? Or fail? 
                            // Let's connect but potentially wrong network.
                             _connectionState.value = WalletConnectionState.Connected(
                                address = address,
                                chainId = "11155111" // We assume user might handle it manually if auto-switch fails
                            )
                        }
                    )
                }

                is io.metamask.androidsdk.Result.Success.Items -> {
                    val addresses = result.value
                    val address = addresses.firstOrNull() ?: ""
                    Log.d(TAG, "Connected (Items): $address. Switching to Sepolia...")
                    
                     switchChain("0xaa36a7", 
                        onSuccess = {
                            Log.d(TAG, "Switched to Sepolia successfully")
                            _connectionState.value = WalletConnectionState.Connected(
                                address = address,
                                chainId = "11155111"
                            )
                        },
                        onError = { error ->
                            Log.e(TAG, "Failed to switch chain: $error")
                             _connectionState.value = WalletConnectionState.Connected(
                                address = address,
                                chainId = "11155111"
                            )
                        }
                    )
                }

                is io.metamask.androidsdk.Result.Error -> {
                    Log.e(TAG, "Connection Error: ${result.error.message}")
                    _connectionState.value =
                        WalletConnectionState.Error(result.error.message ?: "Unknown error")
                }
                else -> {
                    Log.e(TAG, "Unknown result type: $result")
                    _connectionState.value = WalletConnectionState.Error("Unknown result type")
                }
            }
        }

    }

    fun disconnect() {
        ethereum.disconnect()
        _connectionState.value = WalletConnectionState.Disconnected
    }

    /**
     * Signs and sends a transaction (eth_sendTransaction)
     * @param fromAddress The sender's address
     * @param toAddress The contract address (or recipient)
     * @param data The hex-encoded data for the smart contract call
     * @param value The value to send in Wei (hex string) - defaults to "0x0"
     * @param onSuccess Callback for success (returns tx hash)
     * @param onError Callback for error
     */
    fun sendTransaction(
        fromAddress: String,
        toAddress: String,
        data: String,
        value: String = "0x0",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val params: MutableMap<String, Any> = HashMap()
        params["from"] = fromAddress
        params["to"] = toAddress
        params["data"] = data
        params["value"] = value
        
        // Sepolia chain ID is recommended to be strictly enforced if needed, 
        // but MetaMask usually handles network switching prompt if chain doesn't match.

        val request = io.metamask.androidsdk.EthereumRequest(
            method = "eth_sendTransaction",
            params = listOf(params)
        )

        ethereum.sendRequest(request) { result ->
            when (result) {
                is io.metamask.androidsdk.Result.Success.Item -> {
                    onSuccess(result.value)
                }
                is io.metamask.androidsdk.Result.Error -> {
                    onError(result.error.message ?: "Transaction failed")
                }
                else -> {
                    onError("Unknown transaction result")
                }
            }
        }
    }

    /**
     * Switch the wallet's active chain (wallet_switchEthereumChain)
     * e.g. Sepolia = "0xaa36a7"
     */
    fun switchChain(
        chainId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val params: MutableMap<String, Any> = HashMap()
        params["chainId"] = chainId

        val request = io.metamask.androidsdk.EthereumRequest(
            method = "wallet_switchEthereumChain",
            params = listOf(params)
        )

        Log.d(TAG, "Requesting switch to chain: $chainId")

        ethereum.sendRequest(request) { result ->
            when (result) {
                is io.metamask.androidsdk.Result.Success -> {
                   Log.d(TAG, "Switch chain success")
                   onSuccess()
                }
                is io.metamask.androidsdk.Result.Error -> {
                    Log.e(TAG, "Switch chain failed: ${result.error.message} Code: ${result.error.code}")
                    // Error 4902 (or 4902.0) means chain not added.
                    if (result.error.code.toString().contains("4902")) {
                        Log.d(TAG, "Chain not found. Attempting to add chain...")
                        addSepoliaChain(onSuccess, onError)
                    } else {
                        onError(result.error.message ?: "Switch chain failed")
                    }
                }
                else -> {
                    Log.d(TAG, "Switch chain returned unknown result: $result")
                    onSuccess() 
                }
            }
        }
    }
    
    private fun addSepoliaChain(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val params: MutableMap<String, Any> = HashMap()
        params["chainId"] = "0xaa36a7"
        params["chainName"] = "Sepolia"
        params["rpcUrls"] = listOf("https://sepolia.infura.io/v3/") // Or generic public RPC
        params["nativeCurrency"] = mapOf("name" to "Sepolia Ether", "symbol" to "SEP", "decimals" to 18)
        params["blockExplorerUrls"] = listOf("https://sepolia.etherscan.io")

        val request = io.metamask.androidsdk.EthereumRequest(
            method = "wallet_addEthereumChain",
            params = listOf(params)
        )

        ethereum.sendRequest(request) { result ->
             when (result) {
                is io.metamask.androidsdk.Result.Success -> {
                    Log.d(TAG, "Add chain success. Switching...")
                    // Now try to switch again, or assume added = auto-switched? 
                    // Usually adding prompts to switch.
                    onSuccess()
                }
                is io.metamask.androidsdk.Result.Error -> {
                    Log.e(TAG, "Add chain error: ${result.error.message}")
                    onError(result.error.message ?: "Failed to add chain")
                }
                else -> { onSuccess() }
             }
        }
    }
}