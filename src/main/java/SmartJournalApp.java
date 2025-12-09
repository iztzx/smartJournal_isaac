import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Optional;

public class SmartJournalApp extends Application {

    private SmartJournal smartJournal;
    private User currentUser;
    private BorderPane rootLayout;
    private Scene mainScene;

    // UI Components
    private ListView<SmartJournal.JournalEntry> timelineList;
    private VBox timelineContainer;
    private Button addEntryBtn;
    private Label greetingLabel, quoteLabel;

    @Override
    public void start(Stage primaryStage) {
        smartJournal = new SmartJournal();

        // 1. Show Login. If logic fails (user cancels), exit.
        if (!showLoginDialog()) {
            return;
        }

        // 2. Load Data (Now Async inside SmartJournal)
        smartJournal.setCurrentUser(currentUser);
        smartJournal.setOnLevelUp(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🎉 LEVEL UP!");
            alert.setHeaderText("Congratulations!");
            alert.setContentText("You reached Level " + smartJournal.getLevel() + "!");
            alert.show();
        });
        smartJournal.loadUserData();
        smartJournal.loadHistory();

        // 3. Build UI
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root-pane");

        HBox topBar = createTopBar(primaryStage);
        rootLayout.setTop(topBar);

        SplitPane mainContent = createMainContent();
        rootLayout.setCenter(mainContent);

        mainScene = new Scene(rootLayout, 1000, 700);
        loadCSS();

        if (currentUser == null) {
            // Should not happen if showLoginDialog checks are correct, but extra safety
            System.err.println("Fatal: User is null after login dialog. Exiting.");
            Platform.exit();
            return;
        }

        primaryStage.setTitle("SmartJournal - " + currentUser.getDisplayName());
        primaryStage.setScene(mainScene);
        primaryStage.show();

