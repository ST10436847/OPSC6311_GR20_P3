# MzansiBudget - Comprehensive Project Report

## 1. Purpose of the Application
**MzansiBudget** is a personal finance management application tailored for users who want to take control of their spending habits. In the current economic climate, tracking every cent is vital. This app provides a platform to:
*   Log daily expenses with detailed descriptions and categories.
*   Capture and store digital copies of receipts for record-keeping.
*   Set and monitor monthly minimum and maximum spending goals.
*   Visualize financial health through interactive charts and progress tracking.
*   Stay motivated using gamification elements like points and achievement badges.

---
## OneDrive Link:
 https://1drv.ms/v/c/6b92486a3850091b/IQDHDnPUV23uQ65ULc09gghXAd8DfE3FrgbcO88OcgCZoyc?e=DIy9VY

## 2. Design Considerations
The application was designed with a focus on **Usability**, **Performance**, and **Visual Clarity**:

### UI/UX Design
*   **Material Design 3**: The app utilizes Google's latest design system to ensure a modern look and feel.
*   **Intuitive Navigation**: A Bottom Navigation Bar allows users to switch between the Dashboard, Expense Entry, Category Management, and Reports seamlessly.
*   **Color Theory**: Used a "Financial Green" palette (#4CAF50) to represent growth and money, paired with contrasting error/success colors for budget status indicators.

### Technical Architecture
*   **Local Persistence**: Built on **Room Database**, ensuring all user data is stored locally on the device. This allows the app to function perfectly offline.
*   **Asynchronous Processing**: Utilizes **Kotlin Coroutines** and `lifecycleScope` to perform database operations without freezing the user interface.
*   **Data Visualization**: Integrated the **MPAndroidChart** library to transform raw expense data into meaningful bar charts for category-wise analysis.

### Visual Assets
*   **Custom Iconography**: I designed a professional currency-themed logo (`ic_logo.xml`) used on the login screen and as the application's launcher icon to give the app a unique identity.
*   **Digital Receipts**: The design includes a specific flow for processing high-resolution images into compressed ByteArrays for efficient database storage.

---

## 3. GitHub and GitHub Actions
### Version Control Utilization
*   **Collaboration**: Git was used to manage the development process between partners. We used descriptive commit messages to track the evolution of features.
*   **Branching Strategy**: Used for isolated development of specific features (like the Reports graph vs. Expense deletion) before merging into the main codebase.

### GitHub Actions (CI/CD)
The project utilizes GitHub Actions to automate the development workflow:
*   **Continuous Integration**: A `.yml` workflow is configured to automatically trigger on every `push` or `pull_request`.
*   **Automated Builds**: The action runs `./gradlew assembleDebug` to ensure that any new code changes do not break the project's build.
*   **Quality Gate**: By automating the build process, we ensure that only functional, compilable code exists in the main repository.

---

## 4. Feature Breakdown

### Partner Features
*   **Graph Visualization**: Interactive Bar charts showing spending distribution.
*   **Budget Goal Tracking**: Dynamic progress bars and status updates based on user-defined limits.
*   **Gamification**: A reward system where users earn **Points** and **Badges** (e.g., "Consistent Logger") for maintaining their financial records.

### My Added Features (Individual Tasks)
1.  **Expense Deletion Management**: Implemented a long-press interaction on the RecyclerView items. This allows users to manage their history by deleting incorrect entries with a confirmation dialog.
2.  **Search and Filter Capability**: Added a real-time search engine in the Reports section. Users can filter hundreds of transactions by description or category name instantly.
3.  **Visual Branding & Assets**: Designed and implemented the final app icon, the adaptive launcher icon set, and the polished Login UI using the new custom logo assets.

---

## 5. Visual Samples & Assets
Below are the key visual assets used in the application:

### App Logo
The custom-designed logo representing financial stability and the "Mzansi" identity.
![App Logo](app/src/main/res/drawable/ic_logo.xml)

### Launcher Icon (Adaptive)
The adaptive icon used for the Android home screen.
![Launcher Icon](app/src/main/res/mipmap-xhdpi/ic_launcher.webp)

### Feature Highlight: Digital Receipts
The app supports capturing photos of receipts to verify expenses.
![Receipt Sample](app/src/main/res/drawable/watermarked_e4fd9e57_bf48_4815_961c_1a36251620bc.jpg)

### UI Layouts
*   **Dashboard**: `app/src/main/res/layout/fragment_dashboard.xml`
*   **Reports & Search**: `app/src/main/res/layout/fragment_reports.xml`

