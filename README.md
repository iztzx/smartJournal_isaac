# SmartJournal-FOP

**SmartJournal-FOP** is a modern, AI-powered personal journaling application built with JavaFX. It combines a beautiful, timeline-based interface with gamification elements and AI insights to make journaling engaging and meaningful.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-22-blue.svg)

## ✨ Key Features

-   **📅 Interactive Timeline**: A continuous, scrolling timeline view of your life. Click date bubbles to snap to entries.
-   **🤖 AI Weekly Summaries**: Generates weekly summaries of your entries using **Google Gemini Pro**, complete with emoji-rich insights.
-   **🧠 Mood & Weather Tracking**: Automatically tracks the weather and uses AI to analyze the sentiment (Mood) of your entries on a 5-point scale (Very Negative to Very Positive).
-   **🎮 Gamification**: Earn XP, level up, and maintain streaks by journaling daily. Unlock achievements as you progress.
-   **🔒 Secure Cloud Storage**: Your data is securely stored in a **Supabase (PostgreSQL)** database.
-   **🎨 Modern UI**: Features a polished, glassmorphism-inspired design with dark/light mode support (configurable via CSS).

## 🛠️ Tech Stack

-   **Language**: Java 21
-   **Framework**: JavaFX 22
-   **Build Tool**: Maven
-   **Database**: PostgreSQL (Supabase)
-   **AI APIs**: Google Gemini (Content Generation), Hugging Face (Sentiment Analysis)
-   **Libraries**: HikariCP (Pooling), Gson (JSON), SLF4J (Logging)

## 🚀 Getting Started

### Prerequisites

-   **JDK 21** or higher installed.
-   **Maven** installed and added to PATH.
-   **Supabase Account**: A PostgreSQL database project.
-   **API Keys**:
    -   Google Gemini API Key
    -   Hugging Face API Token

### Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/smartjournal-fop.git
    cd smartjournal-fop
    ```

2.  **Configure Environment**:
    Create a `.env` file in the project root (`c:\smartJournal_fop\.env`) and add your credentials:
    ```properties
    # .env
    GEMINI_API_KEY=your_gemini_api_key_here
    BEARER_TOKEN=your_hugging_face_token_here

    # Database Configuration (Supabase Connection Pooler Recommended for Firewall Issues)
    # Port 6543 is for the Session Pooler (Transaction mode)
    DB_URL=jdbc:postgresql://<your-project>.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
    DB_USER=postgres.<your-project-user>
    DB_PASSWORD=your_db_password
    ```

    > **Note:** If you are behind a restrictive firewall (like university WiFi), usage of the **Supabase Connection Pooler** (Session mode on port 5432 or 6543) is highly recommended. The `prepareThreshold=0` parameter is required for transaction pooling compatibility.

3.  **Build and Run**:
    ```bash
    mvn javafx:run
    ```

## 📖 Usage

-   **Add Entry**: Click the "**+**" button or "New Entry" to write about your day.
-   **View Timeline**: Scroll through your past entries. Click the date bubble to quick-scroll.
-   **Weekly Summary**: Click "Weekly Summary" to generate an AI report of your week.
-   **Gamification**: Check the left panel for your current Level, XP, and Streak.

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

## 📜 License

This project is open-source and available under the [MIT License](LICENSE).
