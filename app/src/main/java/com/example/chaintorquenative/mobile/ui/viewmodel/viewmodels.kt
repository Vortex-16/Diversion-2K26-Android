package com.example.chaintorquenative.mobile.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaintorquenative.mobile.data.repository.MarketplaceRepository
import com.example.chaintorquenative.mobile.data.repository.UserRepository
import com.example.chaintorquenative.mobile.data.repository.Web3Repository
import com.example.chaintorquenative.mobile.data.api.MarketplaceItem
import com.example.chaintorquenative.mobile.data.api.UserNFT
import com.example.chaintorquenative.mobile.data.api.UserProfile
import com.example.chaintorquenative.wallet.MetaMaskManager
import com.example.chaintorquenative.wallet.WalletConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val marketplaceRepository: MarketplaceRepository,
    private val web3Repository: Web3Repository,
    private val metaMaskManager: MetaMaskManager
) : ViewModel() {

    private val _marketplaceItems = MutableLiveData<List<MarketplaceItem>>()
    val marketplaceItems: LiveData<List<MarketplaceItem>> = _marketplaceItems

    private val _selectedItem = MutableLiveData<MarketplaceItem?>()
    val selectedItem: LiveData<MarketplaceItem?> = _selectedItem

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _purchaseSuccess = MutableLiveData<String?>()
    val purchaseSuccess: LiveData<String?> = _purchaseSuccess

    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _filteredItems = MutableLiveData<List<MarketplaceItem>>()
    val filteredItems: LiveData<List<MarketplaceItem>> = _filteredItems

    private val _selectedCategory = MutableLiveData<String>("All")
    val selectedCategory: LiveData<String> = _selectedCategory

    init {
        loadMarketplaceItems()
    }

    fun loadMarketplaceItems() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            marketplaceRepository.getMarketplaceItems()
                .onSuccess { items ->
                    _marketplaceItems.value = items
                    applyFilters()
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            _loading.value = false
            _isRefreshing.value = false
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        loadMarketplaceItems()
    }

    private fun applyFilters() {
        val items = _marketplaceItems.value ?: emptyList()
        val query = _searchQuery.value ?: ""
        val category = _selectedCategory.value ?: "All"

        _filteredItems.value = items.filter { item ->
            // 1. Text Search (Title, Description, Seller)
            val matchesSearch = query.isBlank() ||
                    item.title?.contains(query, ignoreCase = true) == true ||
                    item.description?.contains(query, ignoreCase = true) == true ||
                    item.seller?.contains(query, ignoreCase = true) == true

            // 2. Category Filter
            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)

            matchesSearch && matchesCategory
        }
    }

    fun searchItems(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun selectItem(item: MarketplaceItem) {
        _selectedItem.value = item
    }

    fun loadItemDetails(tokenId: Int) {
        viewModelScope.launch {
            _loading.value = true

            marketplaceRepository.getMarketplaceItem(tokenId)
                .onSuccess { item ->
                    _selectedItem.value = item
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            _loading.value = false
        }
    }

    fun purchaseItem(tokenId: Int, buyerAddress: String, price: Double) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _purchaseSuccess.value = null

            android.util.Log.d("MarketplaceViewModel", "purchaseItem called - tokenId: $tokenId, buyer: $buyerAddress, price: $price ETH")
            
            // 1. Prepare Data for purchaseToken(uint256)
            // Selector: 0xc2db2c42
            val functionSelector = "0xc2db2c42"
            val paddedTokenId = "%064x".format(tokenId)
            val data = functionSelector + paddedTokenId
            
            // 2. Prepare Value (Price to Wei Hex)
            val priceWei = java.math.BigDecimal(price)
                .multiply(java.math.BigDecimal.TEN.pow(18))
                .toBigInteger()
            val valueHex = "0x" + priceWei.toString(16)
            
            // Contract Address (Fresh Start v2)
            val contractAddress = "0x38Ada2D66de5A9d0aF3734E96aC11E6B2366BfF4"

            // 3. Send Transaction
            metaMaskManager.sendTransaction(
                fromAddress = buyerAddress,
                toAddress = contractAddress,
                data = data,
                value = valueHex,
                onSuccess = { txHash ->
                     _purchaseSuccess.value = txHash
                     _loading.value = false
                     android.util.Log.d("MarketplaceViewModel", "Purchase successful: $txHash")
                },
                onError = { errorMsg ->
                    _error.value = "Purchase failed: $errorMsg"
                    _loading.value = false
                    android.util.Log.e("MarketplaceViewModel", "Purchase error: $errorMsg")
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearPurchaseSuccess() {
        _purchaseSuccess.value = null
    }
}

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val web3Repository: Web3Repository
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _userPurchases = MutableLiveData<List<MarketplaceItem>>()
    val userPurchases: LiveData<List<MarketplaceItem>> = _userPurchases

    private val _walletBalance = MutableLiveData<String>("")
    val walletBalance: LiveData<String> = _walletBalance

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isRefreshing = MutableLiveData<Boolean>(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // Store the current address for refresh purposes
    private var currentAddress: String? = null

    fun loadUserData(address: String) {
        android.util.Log.d("UserProfileViewModel", "loadUserData called with address: $address")
        currentAddress = address
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            userRepository.registerUser(address)
                .onSuccess { profile ->
                    android.util.Log.d("UserProfileViewModel", "User registered: ${profile.walletAddress}")
                    _userProfile.value = profile
                }
                .onFailure { 
                    android.util.Log.e("UserProfileViewModel", "Register user failed: ${it.message}")
                }

            loadUserPurchases(address)
            _loading.value = false
        }
    }

    fun loadUserPurchases(address: String) {
        viewModelScope.launch {
            _loading.value = true
            userRepository.getUserPurchases(address)
                .onSuccess { purchases ->
                    _userPurchases.value = purchases
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            _loading.value = false
            _isRefreshing.value = false
        }
    }

    fun refresh() {
        currentAddress?.let { address ->
            _isRefreshing.value = true
            loadUserData(address)
        }
    }

    fun clearError() {
        _error.value = null
    }
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val web3Repository: Web3Repository,
    private val metaMaskManager: MetaMaskManager
) : ViewModel() {

    enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    private val _walletAddress = MutableLiveData<String?>()
    val walletAddress: LiveData<String?> = _walletAddress

    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _balance = MutableLiveData<String>("")
    val balance: LiveData<String> = _balance

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _connectionStatus = MutableLiveData<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus
    
    private val _chainName = MutableLiveData<String>("Sepolia")
    val chainName: LiveData<String> = _chainName

    init {
        // Observe MetaMask state changes
        viewModelScope.launch {
            metaMaskManager.connectionState.collect { state ->
                when (state) {
                    is WalletConnectionState.Disconnected -> {
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        _isConnected.value = false
                        _walletAddress.value = null
                        _balance.value = ""
                    }
                    is WalletConnectionState.Connecting -> {
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                        _loading.value = true
                    }
                    is WalletConnectionState.Connected -> {
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        _isConnected.value = true
                        _walletAddress.value = state.address
                        _balance.value = state.balance
                        _loading.value = false
                        _chainName.value = if (state.chainId.contains("11155111")) "Sepolia" else "Unknown"
                    }
                    is WalletConnectionState.Error -> {
                        _connectionStatus.value = ConnectionStatus.ERROR
                        _error.value = state.message
                        _loading.value = false
                    }
                }
            }
        }
    }

    /**
     * Connect wallet using MetaMask SDK
     */
    fun connectWallet() {
        _loading.value = true
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _error.value = null
        metaMaskManager.connect()
    }

    fun disconnectWallet() {
        metaMaskManager.disconnect()
        _walletAddress.value = null
        _isConnected.value = false
        _balance.value = ""
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
