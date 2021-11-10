import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client extends Thread {

    private Socket requestSocket;           //socket connect to the server
    //not needed
    private byte[] message;                //message send to the server
    private String MESSAGE;                //capitalized message read from the server
    private DataInputStream in;    //stream read from the socket
    private DataOutputStream out;    //stream write to the socket

    private int port = 8001;
    private String host;
    private int myId;

    //debug
    private String s;


    public Client(int myId, String host, int port) {
        this.myId = myId;
        this.host = host;
        this.port = port;
    }

    public void run() {
        try {
            //create a socket to connect to the server
            TimeUnit.SECONDS.sleep(1);
            System.out.println("CLIENT " + myId + ":attempt to connect to " + host + " on port " + port);
            requestSocket = new Socket(host, port);
            System.out.println("CLIENT " + myId + ":connected to " + host + " on port " + port);

            //initialize inputStream and outputStream
            out = new DataOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new DataInputStream(requestSocket.getInputStream());

            //preform handshake here to validate connection
            System.out.println("CLIENT " + myId + ": Creating and sending handshake to peer with ID: " + myId);
            message = MessageHandler.createHandshake(myId);
            MessageHandler.sendMessage(out, message);
            System.out.println("CLIENT " + myId + ": sent handshake to peer");


            //send bitfield
            //we could probably do this better.
            s = "CLIENT " + myId + ": creating and sending bitField message: ";
            for (byte b : MyProcess.bitField.toByteArray()) {
                s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
            }

            System.out.println(s);
            //System.out.println("CLIENT " + peerId + " bitfield len" + MyProcess.bitField.toByteArray().length + " DEBUG: " + s);

            message = MessageHandler.createMsg(5, MyProcess.bitField.toByteArray());
            MessageHandler.sendMessage(out, message);
            System.out.println("CLIENT " + myId + ": sent bitField message");

            //move these outside of the while loop so we don't reinitialize variables a bunch.
            byte[] msg;
            byte[] sizeB = new byte[4];
            int type = -1; // <- wut
            while (true) {
                //yeah this is copy and pasted code from server.java but I can't use a method because
                //passing an inputstream causes a nullpointer exception.
                if(in.available() > 0 ) {
                    in.read(sizeB);
                    System.out.println("size byte array is: " + sizeB.toString());
                    int size = ByteBuffer.wrap(sizeB).getInt();
                    msg = new byte[size];
                    type = in.read();
                    in.read(msg);
                    message = MessageHandler.handleMessage(msg, type);
                    MessageHandler.handleMessage(msg, type);
                }
            }
        } catch (ConnectException e) {
            //System.out.println(e.getLocalizedMessage());
            System.err.println("Connection refused. You need to initiate a server first.");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //Close connections
            try {
                if(in != null) {
                    in.close();
                }
                if(out != null){
                    out.close();
                }
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}