# Spoonful - Recipe Sharing App

A modern Android recipe sharing and discovery application built with Jetpack Compose and Firebase. Spoonful allows users to create, share, and discover recipes with a focus on user engagement and community features.

## 🍳 Features

### Authentication & User Management
- **Firebase Authentication** with email/password
- User registration and login
- Profile management with user statistics
- Secure user data storage

### Recipe Management
- **Create & Upload Recipes**: Build detailed recipes with:
  - Multiple categories (up to 5 per recipe)
  - Ingredient lists with calorie information
  - Step-by-step cooking directions
  - Difficulty levels and cooking time
  - Recipe images (via Unsplash API)
- **Recipe Discovery**: Browse recipes by categories, search, and advanced filtering
- **Favorites System**: Save and manage your favorite recipes
- **Popular Recipes**: Algorithm-based recommendations based on community favorites

### Comprehensive Ingredient Database
- **334+ ingredients** with calorie information in multiple units:
  - Calories per gram
  - Calories per piece
  - Calories per tablespoon/teaspoon
  - Calories per ml/cup
- **48 predefined recipe categories** including:
  - Meal types: Breakfast, Lunch, Dinner, Dessert, Snack
  - Cuisines: Asian, Mexican, Italian, Mediterranean, Indian, etc.
  - Dietary preferences: Vegan, Vegetarian, Gluten-Free, Keto, Paleo
  - Cooking methods: Grill, BBQ, Baking, Slow Cooker

### Modern UI/UX
- **Material Design 3** with dynamic color schemes
- **Jetpack Compose** for modern, responsive UI
- **Edge-to-edge design** for immersive experience
- **Bottom navigation** with intuitive screen flow
- **Search & filtering** with real-time results

## 🛠️ Technical Stack

### Frontend
- **Jetpack Compose** - Modern Android UI toolkit
- **Material 3** - Latest Material Design components
- **Navigation Compose** - Type-safe navigation
- **Coil** - Image loading and caching
- **Accompanist** - Additional UI utilities

### Backend & Data
- **Firebase Realtime Database** - Real-time data synchronization
- **Firebase Authentication** - Secure user management
- **Unsplash API** - High-quality recipe images
- **Retrofit** - HTTP client for API calls

### Architecture
- **MVVM** (Model-View-ViewModel) pattern
- **Repository pattern** for data access
- **LiveData** for reactive UI updates
- **Coroutines** for asynchronous operations

## 📱 Screens

1. **Login/Register** - User authentication
2. **Home** - Recipe discovery with search and filters
3. **Recipe Detail** - Full recipe view with ingredients and directions
4. **Upload Recipe** - Recipe creation with ingredient picker
5. **Favorites** - Saved recipes management
6. **Profile** - User statistics and settings

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 35 (Android 15) or higher
- Java 11 or higher
- Google account for Firebase setup

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd AndroidSpoonful
   ```

2. **Firebase Setup**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication (Email/Password)
   - Enable Realtime Database
   - Download `google-services.json` and place it in the `app/` directory
   - Update database rules for your security requirements

   **⚠️ Important Security Note**: The `google-services.json` file contains sensitive Firebase configuration and is excluded from version control. Each developer must download their own copy from the Firebase Console.

3. **Unsplash API Setup** (Optional)
   - Create an account at [Unsplash Developers](https://unsplash.com/developers)
   - Get your API access key
   - Create `app/unsplash_key.properties`:
     ```properties
     UNSPLASH_ACCESS_KEY=your_unsplash_api_key_here
     ```

4. **Build and Run**
   ```bash
   ./gradlew build
   ```
   - Open the project in Android Studio
   - Connect an Android device or start an emulator
   - Click "Run" to install and launch the app

### Project Structure

```
app/src/main/java/com/MyApp/Spoonful/
├── MainActivity.kt              # Main activity with navigation
├── model/
│   └── Recipe.kt               # Recipe data model
├── viewmodel/
│   ├── AuthViewModel.kt        # Authentication logic
│   └── RecipeViewModel.kt      # Recipe management logic
├── repository/
│   ├── RecipeRepository.kt     # Data access layer
│   └── UnsplashApi.kt         # Image API integration
├── ui/
│   ├── screens/                # Main app screens
│   │   ├── HomeScreen.kt
│   │   ├── RecipeDetailScreen.kt
│   │   ├── UploadRecipeScreen.kt
│   │   ├── FavoritesScreen.kt
│   │   ├── ProfileScreen.kt
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   ├── components/             # Reusable UI components
│   └── theme/                  # App theming
└── util/                       # Utility classes
```

## 📊 Data Models

### Recipe Structure
```kotlin
data class Recipe(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val imageUrl: String = "",
    val authorId: String = "",
    val calories: Int = 0,
    val difficulty: Int = 1,
    val time: String = "",
    val favoriteCounter: Int = 0,
    val directions: List<String> = emptyList()
)
```

### Ingredient Information
```kotlin
data class IngredientInfo(
    val name: String,
    val caloriesPerG: Double,
    val caloriesPerPiece: Double,
    val caloriesPerTbsp: Double,
    val caloriesPerTsp: Double,
    val caloriesPerMl: Double,
    val caloriesPerCup: Double
)
```

## 🔧 Configuration

### 🔒 Required Configuration Files

**⚠️ Security Note**: These files contain sensitive data and are excluded from version control.

**Firebase Configuration**
- **File**: `app/google-services.json`
- **Purpose**: Firebase project settings and authentication
- **Setup**: Download from [Firebase Console](https://console.firebase.google.com/) → Project Settings → Your Apps
- **Place**: Put in `app/` directory

**Unsplash API (Optional)**
- **File**: `app/unsplash_key.properties`
- **Purpose**: Access to Unsplash API for recipe images
- **Setup**: Create account at [Unsplash Developers](https://unsplash.com/developers)
- **Place**: Put in `app/` directory
- **Format**:
  ```properties
  UNSPLASH_ACCESS_KEY=your_unsplash_api_key_here
  ```

**Team Development**: Each developer must download their own configuration files.

### Build Configuration
- **Minimum SDK**: 35 (Android 15)
- **Target SDK**: 36 (Android 16)
- **Kotlin**: Latest stable version
- **Java**: Version 11

### Dependencies
Key dependencies include:
- `androidx.compose.*` - Jetpack Compose UI components
- `com.google.firebase.*` - Firebase services
- `com.squareup.retrofit2` - HTTP client
- `io.coil-kt.compose` - Image loading
- `androidx.navigation.compose` - Navigation



**Spoonful** - Making recipe sharing deliciously simple! 🍽️ 
