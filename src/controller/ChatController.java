package controller;


import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import model.ChatWriter;
import model.logging.ChatterLogger;
import model.net.ChatterBoxInstance;
import model.net.client.ChatterBoxClient;
import model.net.server.ChatterBoxServer;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

public class ChatController implements Initializable{
    @FXML
    private ToggleGroup connectionType;
    @FXML
    private RadioButton radioButtonServer;
    @FXML
    private RadioButton radioButtonClient;
    @FXML
    private TextField txtfldUsername;
    @FXML
    private TextField txtfldHostIp;
    @FXML
    private TextField txtfldUserMessage;
    @FXML
    private TextField txtfldPort;
    @FXML
    private PasswordField pwdfldPassword;
    @FXML
    private TextFlow txtflowChat;
    @FXML
    private Button btnConnect;
    @FXML
    private Button btnDisconnect;
    @FXML
    private Button btnSend;
    @FXML
    private Label lblHostIp;

    private Task<Void> chatTask; // Create a reference to an FX Task that holds our chat task

    // String to hold the username chosen by the user
    private String chosenUsername = "";

    // When this controller is initialized, set the ChatWriter properties to the correct values
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ChatWriter.setController(this); // Since this class is allowed to write on the TextFlow it must be passed to the ChatWriter
        ChatWriter.setFont(Font.font("Consolas", 15.0));
    }

    private boolean fieldIsEmpty(TextField fieldToCheck){
        boolean isEmpty = false;

        if(fieldToCheck == null || fieldToCheck.getText().trim().equals(""))
            isEmpty = true;

        return isEmpty;
    }

    @FXML
    public void changeControlStates(){
        String toggleText = ((RadioButton) connectionType.getSelectedToggle()).getText();
        if(toggleText.equals("Client")){
            txtfldHostIp.setDisable(false);
            lblHostIp.setDisable(false);
        }
        else{
            txtfldHostIp.setDisable(true);
            lblHostIp.setDisable(true);
        }
    }

    @FXML
    public void connect(){
        // Create variables that hold info to start a connection
        String username = "";
        int port;
        String roomPassword;
        String hostIP = "";

        // Check to make sure the username was filled out
        if(fieldIsEmpty(txtfldUsername)){
            ChatWriter.showErrorMessage("<ERROR> The username field must be filled out");
            return;
        }
        else{
            username = txtfldUsername.getText();
        }

        if(fieldIsEmpty(txtfldPort)){
            ChatWriter.showErrorMessage("<ERROR> The port field must be filled out");
            return;
        }
        else{
            port = Integer.parseInt(txtfldPort.getText());

            // Check that a correct port number is given
            if(port < 80 || port > 65535){
                ChatWriter.showErrorMessage("<ERROR> The port field must be filled out with a number from 80-65535");
                return;
            }
        }

        // Check to see if the room password was filled out, if not then just set the room password to empty string
        roomPassword = (fieldIsEmpty(pwdfldPassword)) ? "" : pwdfldPassword.getText();

        // Determine if the user wanted a client connection by checking the text of the selected connection type radio button
        boolean userWantsClientConnection = (((RadioButton) connectionType.getSelectedToggle()).getText()).equals("Client");

        // If the user selected the Client radio button make sure the host ip was filled out
        if(userWantsClientConnection){
            // If the hostIP was left blank then just set to null String, this represents the localhost address
            if(fieldIsEmpty(txtfldHostIp)){
                hostIP = "";
            }
            else
                hostIP = txtfldHostIp.getText();
        }

        // Disable the Connect button to stop the user from trying to start a connection multiple times
        btnConnect.setDisable(true);

        // Now determine if the user wanted to be a client or server and create a ChatterInstance accordingly
        try {
            if (userWantsClientConnection)
                chatTask = new ChatterBoxClient(username, hostIP, port, roomPassword);
            else
                chatTask = new ChatterBoxServer(username, port, roomPassword);

            // Start our background chat task
            Thread chatThread = new Thread(chatTask);
            //chatThread.setDaemon(true);
            chatThread.start();

            ChatterLogger.log(Level.INFO, "ChatterBoxMessenger thread started");

            // Enable the ui control fields that are used when a session is established
            btnDisconnect.setDisable(false);
            txtfldUserMessage.setDisable(false);
            btnSend.setDisable(false);

            // Set the global chosenUsername to the username
            chosenUsername = username;
        }
        catch(IOException ioe){
            ChatWriter.showErrorMessage("Network error occurred when trying to start connection");
            ChatterLogger.log(Level.SEVERE, "ChatterBoxInstance failed to start due to IOException");

            btnDisconnect.setDisable(true);
            txtfldUserMessage.setDisable(true);
            btnSend.setDisable(true);

            btnConnect.setDisable(false);
        }
    }

    @FXML
    public void disconnect(){
        // Log the disconnect start
        ChatterLogger.log(Level.INFO, "Disconnect starting");

        // Disable all the controls that are enabled during a connection
        btnDisconnect.setDisable(true);
        btnSend.setDisable(true);
        txtfldUserMessage.setDisable(true);

        // Re-Enable the connect button
        btnConnect.setDisable(false);

        chatTask.cancel(true);

        // Log the end of the disconnect
        ChatterLogger.log(Level.INFO, "Disconnect finished");
    }

    @FXML
    public void sendMessage(){
        // If the user choose to be a server, then the message needs to be appended with the the server username since
        // it is not inside the actual ChatterBoxServer class
        if(radioButtonServer.isSelected()){
            ((ChatterBoxInstance) chatTask).addMessage(chosenUsername + " : " + txtfldUserMessage.getText());
            txtfldUserMessage.clear();
        }
        // Otherwise as a client, when the message is sent to the server it will be parsed properly
        else{
            ((ChatterBoxInstance) chatTask).addMessage(txtfldUserMessage.getText());
            txtfldUserMessage.clear();
        }
    }

    // This method is used by other classes to display colored text on the TextFlow, it is needed because other threads
    // besides this FX thread aren't allowed to modify the controls
    public void showMessageInChat(Text message){
        Platform.runLater(() -> {
            txtflowChat.getChildren().add(message);
            txtflowChat.getChildren().add(new Text(System.lineSeparator()));
        });
    }
}
