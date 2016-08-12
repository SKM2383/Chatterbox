import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application{

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("view/startup.fxml"));
        primaryStage.setTitle("ChatterBox");
        primaryStage.setScene(new Scene(root, 853, 392));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
