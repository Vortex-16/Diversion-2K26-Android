# ChainTorque Native

**Premium 3D CAD NFT Marketplace - Android App**

A native Android application for browsing, purchasing, and managing NFT-based CAD assets on the Ethereum Sepolia testnet.

## Features

- **NFT Marketplace** - Browse and purchase premium 3D CAD assets
- **MetaMask SDK Integration** - Connect directly with MetaMask Android App
- **User Profiles** - View owned NFTs, purchase history, and sales
- **Sepolia Testnet** - Built for Ethereum Sepolia testnet transactions

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Hilt DI |
| Networking | Retrofit, OkHttp |
| Wallet | MetaMask Android SDK (v0.5.1) |
| State | StateFlow, LiveData |
| Blockchain | Native `eth_sendTransaction` |

## Project Structure

```
app/src/main/java/com/example/chaintorquenative/
├── di/                    # Hilt dependency injection
├── mobile/
│   ├── data/
│   │   ├── api/           # API service & models
│   │   └── repository/    # Data repositories
│   └── ui/
│       └── viewmodel/     # ViewModels
├── ui/
│   ├── components/        # Reusable UI components
│   ├── screens/           # Compose screens
│   └── theme/             # App theming
├── wallet/                # MetaMask SDK integration
├── ChainTorqueApplication.kt
└── MainActivity.kt
```

## Setup

### Prerequisites
- Android Studio Ladybug or later
- JDK 17+
- Android SDK 33+

### Configuration
1. No API keys needed for MetaMask SDK default integration
2. Uses standard `io.metamask.androidsdk` dependency

### Build
```bash
./gradlew assembleDebug
```

## API Endpoints

The app connects to a backend server for:
- `GET /api/marketplace` - List NFT assets
- `GET /api/marketplace/:id` - Asset details
- `POST /api/user/register` - Register wallet
- `GET /api/user/:address/nfts` - User's NFTs

## Testing

1. Install **MetaMask** on your Android device/emulator
2. Create/Import a wallet and switch to **Sepolia Testnet**
3. Get test ETH from a [Sepolia faucet](https://sepoliafaucet.com/)
4. Open ChainTorque, go to **Wallet** tab, and tap "Connect with MetaMask"
5. Approve the connection in the MetaMask app
6. To buy: Select an item, tap "Buy Now", and sign the transaction in MetaMask

## License

MIT License

---

Built with ❤️ for the Web3 community
