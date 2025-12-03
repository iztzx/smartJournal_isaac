import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SmartJournalGUI extends Application {

    private Stage primaryStage;
    private User currentUser = null;
    
    // Uses the UserManager, which is now connected to Supabase
    private final UserManager userManager = new UserManager();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Smart Journaling App - GUI"); 
        
        showLoginScene();
        
        primaryStage.show();
    }
    
    /**
     * Creates and displays the Login/Registration Scene.
     */
    public void showLoginScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // --- UI COMPONENTS ---
        Label title = new Label("Smart Journal Login/Register");
        title.setStyle("-fx-font-size: 18pt; -fx-font-weight: bold;");

        TextField emailField = new TextField();
        emailField.setPromptText("Email Address (e.g., s100201@student.fop)");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (e.g., pw-Stud#1)");
        
        TextField nameField = new TextField();
        nameField.setPromptText("Display Name");
        nameField.setVisible(false);
        nameField.setManaged(false);

        Button loginBtn = new Button("Login");
        Button toggleRegBtn = new Button("Need an account? Register");
        Button registerBtn = new Button("Register Account");
        registerBtn.setVisible(false);
        registerBtn.setManaged(false);
        
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: red;");

        // --- LAYOUT ---
        VBox titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER);
        grid.add(emailField, 0, 1, 2, 1);
        grid.add(passwordField, 0, 2, 2, 1);
        grid.add(nameField, 0, 3, 2, 1);
        grid.add(loginBtn, 0, 4);
        grid.add(registerBtn, 0, 4);
        grid.add(toggleRegBtn, 1, 4);
        grid.add(statusLabel, 0, 5, 2, 1);
        
        // --- EVENT HANDLERS ---
        toggleRegBtn.setOnAction(e -> {
            boolean isReg = !nameField.isVisible();
            nameField.setVisible(isReg);
            nameField.setManaged(isReg);
            loginBtn.setVisible(!isReg);
            loginBtn.setManaged(!isReg);
            registerBtn.setVisible(isReg);
            registerBtn.setManaged(isReg);
            toggleRegBtn.setText(isReg ? "Back to Login" : "Need an account? Register");
            primaryStage.sizeToScene(); 
        });

        loginBtn.setOnAction(e -> {
            boolean success = handleLogin(emailField.getText(), passwordField.getText());
            statusLabel.setText(success ? "" : "Login failed. Check credentials or console for DB error.");
        });

        registerBtn.setOnAction(e -> {
            boolean success = handleRegister(emailField.getText(), nameField.getText(), passwordField.getText());
            statusLabel.setText(success ? "Registration successful! You can now log in." : "Registration failed. Email might exist or DB failed.");
            if (success) toggleRegBtn.fire(); // Switch back to login view
        });

        VBox root = new VBox(10, titleBox, grid);
        root.setAlignment(Pos.CENTER);
        Scene scene = new Scene(root, 400, 350);
        primaryStage.setScene(scene);
    }
    
    public boolean handleLogin(String email, String password) {
        User user = userManager.login(email, password);
        if (user != null) {
            showMainScene(user);
            return true;
        }
        return false;
    }
    
    public boolean handleRegister(String email, String displayName, String password) {
        if (email.isEmpty() || displayName.isEmpty() || password.isEmpty()) {
            return false;
        }
        return userManager.register(email, displayName, password);
    }
    
    public void showMainScene(User user) {
        this.currentUser = user;
        Label welcome = new Label("Welcome, " + user.getDisplayName() + "! Supabase connection successful.");
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLoginScene());
        
        VBox mainLayout = new VBox(20, welcome, new Label("Journal and Summary Screens will be built here!"), logoutBtn);
        mainLayout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Smart Journaling - Main Menu");
    }
}