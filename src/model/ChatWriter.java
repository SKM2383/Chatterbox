package model;

import controller.ChatController;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class ChatWriter {
    private static ChatController chatControllerClass;
    public static Font notificationFont = new Font("Consolas", 15.0);

    public static void setController(ChatController cont){ chatControllerClass = cont; }

    public static Font getFont(){ return notificationFont; }
    public static void setFont(Font newFont){ notificationFont = newFont; }

    public static void showErrorMessage(String text){
        Text errorMessage = new Text(text);
        errorMessage.setFont(notificationFont);
        errorMessage.setFill(Color.RED);

        chatControllerClass.showMessageInChat(errorMessage);
    }

    public static void showWarningMessage(String text){
        Text warningMessage = new Text(text);
        warningMessage.setFont(notificationFont);
        warningMessage.setFill(Color.ORANGE);

        chatControllerClass.showMessageInChat(warningMessage);
    }

    public static void showMessage(String text){
        Text message = new Text(text);
        message.setFont(notificationFont);
        message.setFill(Color.BROWN);

        chatControllerClass.showMessageInChat(message);
    }

    public static void showMessage(String text, Color textFill){
        Text message = new Text(text);
        message.setFont(notificationFont);
        message.setFill(textFill);

        chatControllerClass.showMessageInChat(message);
    }
}
