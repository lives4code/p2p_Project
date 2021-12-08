import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Client extends Thread {

    private Socket requestSocket;
    private byte[] message;
    private DataInputStream in;
    private DataOutputStream out;
    private BitSet clientBitfield;
    private int port;
    private String host;
    private int myId;
    private int connectedToID;

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

            byte[] msg;
            byte[] sizeB = new byte[4];
            int type;
            int peerIndex = MyProcess.getPeerIndexById(connectedToID);

            while (true) {
                System.out.println("CLIENT " + myId + ": connected to " + connectedToID + ": beginning new loop iteration");

                //check if it needs to send a choke or unchoke message.
                if(MyProcess.peers.get(peerIndex).shouldChangeChoke()){
                    if(MyProcess.peers.get(peerIndex).isPeerChoked()){
                        System.out.println("CLIENT CHOKER " + myId + ": unchoke: " + connectedToID);
                        message = MessageHandler.createunChokeMessage();
                        MyProcess.peers.get(peerIndex).unchokePeer();
                    }
                    else {
                        System.out.println("CLIENT CHOKER " + myId + ": choke: " + connectedToID);
                        message = MessageHandler.createChokeMessage();
                        MyProcess.peers.get(peerIndex).chokePeer();
                    }
                    MessageHandler.sendMessage(out, message);
                    MyProcess.peers.get(peerIndex).changeChokeOfPeer(false);
                }

                //send out the have messages
                BitSet neededPieces = MessageHandler.getNeededPieces(MyProcess.bitField, clientBitfield);
                if(!neededPieces.isEmpty()) {
                    System.out.println("get have message pieces" + neededPieces);
                    int[] neededPieceIndexes = MessageHandler.getIndecesOfInterest(neededPieces);
                    clientBitfield = (BitSet) MyProcess.bitField.clone();
                    int temp;
                    for (int i = 0; i < neededPieceIndexes.length; i++) {
                        temp = neededPieceIndexes[i];
                        ByteBuffer bb = ByteBuffer.allocate(4);
                        bb.putInt(temp);
                        msg = MessageHandler.createHaveMessage(bb.array());
                        MessageHandler.sendMessage(out, msg);
                    }
                }

                if(in.available() > 0 ) {
                    System.out.println("CLIENT " + myId + ": new input");
                    in.read(sizeB);
                    int size = ByteBuffer.wrap(sizeB).getInt();
                    msg = new byte[size];
                    type = in.read();
                    in.read(msg);
                    message = MessageHandler.handleMessage(msg, type, connectedToID, myId, 'C', log);
                    if (type == 8){
                        //the peer who i am connected to is now done
                        MyProcess.peers.get(MyProcess.getPeerIndexById(connectedToID)).setDone();
                        //tell my process to check for other processes complete
                        MyProcess.checkDone = true;
                        String s = "CLIENT END " + myId + ": connected to " + connectedToID + " download complete. print bitfield: ";
                        for (int i = 0; i < MyProcess.bitField.size(); i ++){
                            s += MyProcess.bitField.get(i) + ", ";
                        }
                        System.out.println(s);
                        return;
                    }

                    // send messages
                    if (message != null && (message[4] == 2 || message[4] == 3 || message[4] == 7)) {
                        MessageHandler.sendMessage(out, message);
                    }
                }
            }
        } catch (ConnectException e) {
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