        updateDynamicUI();
    }

    private void loadCSS() {
        if (getClass().getResource("/journal_styles.css") != null) {
            mainScene.getStylesheets().setAll(getClass().getResource("/journal_styles.css").toExternalForm());
        } else {
            System.err.println("CSS NOT FOUND in /journal_styles.css. Checking local...");
            if (getClass().getResource("journal_styles.css") != null) {
                mainScene.getStylesheets().setAll(getClass().getResource("journal_styles.css").toExternalForm());
            }
        }
    }

    private void updateDynamicUI() {
        Platform.runLater(() -> {
            greetingLabel.setText(
                    LanguageManager.get(smartJournal.getGreeting()) + ", " + currentUser.getDisplayName() + "!");

            String mood = "Neutral";
            if (!smartJournal.getEntries().isEmpty()) {
                mood = smartJournal.getEntries().get(0).getAiMood();
            }
            // Translate the quote if possible
            String rawQuote = smartJournal.getDailyQuote(mood);
            quoteLabel.setText("\"" + LanguageManager.get(rawQuote) + "\"");

            SmartJournal.JournalEntry today = smartJournal.getTodayEntry();
            // Re-check logic: smartJournal.getTodayEntry() checks if today's date exists.
            if (today != null) {
                addEntryBtn.setText(LanguageManager.get("btn.editentry"));
                addEntryBtn.setOnAction(e -> openJournalEditor(today));
            } else {
                addEntryBtn.setText(LanguageManager.get("btn.newentry"));
                addEntryBtn.setOnAction(e -> openJournalEditor(null));
            }
        });
    }

    // --- NON-BLOCKING LOGIN DIALOG (TOGGLE) ---
    private boolean showLoginDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("login.title"));
        dialog.setHeaderText(null);

        StackPane container = new StackPane();
        container.setPadding(new Insets(20));

        // LOGIN VIEW
        VBox loginView = new VBox(15);
        TextField emailLogin = new TextField();
        emailLogin.setPromptText(LanguageManager.get("prompt.email"));
        PasswordField passLogin = new PasswordField();
        passLogin.setPromptText(LanguageManager.get("prompt.password"));
        Label loginError = new Label();
        loginError.setStyle("-fx-text-fill: red;");
        loginError.setWrapText(true);
        loginError.setPrefWidth(280); // Ensure it wraps within the dialog

        Button btnLogin = new Button(LanguageManager.get("btn.signin"));
        btnLogin.getStyleClass().add("primary-button");
        btnLogin.setMaxWidth(Double.MAX_VALUE);

        Hyperlink switchToReg = new Hyperlink(LanguageManager.get("login.create"));
        loginView.getChildren().addAll(new Label(LanguageManager.get("login.signin")), emailLogin, passLogin,
                loginError, btnLogin, switchToReg);

        // REGISTER VIEW
        VBox regView = new VBox(15);
        regView.setVisible(false);
        TextField emailReg = new TextField();
        emailReg.setPromptText(LanguageManager.get("prompt.email"));
        TextField nameReg = new TextField();
        nameReg.setPromptText(LanguageManager.get("prompt.displayname"));
        PasswordField passReg = new PasswordField();
        passReg.setPromptText(LanguageManager.get("prompt.password"));
        Label regError = new Label();
        regError.setStyle("-fx-text-fill: red;");
        regError.setWrapText(true);
        regError.setPrefWidth(280);

        Button btnReg = new Button(LanguageManager.get("login.register"));
        btnReg.getStyleClass().add("accent-button");
        btnReg.setMaxWidth(Double.MAX_VALUE);

        Hyperlink switchToLogin = new Hyperlink(LanguageManager.get("login.back"));
        regView.getChildren().addAll(new Label(LanguageManager.get("login.create")), emailReg, nameReg, passReg,
                regError, btnReg,
                switchToLogin);

        container.getChildren().addAll(loginView, regView);
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().setMinSize(400, 500); // Make the login page larger

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

        // EVENTS
        switchToReg.setOnAction(e -> {
            loginView.setVisible(false);
            regView.setVisible(true);
        });
        switchToLogin.setOnAction(e -> {
            regView.setVisible(false);
            loginView.setVisible(true);
        });

        btnLogin.setOnAction(e -> {
            String eStr = emailLogin.getText().trim();
            String pStr = passLogin.getText();
            if (eStr.isEmpty() || pStr.isEmpty()) {
                loginError.setText("Missing fields.");
                return;
            }

            loginView.setDisable(true);
            loginError.setText("Signing in...");
            new Thread(() -> {
                User user = new UserManager().login(eStr, pStr);
                Platform.runLater(() -> {
                    if (user != null) {
                        this.currentUser = user;
                        dialog.setResult(user);
                        dialog.close();
                    } else {
                        loginView.setDisable(false);
                        loginError.setText("Login failed. Check credentials/network.");
                    }
                });
            }).start();
        });

        btnReg.setOnAction(e -> {
            String eStr = emailReg.getText().trim();
            String nStr = nameReg.getText().trim();
            String pStr = passReg.getText();
            if (eStr.isEmpty() || pStr.isEmpty() || nStr.isEmpty()) {
                regError.setText("All fields required.");
                return;
            }

            regView.setDisable(true);
            regError.setText("Creating account...");
            new Thread(() -> {
                try {
                    new UserManager().register(eStr, nStr, pStr);
                    // If successful (no exception thrown), log in
                    Platform.runLater(() -> {
                        new Thread(() -> {
                            User user = new UserManager().login(eStr, pStr);
                            Platform.runLater(() -> {
                                if (user != null) {
                                    this.currentUser = user;
                                    dialog.setResult(user);
                                    dialog.close();
                                } else {
                                    regView.setDisable(false);
                                    regError.setText("Registration successful but auto-login failed.");
                                }
                            });
                        }).start();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        regView.setDisable(false);
                        regError.setText(ex.getMessage());
                    });
                }
            }).start();

        });

        Optional<User> result = dialog.showAndWait();
        if (result.isPresent())
            return true;

        Platform.exit();
        System.exit(0);
        return false;
    }

    private void performLogout(Stage stage) {
        currentUser = null;
        stage.close();
        Platform.runLater(() -> {
            try {
                new SmartJournalApp().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private HBox createTopBar(Stage stage) {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("menu-bar");

        MenuButton hamburger = new MenuButton("☰");
        hamburger.getStyleClass().add("hamburger-menu-button");

        MenuItem profileItem = new MenuItem(LanguageManager.get("menu.profile"));
        profileItem.setOnAction(e -> showProfileDialog());

        MenuItem settingsItem = new MenuItem(LanguageManager.get("menu.settings"));
        settingsItem.setOnAction(e -> showSettingsDialog(stage));

        MenuItem summaryItem = new MenuItem(LanguageManager.get("menu.summary"));
        summaryItem.setOnAction(e -> showSummaryDialog());

        MenuItem logoutItem = new MenuItem(LanguageManager.get("menu.logout"));
        logoutItem.setOnAction(e -> performLogout(stage));

        hamburger.getItems().addAll(profileItem, settingsItem, summaryItem, new SeparatorMenuItem(), logoutItem);

        Label title = new Label("SmartJournal");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        topBar.getChildren().addAll(hamburger, title);
        return topBar;
    }

    // --- DIALOGS must be defined before use or in class scope ---
    private void showProfileDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update your details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(currentUser.getDisplayName());
        PasswordField passField = new PasswordField();
        passField.setPromptText("New Password");

        grid.add(new Label("Display Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String newPass = passField.getText().isEmpty() ? currentUser.getPassword() : passField.getText();
                new UserManager().updateProfile(currentUser, nameField.getText(), newPass);
                return true;
            }
            return false;
        });

        dialog.showAndWait();
        updateDynamicUI();
    }

    private void showSettingsDialog(Stage stage) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("settings.title"));
        dialog.setHeaderText(LanguageManager.get("settings.header"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll("Light", "Dark");
        themeBox.setValue(rootLayout.getStyleClass().contains("dark-theme") ? "Dark" : "Light");

        themeBox.setOnAction(e -> {
            String selected = themeBox.getValue();
            if (selected.equals("Dark")) {
                if (smartJournal.isThemeUnlocked("Dark")) {
                    rootLayout.getStyleClass().add("dark-theme");
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reach Level 5 to unlock Dark Mode!");
                    alert.show();
                    themeBox.setValue("Light");
                }
            } else {
                rootLayout.getStyleClass().remove("dark-theme");
            }
        });

        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().addAll("English", "Bahasa Malaysia");
        langBox.setValue(LanguageManager.getCurrentLanguage());

        langBox.setOnAction(e -> {
            String selected = langBox.getValue();
            if (!selected.equals(LanguageManager.getCurrentLanguage())) {
                LanguageManager.setLanguage(selected);
                // Restart UI
                dialog.close();
                stage.close();
                Platform.runLater(() -> {
                    try {
                        new SmartJournalApp().start(new Stage());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        });

        grid.add(new Label(LanguageManager.get("settings.theme")), 0, 0);
        grid.add(themeBox, 1, 0);
        grid.add(new Label(LanguageManager.get("settings.language")), 0, 1);
        grid.add(langBox, 1, 1);

        TextArea rewardsInfo = new TextArea("Rewards:\nLvl 5: Dark Theme\nLvl 10: Nature Theme\nLvl 15: Ocean Theme");
        rewardsInfo.setEditable(false);
        rewardsInfo.setPrefHeight(100);
        grid.add(rewardsInfo, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void showSummaryDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("summary.title"));
        dialog.setHeaderText(LanguageManager.get("summary.header"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Increase Dialog Size
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().setPrefHeight(600);
        dialog.setResizable(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(650);

        if (smartJournal.getWeeklyStats().isEmpty()) {
            content.getChildren().add(new Label(LanguageManager.get("summary.noentries")));
        } else {
            TableView<SmartJournal.JournalEntry> table = new TableView<>();

            TableColumn<SmartJournal.JournalEntry, String> dateCol = new TableColumn<>(LanguageManager.get("col.date"));
            dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getDate().toString()));
            // Fixed width for date
            dateCol.setPrefWidth(120);

            TableColumn<SmartJournal.JournalEntry, String> moodCol = new TableColumn<>(LanguageManager.get("col.mood"));
            moodCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getAiMood()));
            moodCol.setPrefWidth(100);

            // Added Weather Column
            TableColumn<SmartJournal.JournalEntry, String> weatherCol = new TableColumn<>("Weather");
            weatherCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getWeather()));
            weatherCol.setPrefWidth(120);

            table.getColumns().add(dateCol);
            table.getColumns().add(moodCol);
            table.getColumns().add(weatherCol);
            table.setItems(smartJournal.getWeeklyStats());
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.setPrefHeight(200);

            // Row Factory for Color Coding
            table.setRowFactory(tv -> new TableRow<SmartJournal.JournalEntry>() {
                @Override
                protected void updateItem(SmartJournal.JournalEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("table-row-positive", "table-row-negative", "table-row-neutral");
                    if (item == null || empty) {
                        return;
                    }

                    String mood = item.getAiMood();
                    if ("Positive".equalsIgnoreCase(mood)) {
                        getStyleClass().add("table-row-positive");
                    } else if ("Negative".equalsIgnoreCase(mood)) {
                        getStyleClass().add("table-row-negative");
                    } else {
                        getStyleClass().add("table-row-neutral");
                    }
                }
            });

            Label assessmentLabel = new Label(LanguageManager.get("summary.assessment") + ":");
            assessmentLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0; -fx-font-size: 14px;");

            Label aiSummary = new Label("Generating AI Summary... (This may take a few seconds)");
            aiSummary.setWrapText(true);
            aiSummary.setStyle("-fx-font-style: italic; -fx-text-fill: gray; -fx-font-size: 13px;");

            // Allow summary to expand
            ScrollPane summaryScroll = new ScrollPane(aiSummary);
            summaryScroll.setFitToWidth(true);
            summaryScroll.setPrefHeight(300);
            summaryScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            content.getChildren().addAll(table, assessmentLabel, summaryScroll);

            // Fetch AI Summary asynchronously
            new Thread(() -> {
                boolean isEnglish = "English".equals(LanguageManager.getCurrentLanguage());
                String summary = SummaryGenerator.generate(smartJournal.getWeeklyStats(), isEnglish);
                Platform.runLater(() -> {
                    aiSummary.setText(summary);
                    aiSummary.setStyle("-fx-font-style: normal; -fx-text-fill: -color-text-primary;");
                });
            }).start();
        }

        if (getClass().getResource("/journal_styles.css") != null) {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/journal_styles.css").toExternalForm());
        }

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private SplitPane createMainContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.7);

        // LEFT: Timeline
        timelineContainer = new VBox(15);
        timelineContainer.getStyleClass().add("timeline-container");
        timelineContainer.setPadding(new Insets(20));

        greetingLabel = new Label(LanguageManager.get("timeline.welcome"));
        greetingLabel.getStyleClass().add("greeting-text");
        quoteLabel = new Label("Loading quote...");
        quoteLabel.getStyleClass().add("quote-text");

        VBox greetingBox = new VBox(5, greetingLabel, quoteLabel);

        addEntryBtn = new Button(LanguageManager.get("btn.newentry"));
        addEntryBtn.getStyleClass().add("primary-button");
        addEntryBtn.setOnAction(e -> openJournalEditor(null));

        HBox actionBox = new HBox(addEntryBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        timelineList = new ListView<>();
        timelineList.getStyleClass().add("timeline-list");
        timelineList.setItems(smartJournal.getEntries());
        timelineList.setCellFactory(param -> new TimelineCell(this));

        VBox.setVgrow(timelineList, Priority.ALWAYS);

        Label emptyState = new Label(LanguageManager.get("timeline.empty"));
        emptyState.getStyleClass().add("empty-state-text");

        StackPane listStack = new StackPane(timelineList, emptyState);
        emptyState.visibleProperty().bind(Bindings.isEmpty(smartJournal.getEntries()));
        timelineList.visibleProperty().bind(Bindings.isEmpty(smartJournal.getEntries()).not());

        timelineContainer.getChildren().addAll(greetingBox, actionBox, listStack);

        // RIGHT: Gamification (Now Returns Node for ScrollPane compatibility)
        Node statsPanel = createGamificationPanel();

        splitPane.getItems().addAll(timelineContainer, statsPanel);
        return splitPane;
    }

    private Node createGamificationPanel() {
        VBox vbox = new VBox(15);
        vbox.getStyleClass().add("gamification-content");
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.TOP_CENTER);

        // --- LEVEL & STREAK ---
        Label title = new Label(LanguageManager.get("gamification.progress"));
        title.getStyleClass().add("subheader-text");

        // NEW: Level Badge & Text side-by-side
        HBox levelBox = new HBox(15);
        levelBox.setAlignment(Pos.CENTER_LEFT);
        levelBox.getStyleClass().add("level-container");

        StackPane levelBadge = new StackPane();
        levelBadge.getStyleClass().add("level-badge");
        Label levelLabel = new Label();
        levelLabel.getStyleClass().add("level-number");
        levelLabel.textProperty().bind(smartJournal.levelProperty().asString());
        levelBadge.getChildren().add(levelLabel);

        VBox levelMeta = new VBox(0);
        levelMeta.setAlignment(Pos.CENTER_LEFT);
        Label lvlTitle = new Label(LanguageManager.get("gamification.level"));
        lvlTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-text-secondary;");
        Label streakLabel = new Label();
        streakLabel.getStyleClass().add("streak-label");
        streakLabel.textProperty()
                .bind(smartJournal.streakProperty().asString(LanguageManager.get("gamification.streak")));

        levelMeta.getChildren().addAll(lvlTitle, streakLabel);
        levelBox.getChildren().addAll(levelBadge, levelMeta);

        ProgressBar xpProgressBar = new ProgressBar(0);
        xpProgressBar.prefWidthProperty().bind(vbox.widthProperty());
        xpProgressBar.progressProperty().bind(smartJournal.xpProperty().divide(500.0));

        Label xpLabel = new Label();
        xpLabel.getStyleClass().add("xp-label");
        xpLabel.textProperty().bind(smartJournal.xpProperty().asString("%d / 500 XP"));

        // --- QUESTS SECTION ---
        Label questsTitle = new Label(LanguageManager.get("gamification.quests"));
        questsTitle.getStyleClass().add("section-title");

        VBox questsBox = new VBox(10);
        for (Quest q : GamificationManager.getDailyQuests(currentUser)) {
            VBox qCard = new VBox(5);
            qCard.getStyleClass().add("quest-card");
            Label qDesc = new Label(q.getDescription());
            qDesc.getStyleClass().add("quest-desc");
            Label qRew = new Label("+" + q.getXpReward() + " XP");
            qRew.getStyleClass().add("quest-reward");
            qCard.getChildren().addAll(qDesc, qRew);
            questsBox.getChildren().add(qCard);
        }

        // --- ACHIEVEMENTS SECTION ---
        Label achTitle = new Label(LanguageManager.get("gamification.achievements"));
        achTitle.getStyleClass().add("section-title");

        FlowPane achPane = new FlowPane();
        achPane.getStyleClass().add("achievement-grid");
        achPane.setPrefWrapLength(200);

        for (Achievement a : GamificationManager.getAchievements(currentUser)) {
            VBox aBadge = new VBox(5);
            aBadge.getStyleClass().add("achievement-badge");
            if (!a.isUnlocked())
                aBadge.getStyleClass().add("achievement-locked");

            Label icon = new Label(a.getIcon());
            icon.getStyleClass().add("achievement-icon");
            aBadge.getChildren().add(icon);

            // Translated Title & Description
            String tTitle = LanguageManager.get("ach." + a.getId() + ".title");
            String tDesc = LanguageManager.get("ach." + a.getId() + ".desc");
            if (tTitle.startsWith("ach."))
                tTitle = a.getTitle(); // Fallback
            if (tDesc.startsWith("ach."))
                tDesc = a.getDescription();

            Tooltip.install(aBadge, new Tooltip(tTitle + "\n" + tDesc));
            achPane.getChildren().add(aBadge);
        }

        vbox.getChildren().addAll(
                title, levelBox,
                new Separator(), new Label(LanguageManager.get("gamification.xp")), xpProgressBar, xpLabel,
                new Separator(), questsTitle, questsBox,
                new Separator(), achTitle, achPane);

        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("gamification-scroll");
        return scroll;
    }

    // --- EDITOR ---
    void openJournalEditor(SmartJournal.JournalEntry existingEntry) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(existingEntry == null ? LanguageManager.get("editor.new") : LanguageManager.get("editor.edit"));

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("editor-modal");

        Label prompt = new Label(existingEntry == null ? LanguageManager.get("editor.prompt.new")
                : LanguageManager.get("editor.prompt.edit"));
        prompt.getStyleClass().add("subheader-text");

        TextArea contentArea = new TextArea();
        contentArea.setWrapText(true);
        if (existingEntry != null)
            contentArea.setText(existingEntry.getContent());

        Button saveBtn = new Button(LanguageManager.get("btn.save"));
        saveBtn.getStyleClass().add("primary-button");
        Label statusLabel = new Label();

        saveBtn.setOnAction(e -> {
            String text = contentArea.getText();
            if (text.isEmpty())
                return;
            saveBtn.setDisable(true);
            statusLabel.setText("Processing...");

            LocalDate targetDate = (existingEntry != null) ? existingEntry.getDate() : LocalDate.now();

            new Thread(() -> {
                String weather;
                if (existingEntry != null) {
                    // Preserve existing weather data
                    weather = existingEntry.getWeather();
                } else {
                    // New entry: Fetch current weather
                    boolean isEnglish = "English".equals(LanguageManager.getCurrentLanguage());
                    weather = WeatherManager.getCurrentWeather(isEnglish);
                }
                smartJournal.processEntry(targetDate, text, weather);
                Platform.runLater(() -> {
                    modal.close();
                    updateDynamicUI();
                });
            }).start();
        });

        layout.getChildren().addAll(prompt, contentArea, statusLabel, saveBtn);
        Scene scene = new Scene(layout, 500, 400);
        if (getClass().getResource("/journal_styles.css") != null) {
            scene.getStylesheets().setAll(getClass().getResource("/journal_styles.css").toExternalForm());
        }
        if (rootLayout.getStyleClass().contains("dark-theme")) {
            layout.getStyleClass().add("dark-theme");
        }

        modal.setScene(scene);
        modal.showAndWait();
    }

    // --- TIMELINE CELL ---
    static class TimelineCell extends ListCell<SmartJournal.JournalEntry> {
        private final VBox container = new VBox(5);
        private final Label dateLabel = new Label();
        private final Label contentPreview = new Label();
        private final HBox tagsBox = new HBox(10);
        private final Label moodTag = new Label();
        private final Label weatherTag = new Label();
        private final Button editBtn = new Button("✎");
        private final BorderPane headerPane = new BorderPane();
        private final SmartJournalApp app;

        public TimelineCell(SmartJournalApp app) {
            this.app = app;
            container.getStyleClass().add("timeline-card");
            dateLabel.getStyleClass().add("card-date");
            contentPreview.getStyleClass().add("card-preview");
            contentPreview.setWrapText(true);

            editBtn.getStyleClass().add("edit-icon-button");
            editBtn.setVisible(false);

            headerPane.setLeft(dateLabel);
            headerPane.setRight(editBtn);

            tagsBox.getChildren().addAll(moodTag, weatherTag);
            container.getChildren().addAll(headerPane, contentPreview, tagsBox);

            container.setOnMouseEntered(e -> editBtn.setVisible(true));
            container.setOnMouseExited(e -> editBtn.setVisible(false));
        }

        @Override
        protected void updateItem(SmartJournal.JournalEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                getStyleClass().remove("today-card");
            } else {
                dateLabel.setText(item.getDate().format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")));

                String text = item.getContent().replace("\n", " ");
                if (text.length() > 100)
                    text = text.substring(0, 100) + "...";
                contentPreview.setText(text);

                moodTag.setText(item.getAiMood());
                weatherTag.setText("☁ " + item.getWeather());

                String moodClass = "tag-neutral";
                if ("Positive".equalsIgnoreCase(item.getAiMood()))
                    moodClass = "tag-positive";
                if ("Negative".equalsIgnoreCase(item.getAiMood()))
                    moodClass = "tag-negative";
                moodTag.getStyleClass().setAll("tag", moodClass);
                weatherTag.getStyleClass().setAll("tag", "tag-weather");

                editBtn.setOnAction(e -> app.openJournalEditor(item));

                if (item.getDate().equals(LocalDate.now())) {
                    if (!container.getStyleClass().contains("today-card")) {
                        container.getStyleClass().add("today-card");
                    }
                } else {
                    container.getStyleClass().remove("today-card");
                }

                setGraphic(container);
            }
        }
    }
}