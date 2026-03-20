package querycraft;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import querycraft.ui.MainController;

/**
 * Main entry point for the QueryCraft application with Splash Screen.
 */
public class QueryCraftApp extends Application {

    private static final String APP_TITLE = "QueryCraft - Database Query Tool";
    private static final String APP_VERSION = "1.0.0";

    @Override
    public void start(Stage primaryStage) {
        showSplashScreen(primaryStage);
    }

    private void showSplashScreen(Stage splashStage) {
        // Setup Splash UI
        VBox splashRoot = new VBox();
        splashRoot.getStyleClass().add("splash-root");

        // Logo
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
            logoView.setImage(logo);
            logoView.setFitHeight(120);
            logoView.setFitWidth(120);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Could not load logo for splash: " + e.getMessage());
        }

        Label titleLabel = new Label("QueryCraft");
        titleLabel.getStyleClass().add("splash-label");

        Label statusLabel = new Label("Initializing...");
        statusLabel.getStyleClass().add("splash-status");

        ProgressBar progressBar = new ProgressBar(0);

        splashRoot.getChildren().addAll(logoView, titleLabel, statusLabel, progressBar);

        Scene splashScene = new Scene(splashRoot, 500, 350);
        // Apply CSS
        try {
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            splashScene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Could not load CSS for splash: " + e.getMessage());
        }

        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();

        // Background Task for loading
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Loading database drivers...");
                updateProgress(0.1, 1.0);
                Thread.sleep(100);

                updateMessage("Initializing JDBC services...");
                updateProgress(0.4, 1.0);
                Thread.sleep(100);

                updateMessage("Setting up UI components...");
                updateProgress(0.7, 1.0);
                Thread.sleep(100);

                updateMessage("Ready!");
                updateProgress(1.0, 1.0);
                Thread.sleep(50);

                return null;
            }
        };

        // Bind progress bar and status label
        statusLabel.textProperty().bind(loadTask.messageProperty());
        progressBar.progressProperty().bind(loadTask.progressProperty());

        // When loading is finished
        loadTask.setOnSucceeded(e -> {
            try {
                showMainStage();
                splashStage.close();
            } catch (Throwable ex) {
                ex.printStackTrace();
                showErrorDialog("Failed to initialize Main UI", ex);
            }
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            if (ex != null) ex.printStackTrace();
            showErrorDialog("Loading Process Failed", ex);
        });

        new Thread(loadTask).start();
    }

    private void showErrorDialog(String title, Throwable ex) {
        String message = (ex != null) ? ex.toString() : "Unknown error";
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText(title);
            alert.setContentText(message + "\n\nTry running the .bat file to check console for more details.");
            alert.showAndWait();
            System.exit(1);
        });
    }

    private void showMainStage() {
        Stage mainStage = new Stage();
        // Create main controller
        MainController mainController = new MainController();

        // Create scene
        Scene scene = new Scene(mainController, 1200, 800);

        // Load CSS stylesheet
        try {
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }

        // Configure stage
        mainStage.setTitle(APP_TITLE);
        
        // Set application icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/images/logo.png");
            if (iconStream != null) {
                mainStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        mainStage.setScene(scene);
        mainStage.setMinWidth(800);
        mainStage.setMinHeight(600);

        // Handle close request
        mainStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        mainStage.show();
    }

    @Override
    public void stop() {
        // Cleanup when application closes
        try {
            querycraft.service.DatabaseConnectionService.getInstance().disconnect();
            querycraft.service.QueryExecutorService.shutdown();
        } catch (Exception e) {
            // Ignore during shutdown
        }
    }

    public static void main(String[] args) {
        System.out.println("QueryCraft v" + APP_VERSION);
        launch(args);
    }
}
