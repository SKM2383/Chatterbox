package model.net.server;

import javafx.concurrent.Task;
import model.ChatWriter;
import model.logging.ChatterLogger;
import model.net.ChatterBoxInstance;
import model.net.ChatterBoxMessenger;
import model.net.utilities.permissions.UserLevel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ChatterBoxServer extends Task<Void> implements ChatterBoxInstance{
    //HashMap to hold client usernames and their connections for communications
    private ConcurrentHashMap<String,ChatterBoxMessenger> clientConnections = new ConcurrentHashMap<>(6);
    //This HashMap holds the permission level of each person in the room
    private ConcurrentHashMap<String,UserLevel> userPermissions = new ConcurrentHashMap<>(6);
    // This queue holds the messages yet to be processed
    private ArrayBlockingQueue<String> messageQueue = new ArrayBlockingQueue<>(10);

    private String latestUserToJoin = ""; // Holds the last username to join the room, used when adding a new client
    public volatile boolean running = false;

    private final String ROOM_PASS;
    private final ServerSocket SERVER_SOCKET;
    public final String SERVER_USERNAME;

    public ChatterBoxServer(String username, int port, String password) throws IOException {
        SERVER_SOCKET = new ServerSocket(port);
        SERVER_USERNAME = username;
        ROOM_PASS = password;

        userPermissions.put(SERVER_USERNAME, UserLevel.ADMIN);
    }

    // Since the server isn't an entry in the client connections it doesn't have its name appended at the beginning of
    // its messages, so this method is called to do that
    @Override
    public void addMessage(String message) {
        messageQueue.add(SERVER_USERNAME + " : " + message);
    }

    // This method adds client messages that have already been formatted with the clients username
    public void addClientMessage(String message){
        messageQueue.add(message);
    }

    public int getPort(){
        return SERVER_SOCKET.getLocalPort();
    }

    protected synchronized boolean addClient(ChatterBoxMessenger clientConnection){
        boolean userAddedSuccessfully = false;

        // Check the initial room password the client sends
        //    IF NOT CORRECT:
        //       1) Send an error message to the client
        //       2) Close the SocketMessenger
        //       3) Log the failed attempt
        //    IF CORRECT:
        //       1) Send the server username
        //       2) Check to make sure the client username isn't already being used
        //          IF IT IS BEING USED:
        //             3) Send the used username error message
        //             4) Log the failed attempt
        //          IF NOT BEING USED:
        //             3) Add the user to the client connections and the permission list with basic permissions
        //             4) Set the userAdded flag to true
        //             5) Tell the room the user joined
        try{
            // Get the attempted password
            String clientRoomPass = clientConnection.receiveMessage();

            // If the client password isn't the room pass, send an error message, otherwise check their username
            if(!clientRoomPass.equals(ROOM_PASS)){
                clientConnection.sendMessage("<ERROR>IncorrectPassword");
                clientConnection.close();

                ChatterLogger.log(Level.INFO, "User failed to join due to incorrect room password: " + clientRoomPass);
            }
            else {
                clientConnection.sendMessage(SERVER_USERNAME);

                String clientUsername = clientConnection.receiveMessage();

                // Make sure the username isn't the server's username or the username of a user already in the chat
                if (clientConnections.contains(clientUsername) || clientUsername.equals(SERVER_USERNAME)) {
                    clientConnection.sendMessage("<ERROR>UsernameTaken");

                    ChatterLogger.log(Level.INFO, "User <" + clientUsername + "> had duplicate username, was not allowed");
                } else {
                    // If the room password and client username checked out they are added
                    clientConnections.put(clientUsername, clientConnection);
                    userPermissions.put(clientUsername, UserLevel.BASIC);
                    userAddedSuccessfully = true;

                    // Set the latest user to join to this client username, by doing this any method can retrieve the new client
                    // username while still allowing this method to return a boolean
                    latestUserToJoin = clientUsername;

                    String userJoinedMessage = "User <" + clientUsername + "> joined room";

                    messageQueue.add(userJoinedMessage);
                    ChatterLogger.log(Level.INFO, userJoinedMessage);
                }
            }
        }
        catch(IOException e){
            ChatterLogger.log(Level.WARNING, "Failed to notify client during authentication");
        }
        finally {
            return userAddedSuccessfully;
        }
    }

    protected synchronized boolean removeClient(String username){
        boolean wasClientRemoved = false;

        try{
            if(clientConnections.containsKey(username)){
                clientConnections.get(username).sendMessage("#kicked");
                clientConnections.get(username).close();

                clientConnections.remove(username);
                userPermissions.remove(username);

                wasClientRemoved = true;
            }
        }
        catch(IOException ioe){
            ChatterLogger.log(Level.WARNING, "User <" + username + "> could not be removed");
        }
        finally {
            return wasClientRemoved;
        }
    }

    protected synchronized boolean notifyClient(String clientUsername, String message){
        boolean wasClientNotified = false;

        try{
            if(clientConnections.containsKey(clientUsername)){
                clientConnections.get(clientUsername).sendMessage(message);
                wasClientNotified = true;
            }
        }
        catch(IOException ioe){
            ChatterLogger.log(Level.WARNING, "User <" + clientUsername + "> could not be reached when notified. Connection closed");

            try{
                clientConnections.get(clientUsername).close();
                clientConnections.remove(clientUsername);
                userPermissions.remove(clientUsername);
            }
            catch(IOException e){
                ChatterLogger.log(Level.WARNING, "Connection to user <" + clientUsername + "> could not be closed");
            }
        }
        finally {
            return wasClientNotified;
        }
    }

    protected synchronized void notifyAllClients(String message){
        for(String clientUsername : clientConnections.keySet()){
            notifyClient(clientUsername, message);
        }
    }

    public UserLevel getUserPermission(String username){
        return userPermissions.get(username);
    }

    protected void setUserPermission(String username, UserLevel newPermission){
        userPermissions.replace(username, newPermission);
    }

    public boolean isAlive(){ return running; }

    @Override
    public void close(){
        // Set the flag to stop threads
        if(running){
            running = false;
        }

        // In addition, the SERVER_SOCKET may block this thread in the run() method with its accept() method, therefore
        // it must be closed, causing a SocketException to occur in run()
        try {
            SERVER_SOCKET.close();
        }
        catch(IOException e){
            ChatterLogger.log(Level.SEVERE, "Server socket not closed completely when outer thread called close()");
        }
    }

    private void shutdown(){
        // When this server is signalled to shut down:
        //   1) Null out the message queue
        //   2) Informs all clients the server is shutting down
        //   3) Close all streams to clients
        //   2) Close the server socket
        //   3) Notify outside threads (ChatController) waiting for the server to shutdown it has done so
        try{
            messageQueue.clear();

            for(ChatterBoxMessenger clientConnection : clientConnections.values()){
                clientConnection.sendMessage("#shutdown");
                clientConnection.close();
            }

            clientConnections.clear();
        }
        catch(IOException ioe){
            ChatterLogger.log(Level.SEVERE, "Server failed to shut down when notified");
        }
    }

    @Override
    protected Void call() {
        running = true;

        // Show message to inform user that server is online
        ChatWriter.showMessage("This server <" + SERVER_USERNAME + "> is online");

        // Start up the private inner class thread to handle incoming client connections
        Thread messageDispatcherThread = new Thread(new ServerConnectionHandler());
        messageDispatcherThread.start();

        // Check to see if the Main Application thread cancelled this task
        while(!isCancelled()){
            // Server loop to accept new client connections and then authenticate them
            try{
                String newMessage = messageQueue.take();

                // Make sure the new message isn't a server command like !disconnect to prevent clients from
                // kicking other clients
                if(newMessage.equals("#kicked") || newMessage.equals("#shutdown"))
                    continue;

                //If the CommandParser doesn't find a command, then its safe to write the message to chat
                if(ChatterCommandParser.parse(newMessage, this) == false){
                    // Update the message on the Main Application Thread, this allows the TextFlow to be updated
                    ChatWriter.showMessage(newMessage);

                    notifyAllClients(newMessage);
                }
            }
            catch(InterruptedException ie){
                // Make sure this exception was caused by a cancellation from the Main Application Thread
                if(isCancelled())
                    ChatterLogger.log(Level.WARNING, "ChatterServer interrupted from MAT");
                else
                    ChatterLogger.log(Level.WARNING, "ChatterServer interrupted from unknown source");

                break;
            }
        }

        // Notify all clients of the shutdown
        notifyAllClients("#shutdown");

        // Proceed to clean up server variables
        shutdown();

        // If the message dispatcher thread is still alive, it is most likely being blocked by the messageQueue, so interrupt
        // it. This will cause ServerMessageDispatcher to handle an InterruptedException that will cause it to shutdown
        if(messageDispatcherThread.isAlive())
            messageDispatcherThread.interrupt();

        ChatterLogger.log(Level.INFO, "ChatterServer shut down");

        return null;
    }

    // Create a private class that continually sends new messages to all clients, it is a private inner
    // class to prevent the final server variables such as the message queue from having to be accessed
    // outside of the class
    private class ServerConnectionHandler implements Runnable{
        public void run(){
            while(running){
                try {
                    Socket clientConnection = SERVER_SOCKET.accept();
                    clientConnection.setSoTimeout(300000); // 5 minute timeout

                    //Extract the streams from this Socket
                    ChatterBoxMessenger clientMessenger = new ChatterBoxMessenger(clientConnection);

                    // If the new client was added successfully, create a new session handler to handle incoming messages
                    if(addClient(clientMessenger)) {
                        Thread clientSessionThread = new Thread(new ChatterBoxSessionHandler(latestUserToJoin, clientMessenger, ChatterBoxServer.this));
                        clientSessionThread.start();
                    }
                }
                catch(SocketException se){
                    ChatterLogger.log(Level.WARNING, "ServerSessionHandler shutdown with SocketException");
                    break;
                }
                catch(IOException e){
                    ChatterLogger.log(Level.WARNING, "ServerSessionHandler encountered an IOException");
                }
            }
        }
    }
}
