import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

//handler thread class. handlers are spawned from the listening
//loop and are responsible for dealing with a single client's
//requests
public class Server extends Thread {

    private byte[] message;            //message received from the client
    private Socket connection;        //wait on a connection from client
    private int no;                //The index number of the client
    private DataInputStream in;    //stream read from the socket
    private DataOutputStream out;    //stream write to the socket
    private int myId;
    private int clientId;
    Timer timer;

    //debug
    private String s;

    public Server(Socket connection, int no, int myId) {
        this.connection = connection;
        this.no = no;
        this.myId = myId;
    }

    public void run() {
        try {
            //initialize Input and Output streams
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            // start timer
            startDeterminingNeighbors();

            try {

                //receive handshake and validate
                System.out.println("SERVER " + myId + ": reading handshake from peer");
                byte[] handshake = new byte[32];
                in.read(handshake, 0, 32);

                //MessageHandler.receiveHandshake(in, message);

                //validate handshake
                try{
                    clientId = MessageHandler.validateHandshake(handshake, myId);
                    System.out.println("SERVER " + myId + ": handshake read from peer. ClientID: " + clientId);
                }
                catch (Exception e){
                    currentThread().interrupt();
                    System.out.println("SERVER: " + myId +  " handshake invalid:" + e.getLocalizedMessage());

                }

                //debug print my bitset
                byte[] bf = MyProcess.bitField.toByteArray();
                s = "SERVER " + myId + ": my bitfield: ";

                for(byte b : bf){
                    s += "0x" + MyProcess.byteToHex(b).toUpperCase() + ", ";
                }

                System.out.println(s);
                //end debug

                //receive bitfield
                //yeah this is copy and pasted code from client.java but I can't use a method because
                //passing an inputstream causes a nullpointer exception.
                byte[] msg;
                byte[] sizeB = new byte[4];
                int type = -1;
                long start = System.currentTimeMillis();
                int read = in.read(sizeB);
                int size = ByteBuffer.wrap(sizeB).getInt();
                msg = new byte[size];
                type = in.read();
                read += in.read(msg);
                long cost = System.currentTimeMillis() - start;
                message = MessageHandler.handleMessage(msg, type, clientId);
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField = BitSet.valueOf(message);
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).downloadRate = read / cost; // bytes per ms
                s = "SERVER " + myId + " bitfield msg DEBUG: ";
                printBitfield(message, s);

                //convert to little endian
//                ByteBuffer bb = ByteBuffer.wrap(message);
//                bb.order( ByteOrder.LITTLE_ENDIAN);
//                byte[] arr = new byte[bb.remaining()];
//                bb.get(arr);

                //print receives bitset
                printBitfield(message ,s);

                //debug
//                byte[] bf = MyProcess.bitField.toByteArray();
//                s = "SERVER my bitfield:\n";
//
//                for(byte b : bf){
//                    s += "0x" + MyProcess.byteToHex(b) + ", ";
//                }
//
//                System.out.println(s);
                //end debug

                //listener loop.
                while (true) {
                    if (in.available() > 0) {
                        in.read(sizeB);
                        size = ByteBuffer.wrap(sizeB).getInt();
                        msg = new byte[size];
                        type = in.read();
                        in.read(msg);
                        message = MessageHandler.handleMessage(msg, type, clientId);
                        //if the message handler returns an interested or uninterested message then send it.
                        if(message[4] == 2 || message[4] == 3){
                            MessageHandler.sendMessage(out, message);
                        }
                    }
                }



                // Interested or Not Interested
                /*
                boolean interested = checkForInterest(BitSet.valueOf(message), MyProcess.bitField);
                if (interested)
                    message = MessageHandler.createMsg(2, new byte[]{});
                else
                    message = MessageHandler.createMsg(3, new byte[] {});
                MessageHandler.sendMessage(out, message);

                while (true) {
                    //MessageHandler.handleMessage(in);
                }

                 */
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
                System.err.println("SERVER: Data received in unknown format");
                exception.printStackTrace();
            }
        } catch (IOException ioException) {
            System.out.println("Disconnect with Client " + no);
        } finally {
            //Close connections
            try {
                if(in != null) {
                    in.close();
                }
                if(out != null) {
                    out.close();
                }
                connection.close();
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            }
        }
    }

    private boolean checkForInterest(BitSet received, BitSet mine) {
        System.out.println();
        boolean interested = false;
        received.xor(mine);
        for (int i = 0; i < received.length(); i++) {
            if (received.get(i) == true) {
                interested = true;
                break;
            }
        }
        return interested;
    }

