import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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
    private int connectedToID;

    //debug
    private String s;

    public Client(int myId, String host, int port, int connectedToID) {
        this.myId = myId;
        this.host = host;
        this.port = port;
        this.connectedToID = connectedToID;
    }

    public void run() {
        try {
            //create a socket to connect to the server
            TimeUnit.SECONDS.sleep(1);
            System.out.println("CLIENT " + myId + ": attempt to connect to " + host + " on port " + port);
            requestSocket = new Socket(host, port);
            System.out.println("CLIENT " + myId + ": connected to " + host + " on port " + port);

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
            boolean bitfieldSent = false;
            if(!bitfieldSent) {
                bitfieldSent = true;
                s = "CLIENT " + myId + ": creating and sending bitField message: ";
                for (byte b : MyProcess.bitField.toByteArray()) {
                    s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + ", ";
                }
                System.out.println(s);
                message = MessageHandler.createMsg(5, MyProcess.bitField.toByteArray());
                MessageHandler.sendMessage(out, message);
                System.out.println("CLIENT " + myId + ": sent bitField message");

            }

            //move these outside of the while loop so we don't reinitialize variables a bunch.
            byte[] msg;
            byte[] sizeB = new byte[4];
            int type = -1; // <- wut
            int peerIndex = MyProcess.getPeerIndexById(connectedToID);
            long cost;
            long start;

            while (true) {
                //check if it needs to send a choke or unchoke message.
                if(MyProcess.peers.get(peerIndex).getChangeChoke() == true){
                    if(MyProcess.peers.get(peerIndex).getIsChoked()){
                        message = MessageHandler.createunChokeMessage();
                    }
                    else {
                        message = MessageHandler.createChokeMessage();
                    }
                    MessageHandler.sendMessage(out, message);
                    MyProcess.peers.get(peerIndex).setChangeChoke(false);
                }

                if(in.available() > 0 ) {
                    System.out.println("CLIENT " + myId + ": beginning new loop iteration");
                    start = System.currentTimeMillis();
                    in.read(sizeB);
                    int size = ByteBuffer.wrap(sizeB).getInt();
                    msg = new byte[size];
                    type = in.read();
                    in.read(msg);
                    cost = System.currentTimeMillis() - start;
                    // TODO Client should not be getting type 7; server should be; why is this happening?
                    if (type == 7) { // Only record download rate if receiving a piece
                        MyProcess.peers.get(MyProcess.getPeerIndexById(connectedToID)).downloadRate = cost != 0 ? size / cost : size / 0.0000001; // bytes per ms
                        System.out.println("CLIENT " + myId + ": new rate: " + MyProcess.peers.get(MyProcess.getPeerIndexById(connectedToID)).downloadRate);
                    }
                    message = MessageHandler.handleMessage(msg, type, connectedToID, myId, 'C');
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