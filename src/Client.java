import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Client extends Thread {

    private Socket requestSocket;           //socket connect to the server
    //not needed
    private byte[] message;                //message send to the server
    private String MESSAGE;                //capitalized message read from the server
    private DataInputStream in;    //stream read from the socket
    private DataOutputStream out;    //stream write to the socket
    private BitSet clientBitfield;

    private int port = 8001;
    private String host;
    private int myId;
    private int connectedToID;
    private int[] haveMessages = null;

    //debug
    private String s;
    Logger log;

    public Client(int myId, String host, int port, int connectedToID, Logger log) {
        this.myId = myId;
        this.host = host;
        this.port = port;
        this.connectedToID = connectedToID;
        this.clientBitfield = (BitSet)MyProcess.bitField.clone();
        this.log = log;
    }

    private static int[] getDifference(BitSet myProcess, BitSet client){
        int size = 0;
        if(myProcess != null && myProcess.size() !=0 && client != null){
            for(int i = 0; i < myProcess.size(); i++){
                if(myProcess.get(i) != client.get(i)){
                    size++;
                }
            }
            if(size != 0) {
                System.out.println("get difference size is" + size);
            }
            int[] ret = new int[size];
            int index = 0;
            for (int i = 0; i < myProcess.size(); i++){
                if(myProcess.get(i) != client.get(i)){
                    System.out.println("adding at index" + index);
                    ret[index] = i;
                    index++;
                }
            }
        }
        return null;
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
                //System.out.println("CLIENT CHOKER " + myId + ": connected to, index: " + connectedToID + ", " + peerIndex + "| is get change choke: " + MyProcess.peers.get(peerIndex).getChangeChoke() );
                if(MyProcess.peers.get(peerIndex).getChangeChoke() == true){
                    if(MyProcess.peers.get(peerIndex).getIsChoked()){
                        System.out.println("CLIENT CHOKER " + myId + ": unchoke: " + connectedToID);
                        MyProcess.peers.get(peerIndex).setChoked(false);
                        message = MessageHandler.createunChokeMessage();
                        MyProcess.peers.get(peerIndex).setChoked(false);
                    }
                    else {
                        System.out.println("CLIENT CHOKER " + myId + ": choke: " + connectedToID);
                        MyProcess.peers.get(peerIndex).setChoked(true);
                        message = MessageHandler.createChokeMessage();
                        MyProcess.peers.get(peerIndex).setChoked(true);
                    }
                    MessageHandler.sendMessage(out, message);
                    MyProcess.peers.get(peerIndex).setChangeChoke(false);
                }






                //send out the have messages
                BitSet neededPieces = MessageHandler.getNeededPieces(MyProcess.bitField, clientBitfield);
                if(!neededPieces.isEmpty()) {
                    System.out.println("get have message pieces" + neededPieces);
                    int[] neededPieceIndexes = MessageHandler.getIndecesOfInterest(neededPieces);
                    clientBitfield = (BitSet) MyProcess.bitField.clone();
                    int temp;
                    byte[] tmp = new byte[4];
                    for (int i = 0; i < neededPieceIndexes.length; i++) {
                        temp = neededPieceIndexes[i];
                        ByteBuffer bb = ByteBuffer.allocate(4);
                        bb.putInt(temp);
                        msg = MessageHandler.createHaveMessage(bb.array());
                        MessageHandler.sendMessage(out, msg);
                    }
                }

                //yeah this is copy and pasted code from server.java but I can't use a method because
                //passing an inputstream causes a nullpointer exception.
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
                    message = MessageHandler.handleMessage(msg, type, connectedToID, myId, 'C', log);
                    if (type == 8){
                        //the peer who i am connected to is now done
                        MyProcess.peers.get(MyProcess.getPeerIndexById(connectedToID)).setDone();
                        //tell my process to check for other processes complete
                        MyProcess.checkDone = true;

                        // stop thread
                        System.out.println("CLIENT END " + myId + ": connected to " + connectedToID + " download complete");
                        System.out.println("CLIENT END " + myId + ": connected to " + connectedToID + " Disconnect with Server " + connectedToID);
                        System.out.println("CLIENT END " + myId + ": connected to " + connectedToID + " TERMINATED");
                        //System.exit(1);
                        return;
                    }
                    if (message != null && (message[4] == 2 || message[4] == 3 || message[4] == 7)) {
                        System.out.println("CLIENT " + myId + ": sending type " + message[4] + " to " + connectedToID);
                        MessageHandler.sendMessage(out, message);
                    }
                }
            }
        } catch (ConnectException e) {
            //System.out.println(e.getLocalizedMessage());
            System.err.println("Connection refused. You need to initiate a server first.");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.out.println("ERROR1\n");
            ioException.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("ERROR2\n");
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
                System.out.println("ERROR3\n");
                ioException.printStackTrace();
            }
        }
    }
}