//    private void printBitfield(BitSet bits) {
//        byte[] bytes = bits.toByteArray();
//        s = "";
//        for (byte b : bytes) {
//            s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + " ";
//        }
//        System.out.println(s);
//    }

    private void printBitfield(byte[] bytes, String s) {
        for (byte b : bytes) {
            s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + ", ";
        }
        System.out.println(s);
    }

    // Moved to Server instead of MyProcess cuz it has to send messages
    private void startDeterminingNeighbors() {
        TimerTask redetermineNeighbors = new TimerTask() {
            public void run() {
                System.out.println("Redetermining neighbors...");
                if (MyProcess.numPrefNeighbors > MyProcess.peers.size()) {
                    System.out.println("Error: Number of preferred neighbors cannot be greater than the number of peers.");
                    cancel();
                }
                List<Integer> fastestIndices = new ArrayList<>();
                // Find fastest indices
                for (int k = 0; k < MyProcess.numPrefNeighbors; k++) {
                    int minIndex = 0;
                    float minRate = Integer.MAX_VALUE;
                    for (int i = 0; i < MyProcess.peers.size(); i++) {
                        if (MyProcess.peers.get(i).downloadRate < minRate && !fastestIndices.contains(i)) {
                            minIndex = i;
                            minRate = MyProcess.peers.get(i).downloadRate;
                        }
                    }
                    fastestIndices.add(minIndex);
                }
                // Set new neighbors
                for (int i = 0; i < MyProcess.peers.size(); i++) {
                    // Unchoke new neighbor
                    if (fastestIndices.contains(i) && MyProcess.peers.get(i).choked) {
                        MyProcess.peers.get(i).choked = false;
                        byte[] mes = MessageHandler.createMsg(0, new byte[] {});
                        MessageHandler.sendMessage(out, mes);
                    }
                    // Choke old neighbor
                    else if (!fastestIndices.contains(i) && !MyProcess.peers.get(i).choked) {
                        MyProcess.peers.get(i).choked = true;
                        byte[] mes = MessageHandler.createMsg(1, new byte[] {});
                        MessageHandler.sendMessage(out, mes);
                    }
                    // Anything else, no change
                }

                // Debug
                for (int i = 0; i < MyProcess.peers.size(); i++) {
                    System.out.println("Peer " + i + ": " + MyProcess.peers.get(i).downloadRate + ", " + MyProcess.peers.get(i).choked);
                }
            }
        };
        timer = new Timer();
        timer.schedule(redetermineNeighbors, 0, MyProcess.unchokeInterval * 1000);
    }

    // Moved to Server instead of MyProcess cuz it has to send messages
    private void startDeterminingNeighbors() {
        TimerTask redetermineNeighbors = new TimerTask() {
            public void run() {
                System.out.println("Redetermining neighbors...");
                if (MyProcess.numPrefNeighbors > MyProcess.peers.size()) {
                    System.out.println("Error: Number of preferred neighbors cannot be greater than the number of peers.");
                    cancel();
                }
                List<Integer> fastestIndices = new ArrayList<>();
                // Find fastest indices
                for (int k = 0; k < MyProcess.numPrefNeighbors; k++) {
                    int minIndex = 0;
                    float minRate = Integer.MAX_VALUE;
                    for (int i = 0; i < MyProcess.peers.size(); i++) {
                        if (MyProcess.peers.get(i).downloadRate < minRate && !fastestIndices.contains(i)) {
                            minIndex = i;
                            minRate = MyProcess.peers.get(i).downloadRate;
                        }
                    }
                    fastestIndices.add(minIndex);
                }
                // Set new neighbors
                for (int i = 0; i < MyProcess.peers.size(); i++) {
                    // Unchoke new neighbor
                    if (fastestIndices.contains(i) && MyProcess.peers.get(i).choked) {
                        MyProcess.peers.get(i).choked = false;
                        byte[] mes = MessageHandler.createMsg(0, new byte[] {});
                        MessageHandler.sendMessage(out, mes);
                    }
                    // Choke old neighbor
                    else if (!fastestIndices.contains(i) && !MyProcess.peers.get(i).choked) {
                        MyProcess.peers.get(i).choked = true;
                        byte[] mes = MessageHandler.createMsg(1, new byte[] {});
                        MessageHandler.sendMessage(out, mes);
                    }
                    // Anything else, no change
                }

                // Debug
                for (int i = 0; i < MyProcess.peers.size(); i++) {
                    System.out.println("Peer " + i + ": " + MyProcess.peers.get(i).downloadRate + ", " + MyProcess.peers.get(i).choked);
                }
            }
        };
        timer = new Timer();
        timer.schedule(redetermineNeighbors, 0, MyProcess.unchokeInterval * 1000);
    }
}

