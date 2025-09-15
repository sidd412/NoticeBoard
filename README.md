# Notice Board App

A digital notice board application designed for small schools, coaching centers, colleges, and other educational institutes to share official notices with their community.

## Features

### 🚀 Core Functionality
- **Splash Screen**: Welcome screen with app branding
- **Authentication**: Email/password login with skip option
- **Home Screen**: Displays subscribed notice boards with cards
- **Subscribe System**: Multiple ways to subscribe to notice boards:
  - QR Code scanning
  - WhatsApp number
  - Email address
  - Notice board code
- **Notice Viewer**: Carousel popup to view notices from subscribed boards
- **Profile Management**: User profile with login/signup options
- **Board Management**: Create and update notice boards for institutes

### 📱 User Experience
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Navigation**: Jetpack Navigation with NavHost
- **Responsive Design**: Optimized for different screen sizes
- **Intuitive Flow**: Easy-to-use interface following Material Design guidelines

### 🔧 Technical Features
- **Firebase Integration**: Authentication, Firestore database
- **MVVM Architecture**: ViewModels for state management
- **Coroutines**: Asynchronous operations
- **Material Design**: Consistent theming and components

## Architecture

### Tech Stack
- **UI**: Jetpack Compose
- **Navigation**: Jetpack Navigation Compose
- **Architecture**: MVVM with Repository pattern
- **Backend**: Firebase (Authentication, Firestore)
- **Async Operations**: Kotlin Coroutines

### Project Structure
```
app/src/main/java/com/notifiy/noticeboard/
├── data/
│   ├── model/           # Data models
│   └── repository/      # Firebase repository
├── navigation/           # Navigation setup
├── ui/
│   ├── screens/         # Compose screens
│   ├── theme/           # App theming
│   └── viewmodel/       # ViewModels
└── MainActivity.kt
```

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.8+
- Android SDK 24+
- Firebase project setup

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd NoticeBoard
   ```

2. **Firebase Setup**
   - Create a new Firebase project
   - Enable Authentication (Email/Password)
   - Enable Firestore Database
   - Download `google-services.json` and place it in `app/` directory

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

## Usage

### For Users
1. **Launch the app** - See splash screen
2. **Skip or Sign In** - Choose to skip or create an account
3. **Subscribe to Boards** - Use QR code, WhatsApp, email, or code
4. **View Notices** - Tap on subscribed boards to see notices
5. **Manage Profile** - Update your information

### For Institutes
1. **Sign In** - Create an account (required for institutes)
2. **Create Board** - Set up your notice board with institute details
3. **Share Subscription** - Share QR code, WhatsApp, email, or code with users
4. **Update Board** - Modify board information as needed
5. **Manage Notices** - Add and update notices (future feature)

## Data Models

### User
- Personal information (name, email, phone)
- Subscribed boards list
- Account creation/update timestamps

### NoticeBoard
- Organization details (name, code, email, location, WhatsApp)
- QR code and page URL
- Subscription and status information
- Creator information

### Notice
- Notice content (title, description, priority, category)
- Validity dates
- Attachments support
- Board association

## Future Enhancements

- [ ] Google Sign-In integration
- [ ] Push notifications
- [ ] QR code scanner implementation
- [ ] Notice creation and management
- [ ] File attachments for notices
- [ ] Subscription plans and payments
- [ ] Analytics and insights
- [ ] Multi-language support
- [ ] Offline support
- [ ] Admin dashboard

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please contact the development team or create an issue in the repository.
