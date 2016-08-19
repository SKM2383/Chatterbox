package model.net.server;

import javafx.scene.paint.Color;
import model.ChatWriter;
import model.logging.ChatterLogger;
import model.net.utilities.ChatterExtractor;
import model.net.utilities.permissions.UserLevel;

import java.util.logging.Level;

public class ChatterCommandParser {

    // All commands in Chatterbox follow this basic syntax:
    //     senderUsername : command [parameters]
    //
    // Therefore all messages start with the sender username, so that can be extracted
    // Then we determine if the second string is a valid command and if so we use the parameters
    // to execute that command

    public static boolean parse(String message, ChatterBoxServer server){
        // Split the message by its spaces to get each individual string in the message
        String[] splitMessage = message.split(" ");

        // All command strings are composed of at least four Strings, so if it is less we know it isn't a command
        // Also because all commands start with '!', if the second String corresponding to the command doesn't start with it,
        // we know it isn't a command
        if(splitMessage.length < 4 || splitMessage[2].charAt(0) != '!')
            return false;


        // At this point we might have a possible command, so extract the parts of the message
        String sender = splitMessage[0];
        String possibleCommand = splitMessage[2];

        boolean commandFound = false;
        switch(possibleCommand){
            // !info informs the sender what the permission of a certain user is
            // Syntax: !info [usernameToCheck]
            case "!info":
                try{
                    // Get the name of the user to check
                    String userToQuery = splitMessage[3];

                    // find their permission
                    UserLevel userPermission = server.getUserPermission(userToQuery);

                    // If the sender was the server, then just write the results to the screen, otherwise send the result to the client sender
                    if(sender.equals(server.SERVER_USERNAME))
                        ChatWriter.showMessage("User <" + userToQuery + ">" + " has permission: " + userPermission, Color.SILVER);
                    else
                        server.notifyClient(sender, "#info User <" + userToQuery + ">" + " has permission: " + userPermission);

                    commandFound = true;
                }
                catch(IndexOutOfBoundsException ie){
                    ChatterLogger.log(Level.WARNING, "!info command didn't have required parameters");
                }
                finally {
                    break;
                }

            // !pmsg sends a private message from one user to another
            // Syntax: !pmsg [usernameToSendMessageTo] [message]
            case "!pmsg":
                try{
                    // Get the name of user to send to
                    String userToSendMessageTo = splitMessage[3];

                    // Concatenate the rest of the splitMessage into a single String to construct the private message
                    String privateMessage = ChatterExtractor.extract(splitMessage, 4, splitMessage.length, " ");

                    if(userToSendMessageTo.equals(server.SERVER_USERNAME))
                        ChatWriter.showMessage("<PMSG>" + sender + ": " + privateMessage, Color.BLACK);
                    else
                        server.notifyClient(userToSendMessageTo, "<PMSG>" + sender + " : " + privateMessage);

                    commandFound = true;
                }
                catch(IndexOutOfBoundsException iob){
                    ChatterLogger.log(Level.WARNING, "Parsed !pmsg command didn't have enough parameters");
                    server.notifyClient(sender, "<Error>");
                }
                finally {
                    break;
                }

            // !promote increases a users permission level to moderator, this can only be done by the admin
            // Syntax: !promote [userToPromote]
            case "!promote":
                // If the user isn't an admin then break from this case
                if(!server.getUserPermission(sender).equals(UserLevel.ADMIN))
                    break;

                String userToPromote = splitMessage[3];

                // Change their current permission level to admin
                server.promoteUser(userToPromote);

                commandFound = true;

                break;

            // !demote sets a users permission to that of a basic user
            // Syntax: !demote [userToDemote]
            case "!demote":
                // If the user isn't an admin then break from this case
                if(!server.getUserPermission(sender).equals(UserLevel.ADMIN))
                    break;

                String userToDemote = splitMessage[3];

                // Change their current permission level to basic
                server.demoteUser(userToDemote);

                commandFound = true;

                break;

            // !kick removes a user from the chat
            // Syntax: !kick [userToKick]
            case "!kick":
                try{
                    // If the user only has basic permissions, then break
                    if(server.getUserPermission(sender).equals(UserLevel.BASIC))
                        break;

                    // Get the user to kick
                    String userToKick = splitMessage[3];

                    server.kickUser(userToKick, sender);

                    commandFound = true;
                }
                catch(IndexOutOfBoundsException ib){
                    ChatterLogger.log(Level.INFO, "Parsed !kick command didn't have proper parameters");
                }
                finally{
                    break;
                }
        }

        return commandFound;
    }
}
