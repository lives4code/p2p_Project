import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class Client extends Thread {

    private Socket requestSocket;           //socket connect to the server
    //not needed
    private byte[] message;                //message send to the server
    private String MESSAGE;                //capitalized message read from the server
    private DataInputStream in;	//stream read from the socket
    private DataOutputStream out;    //stream write to the socket

    private int port = 8001;
    private String host;
    private int peerId;

    //debug
    private boolean valid;
    private String s;


    public Client(int peerId, String host, int port) {
        this.peerId = peerId;
        this.host = host;
        this.port = port;
    }

    public void run()
    {
        try{
            //create a socket to connect to the server
            System.out.println("CLIENT " + peerId + ":attempt to connect to " + host + " on port " + port);
            requestSocket = new Socket(host, port);
            System.out.println("CLIENT " + peerId + ":connected to " + host + " on port " + port);

            //initialize inputStream and outputStream
            out = new DataOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new DataInputStream(requestSocket.getInputStream());

            //preform handshake here to validate connection
            //preform handshake here to validate connection
            System.out.println("CLIENT " + peerId + ": Creating and sending handshake to peer with ID: " + peerId);
            message = MessageHandler.createHandshake(peerId);
            MessageHandler.sendMessage(out, message);
            System.out.println("CLIENT " + peerId + ": sent handshake to peer");

            //receive handshake and validate
            System.out.println("CLIENT " + peerId + ": reading handshake from peer");
            MessageHandler.receiveHandshake(in, message);
            System.out.println("CLIENT " + peerId + ": handshake read from peer");

            //validate handshake
            valid = MessageHandler.validateHandshake(message, peerId);
            System.out.println("CLIENT " + peerId + ": validation result: " + valid);
            if (!valid){
                //deal with invalid handshake
            }

            //send bitfield
            System.out.println("CLIENT " + peerId + ": creating and sending bitField message");
            s = "";
            for (byte b : MyProcess.bitField.toByteArray()) {
                s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
            }

            System.out.println("CLIENT " + peerId + " bitfield len" + MyProcess.bitField.toByteArray().length+ " DEBUG: " + s);

            message = MessageHandler.createMsg(5, MyProcess.bitField.toByteArray());
            MessageHandler.sendMessage(out, message);
            System.out.println("CLIENT " + peerId + ": sent bitField message");

            //receive bitfield
            message = MessageHandler.handleMessage(in);
            System.out.println("CLIENT " + peerId + " bitfield msg DEBUG: ");
            s = "";
            for (byte b : message) {
                s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
            }
            System.out.println("CLIENT " + peerId + " bitfield msg DEBUG: " + s);

            //get Input from standard input
            //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                //not needed
//                System.out.print("Hello, please input a sentence: ");
//                //read a sentence from the standard input
//                message = bufferedReader.readLine();
//                //Send the sentence to the server
//                sendMessage(message);
//                //Receive the upperCase sentence from the server
//                MESSAGE = (String)in.readObject();
//                //show the message to the user
//                System.out.println("Receive message: " + MESSAGE);
            }
        }
        //not needed
//        catch ( ClassNotFoundException e ) {
//            System.err.println("Class not found");
//        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    //not needed
    //send a message to the output stream
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