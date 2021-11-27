import java.io.*;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.in;

public class MyProcess {
    // From Peer Info Cfg
    private int myId;
    // put your ip and port no
    private String myHostName;
    int port;

    boolean hasFile;
    static BitSet bitField;

    // Handle client, server, and peers
    Client client;
    Server server;
    static List<Peer> peers;

    // Common variables
    static int numPrefNeighbors;
    static int unchokeInterval;
    static int optUnchokeInterval;
    static String fileName;
    static long fileSize;
    static long pieceSize;
    Piece[] pieceArray;
    //initialize piece array

    static int test = 0;

    public MyProcess(int peerId) {
        myId = peerId;
        peers = new ArrayList<>();
        loadCommonConfig();
        loadPeerInfo();

        //debug
        System.out.println("PEER: piece size: " + pieceSize);
        System.out.println("PEER: file size: " + fileSize);
    }


    //wrote code to flip index.
    //talk to nick make sure this is right.
    public static void writePiece(byte[] pieceIndex, byte[] piece){
        try {
            RandomAccessFile file = new RandomAccessFile("theFile", "w");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            int skip = (int)pieceSize * index;
            file.skipBytes(skip);
            file.write(piece);
            bitField.flip(index);
            file.close();
        }
        catch (Exception e){
            System.out.println("error occured");
        }
    }

    public static byte[] readPiece(byte[] pieceIndex ){
        byte[] ret = new byte[(int)pieceSize];
        int numPieces = (int) Math.ceil(fileSize/pieceSize);
        try {
            RandomAccessFile file = new RandomAccessFile("theFile", "r");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            System.out.println("attempting to read piece with index:" + index);
            int skip = (int)pieceSize * index;
            file.skipBytes(skip);
            if(index == numPieces){
                int lastPieces = (int) (fileSize - (Math.floor(fileSize/pieceSize) * pieceSize));
                byte[] lastPiece = new byte[lastPieces];
                file.read(lastPiece);
                for(int i = 0; i < lastPieces; i++){
                    ret[i] = lastPiece[i];
                }
                for(int i = lastPieces; i < (int) pieceSize; i++){
                    ret[i] = 0;
                }
            }
            else {
                file.read(ret);
            }
            file.close();
        }
        catch (Exception e){
            System.out.println("error occured while reading piece");
        }
        System.out.println("read successful");
        System.out.println(ret);
        return ret;
    }

    //does this function cause each client to start from each computer?

    public void start() throws Exception {
        System.out.println("PEER " + myId + ": Peer is running");
        // Start client
        for (Peer peer: peers){
            if (peer.getPeerId() != myId) {
                new Client(myId, peer.getHostName(), peer.getPort(), peer.getPeerId()).start();
            }
        }
        startDeterminingNeighbors();

        //System.out.println("debug 1");
        //new ClientSpawn().start();
        // Start Server
        ServerSocket listener = new ServerSocket(port);
        int clientNum = 1;
        try {
            while (true) {
                //System.out.println("debug 2");
                new Server(listener.accept(), clientNum, myId).start();
                System.out.println("PEER " + myId + ": Client " + clientNum + " is connected!");
                clientNum++;
                //System.out.println("debug ");

                //System.out.println("debug " + test);
            }
        } finally {
            listener.close();
        }
    }
    public byte[] intToByte(int input){
        byte[] bytes = ByteBuffer.allocate(4).putInt(input).array();
        return bytes;
    }

    public void loadPeerInfo() {
        try {
            int numPieces = (int) Math.ceil(fileSize / pieceSize);
            File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/PeerInfo.cfg");
            Scanner fileReader = new Scanner(myObj);
            while (fileReader.hasNext()) {
                int peerId = Integer.valueOf(fileReader.next());
                String hostName = fileReader.next();
                int port = Integer.valueOf(fileReader.next());
                boolean hasFile = Integer.valueOf(fileReader.next()) == 1;
                if (peerId == myId) {
                    this.myHostName = hostName;
                    this.port = port;
                    this.hasFile = hasFile;
                } else {
                    peers.add(new Peer(peerId, hostName, port, hasFile));
                }
            }
            System.out.println("empty peers are initialized");
            //bitfield is initialized to false by default if the file is present set all the values to true.
            bitField = new BitSet(numPieces);
            if(hasFile){
                for(int i = 0; i < bitField.size(); i++){
                        bitField.set(i);
                }
            }
            if(this.hasFile == false){
                File theFile = new File("theFile");
                if (theFile.createNewFile()) {
                    System.out.println("File created: " + theFile.getName());
                } else {
                    System.out.println("File already exists.");
                }
                File outputFile = theFile;
                byte[] emptyFile = new byte[(int) fileSize];
                for(int i = 0; i < fileSize; i++){
                    emptyFile[i] = 0;
                }
                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    outputStream.write(emptyFile);
                }
                System.out.println("Successfully wrote to the file.  ID: " + myId);
            }
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    public void loadCommonConfig() {
        try {
            File myObj = new File("../Files_From_Prof/project_config_file_small/project_config_file_small/Common.cfg");
            Scanner fileReader = new Scanner(myObj);
            while (fileReader.hasNextLine()) {
                // Number of Preferred Neighbors
                fileReader.next();
                numPrefNeighbors = Integer.valueOf(fileReader.next());
                // Unchoking Interval
                fileReader.next();
                unchokeInterval = Integer.valueOf(fileReader.next());
                // Optimistic Unchoking Interval
                fileReader.next();
                optUnchokeInterval = Integer.valueOf(fileReader.next());
                // File Name
                fileReader.next();
                fileName = fileReader.next();
                // File Size
                fileReader.next();
                fileSize = Long.valueOf(fileReader.next());
                // Piece Size
                fileReader.next();
                pieceSize = Long.valueOf(fileReader.next());
            }
            fileReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static int getPeerIndexById(int id) {
        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).getPeerId() == id) {
                return i;
            }
        }
        return -1;
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }
    // Moved to Server instead of MyProcess cuz it has to send messages
    private void startDeterminingNeighbors(){
        System.out.println("starting Determining neighbors. my id is:" + myId);
        TimerTask redetermineNeighbors = new TimerTask() {
            public void run() {
                System.out.println("Redetermining neighbors...");
                if (MyProcess.numPrefNeighbors > MyProcess.peers.size()) {
                    //System.out.println("Error: Number of preferred neighbors cannot be greater than the number of peers.");
                    //System.out.println("yes it can.");
                    //cancel();
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
                    if (fastestIndices.contains(i) && MyProcess.peers.get(i).getIsChoked() || (!fastestIndices.contains(i)) && MyProcess.peers.get(i).getIsChoked()) {
                        MyProcess.peers.get(i).setChangeChoke(true);
                        //byte[] mes = MessageHandler.createMsg(0, new byte[] {});
                        //MessageHandler.sendMessage(out, mes);
                    }
                    /*
                    // Choke old neighbor
                    else if (!fastestIndices.contains(i) && !MyProcess.peers.get(i).getIsChoked()) {
                        MyProcess.peers.get(i).setChoked(true);
                        byte[] mes = MessageHandler.createMsg(1, new byte[] {});
                        //MessageHandler.sendMessage(out, mes);
                    }

                     */
                    // Anything else, no change
                }

                // Debug
                for (int i = 0; i < MyProcess.peers.size(); i++) {
                    System.out.println("Peer " + i + ": " + MyProcess.peers.get(i).downloadRate + ", " + MyProcess.peers.get(i).choked);
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(redetermineNeighbors, 0, MyProcess.unchokeInterval * 1000);
    }


}
