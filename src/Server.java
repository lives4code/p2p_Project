import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;

//handler thread class. handlers are spawned from the listening
//loop and are responsible for dealing with a single client's
//requests
public class Server extends Thread {

    private byte[] message;            //message received from the client
    private Socket connection;        //wait on a connection from client
    private int no;                //The index number of the client
    private DataInputStream in;	//stream read from the socket
    private DataOutputStream out;    //stream write to the socket
    private int peerId;

    private boolean valid;

    //debug
    private String s;

    public Server(Socket connection, int no, int peerId) {
        this.connection = connection;
        this.no = no;
        this.peerId = peerId;
    }

    public void run() {
        try {
            //initialize Input and Output streams
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            try {
                //preform handshake here to validate connection
                //preform handshake here to validate connection
                System.out.println("SERVER " + peerId + ": Creating and sending handshake to peer with ID: " + peerId);
                message = MessageHandler.createHandshake(peerId);
                MessageHandler.sendMessage(out, message);
                System.out.println("SERVER " + peerId + ": sent handshake to peer");

                //receive handshake and validate
                System.out.println("SERVER " + peerId + ": reading handshake from peer");
                MessageHandler.receiveHandshake(in, message);
                System.out.println("SERVER " + peerId + ": handshake read from peer");

                //validate handshake
                valid = MessageHandler.validateHandshake(message, peerId);
                System.out.println("SERVER " + peerId + ": validation result: " + valid);
                if (!valid){
                    //TODO deal with invalid handshake
                    currentThread().interrupt();
                }

                //send bitfield
                System.out.println("SERVER " + peerId + ": creating and sending bitField message");
                s = "";
                for (byte b : MyProcess.bitField.toByteArray()) {
                    s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
                }

                System.out.println("SERVER " + peerId + " bitfield len" + MyProcess.bitField.toByteArray().length+ " DEBUG: " + s);

                message = MessageHandler.createMsg(5, MyProcess.bitField.toByteArray());
                MessageHandler.sendMessage(out, message);
                System.out.println("SERVER " + peerId + ": sent bitField message");

                //receive bitfield
                message = MessageHandler.handleMessage(in);
                System.out.println("SERVER " + peerId + " bitfield msg DEBUG: ");
                s = "";
                for (byte b : message) {
                    s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
                }

                System.out.println("SERVER " + peerId + " bitfield msg DEBUG: " + s);

                while (true) {
                    //receive the message sent from the client
                    //in.read(message);
                    //debug message to user
                    //System.out.println("Receive message: " + message + " from client " + no);
                    //this is where we will handle the message
                    //handleMessage(message);
                    //not needed
//                    //test message
//                    sendMessage("received");
                }
            } catch (Exception exception) {
                System.err.println("Data received in unknown format");
            }
        } catch (IOException ioException) {
            System.out.println("Disconnect with Client " + no);
        } finally {
            //Close connections
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            }
        }
    }

    private void choke() {
        byte[] bytes = new byte[5];
        byte[] messageLength = ByteBuffer.allocate(4).putInt(0).array();
        for ( int i = 0; i < 4; i++){
            bytes[i] = messageLength[i];
        }
        bytes[4] = 0;
        MessageHandler.sendMessage(out, bytes);
    }

    //not used
//    //send a message to the output stream
//    private void sendMessage(String msg)
//    {
//        try{
//            //stream write the message
//            out.writeObject(msg);
//            out.flush();
//        }
//        catch(IOException ioException){
//            ioException.printStackTrace();
//        }
//    }
}
