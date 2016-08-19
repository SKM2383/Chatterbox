import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AppLauncher extends Application{
    private Stage appStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("view/startup.fxml"));
        primaryStage.setTitle("ChatterBox");
        primaryStage.setScene(new Scene(root, 853, 392));
        primaryStage.setResizable(false);
        primaryStage.show();

        appStage = primaryStage;
    }

    @Override
    public void stop(){
        // Because the user might close the window before hitting the disconnect button, the socket connection must be closed.
        // To do this jsut lookup the disconnect button using its css selector, then fire its event to stop the connection
        ((Button) appStage.getScene().lookup("#disconnectButton")).fire();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
