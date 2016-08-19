package model.net.client;

import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import model.ChatWriter;
import model.logging.ChatterLogger;
import model.net.ChatterBoxInstance;
import model.net.ChatterBoxMessenger;
import model.net.exceptions.ChatterAuthenticationException;
import model.net.utilities.ChatterExtractor;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

public class ChatterBoxClient extends Task<Void> implements ChatterBoxInstance{
    private final String USERNAME;
    private final String ROOM_PASS;
    private final ChatterBoxMessenger MESSENGER;
    private ArrayBlockingQueue<String> userMessageQueue = new ArrayBlockingQueue<>(10);

    private volatile boolean running = false;

    public ChatterBoxClient(String username, String serverIp, int port, String roomPass) throws IOException{
        USERNAME = username;
        ROOM_PASS = roomPass;

        Socket conn = new Socket(serverIp, port);

        MESSENGER = new ChatterBoxMessenger(conn);
    }

    private boolean authenticate(final ChatterBoxMessenger CONNECTION, final String USERNAME, final String PASSWORD) throws IOException, ChatterAuthenticationException{
        // First send the room password to the server
        CONNECTION.sendMessage(PASSWORD);
        final String roomPassResponse = CONNECTION.receiveMessage();

        // If the response was an error message, write an error to the client chat window and return
        if(roomPassResponse.equals("<ERROR>IncorrectPassword")){
            throw new ChatterAuthenticationException("Incorrect room password");
        }

        // If the room pass response didn't end in an exception, then it holds the server username
        String serverUsername = roomPassResponse;

        // If an exception wasn't thrown then send our username
        CONNECTION.sendMessage(USERNAME);
        final String usernameResponse = CONNECTION.receiveMessage();

        if(usernameResponse.equals("<ERROR>UsernameTaken")){
            throw new ChatterAuthenticationException("Username already in use");
        }

        ChatWriter.showMessage("Connected to server <" + serverUsername + ">", Color.BLUE);
        ChatterLogger.log(Level.INFO, "Client connection authenticated with <"+ serverUsername +">");

        // If no exceptions were thrown, then return true
        return true;
    }

    @Override
    public void addMessage(String message) {
        userMessageQueue.add(message);
    }

    @Override
    public void close(){
        if(running)
            running = false;
    }

    @Override
    protected Void call() {
        running = true;

        ChatterLogger.log(Level.INFO, "Authenticating client connection");

        // Try to authenticate with the server, if an exception occurs, update the gui
        try{
            authenticate(MESSENGER, USERNAME, ROOM_PASS);
        }
        catch(IOException e){
            ChatWriter.showErrorMessage("Error occurred while authenticating with the server");
            return null;
        }
        catch(ChatterAuthenticationException ae){
            ChatWriter.showErrorMessage(ae.getMessage());
            return null;
        }

        // Create a thread to listen for new messages from the server
        Thread messageDispatcherThread = new Thread(new ClientMessageDispatcher());
        messageDispatcherThread.start();

        try{
            while(!isCancelled()){
                String serverMessage = MESSENGER.receiveMessage();

                String[] splitMessage = serverMessage.split(" ");
                String messageCommand = splitMessage[0];

                // Check to see if a server status string was sent
                switch(messageCommand){
                    case "#leave":
                        // If kicked, throw a RuntimeException and display its message to the user
                        throw new RuntimeException("You were kicked from the room");
                    case "#shutdown":
                        //The server was shutdown
                        throw new RuntimeException("Server was shutdown. Closing connection");
                    case "#info":
                        // Info about a user's permission, show in silver
                        String permissionMessage = ChatterExtractor.extract(splitMessage, 1, splitMessage.length, " ");
                        ChatWriter.showMessage(permissionMessage, Color.SILVER);
                        break;
                    case "#promotion":
                        // A user was promoted, show as blue text
                        String promotionMessage = ChatterExtractor.extract(splitMessage, 1, splitMessage.length, " ");
                        ChatWriter.showMessage(promotionMessage, Color.BLUE);
                        break;
                    case "#demotion":
                        // A user was demoted, show as orange text
                        String demotedMessage = ChatterExtractor.extract(splitMessage, 1, splitMessage.length, " ");
                        ChatWriter.showMessage(demotedMessage, Color.ORANGE);
                        break;
                    case "#joined":
                        // A user joined the room, show as green text
                        String newUserMessage = ChatterExtractor.extract(splitMessage, 1, splitMessage.length, " ");
                        ChatWriter.showMessage(newUserMessage, Color.GREEN);
                        break;
                    case "#kicked":
                        // A user was kicked, show in orange text
                        String kickedMessage = ChatterExtractor.extract(splitMessage, 1, splitMessage.length, " ");
                        ChatWriter.showMessage(kickedMessage, Color.RED);
                        break;
                    default:
                        // Otherwise just show a message with black text
                        ChatWriter.showMessage(serverMessage, Color.BLACK);
                }
            }
        }
        // If an exception occurs the server connection flag needs to be set to false to stop the mesage listener thread
        catch(RuntimeException re){
            ChatWriter.showErrorMessage(re.getMessage());
            ChatterLogger.log(Level.WARNING, "ChatterClient thread interrupted when taking new message from queue");

            try{
                MESSENGER.close();
            }
            catch(IOException ioe){
                ChatterLogger.log(Level.SEVERE, "Client messenger couldn't be closed");
            }
        }
        catch(IOException ioe){
            ChatWriter.showErrorMessage("Communication with the server has been lost. Closing our end.");
            ChatterLogger.log(Level.SEVERE, "    Connection with the server was lost");

            try{
                MESSENGER.close();
            }
            catch(IOException e){
                ChatterLogger.log(Level.SEVERE, "Client messenger couldn't be closed");
            }
        }
        finally{
            running = false;

            // In order to shut down, the message dispatcher thread must be stopped and all variables cleaned up
            if(messageDispatcherThread.isAlive()){
                messageDispatcherThread.interrupt();
            }

            try{
                MESSENGER.close();
            }
            catch(IOException e){
                ChatterLogger.log(Level.SEVERE, "Client connection didn't close on shutdown");
            }

            userMessageQueue.clear();
            userMessageQueue = null;

            return null;
        }
    }

    // This private inner class listens for new messages from the server, if these messages are a server command like
    // #kicked or #shutdown, the thread alerts the user and ends itself as well as the main ChatterClient thread
    // It is implemented as a private inner class to keep the message queue from being exposed
    private class ClientMessageDispatcher implements Runnable{
        @Override
        public void run(){
            try {
                while (running) {
                    // Until this thread is stopped, continue to take messages from the message queue and send them to the server
                    String nextMessage = userMessageQueue.take();

                    MESSENGER.sendMessage(nextMessage);
                }
            }
            catch(IOException e){
                ChatterLogger.log(Level.SEVERE, "ClientMessageDispatcher had IOException");
            }
            catch(InterruptedException ie){
                ChatterLogger.log(Level.SEVERE, "ClientMessageDispatcher interrupted in order to stop execution");
            }
        }
    }
}
