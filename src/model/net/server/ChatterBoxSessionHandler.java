package model.net.server;

import model.logging.ChatterLogger;
import model.net.ChatterBoxMessenger;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;

public class ChatterBoxSessionHandler implements Runnable{
    private final String CLIENT_USERNAME;
    private final ChatterBoxMessenger CLIENT_CONNECTION;
    private final ChatterBoxServer THIS_SERVER;

    public ChatterBoxSessionHandler(final String USERNAME, final ChatterBoxMessenger CONNECTION, final ChatterBoxServer THIS_SERVER){
        CLIENT_USERNAME = USERNAME;
        CLIENT_CONNECTION = CONNECTION;
        this.THIS_SERVER = THIS_SERVER;
    }

    @Override
    public void run(){
        boolean isClientUnavailable = false;

        // Continue as long as the server is still signalled to run and the client is available to communicate with
        while(THIS_SERVER.running && !isClientUnavailable){
            try{
                String clientMessage = CLIENT_CONNECTION.receiveMessage();
                String formattedMessage = CLIENT_USERNAME + " : " + clientMessage;

                THIS_SERVER.addMessage(formattedMessage);
            }
            catch(SocketException se){
                ChatterLogger.log(Level.WARNING, "SessionHandler with user <" + CLIENT_USERNAME + "> closed with SocketException");
                isClientUnavailable = true;
            }
            catch(IOException ioe){
                ChatterLogger.log(Level.WARNING, "Message from user <" + CLIENT_USERNAME + "> could not be received in session handler. Closing connection");
                // Remove the client from the server connections, if it returns true that means the client is no longer able to be reached
                THIS_SERVER.removeClient(CLIENT_USERNAME);
                isClientUnavailable = true;
            }
        }
    }
}
