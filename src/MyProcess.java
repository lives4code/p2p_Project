import java.io.*;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.System.in;

public class MyProcess {
    // From Peer Info Cfg
    private int myId;
    // put your ip and port no
    private String myHostName;
    int port;

    boolean hasFile;
    static BitSet bitField;
    Timer timer;

    // Handle client, server, and peers
    Client client;
    Server server;
    static List<Peer> peers;

    // Common variables
    int numPrefNeighbors;
    int unchokeInterval;
    int optUnchokeInterval;
    String fileName;
    long fileSize;
    long pieceSize;
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

        // start timer
        startDeterminingNeighbors();
    }

    //TODO write that we are no longer looking for this piece.
    //wrote code to flip index.
    //talk to nick make sure this is right.
    public void writePiece(byte[] pieceIndex, byte[] piece){
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
    //TODO fix error that can occur at the last byte of the file.
    //fixed talk with group before pushing.
    public byte[] readPiece(byte[] pieceIndex ){
        byte[] ret = new byte[(int)pieceSize];
        int numPieces = (int) Math.ceil(fileSize/pieceSize);
        try {
            RandomAccessFile file = new RandomAccessFile("theFile", "r");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
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
        return ret;
    }

    //does this function cause each client to start from each computer?

    public void start() throws Exception {
        System.out.println("PEER " + myId + ": Peer is running");
        // Start client
        for (Peer peer: peers){
            if (peer.getPeerId() != myId) {
                new Client(myId, peer.getHostName(), peer.getPort()).start();
            }
        }

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
            //bitfield is initialized to false by default if the file is present set all the values to true.
            bitField = new BitSet(numPieces);
            if(hasFile){
                for(int i = 0; i < bitField.size(); i++){
                        bitField.flip(i);
                }
                bitField.set(0,8,false);
                bitField.set(1);
                bitField.set(2);
                bitField.set(4);
                //0b0001_0110
                //0x16
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

    public void startDeterminingNeighbors() {
        TimerTask redetermineNeighbors = new TimerTask() {
            public void run() {
                if (numPrefNeighbors > peers.size()) {
                    System.out.println("Error: Number of preferred neighbors cannot be greater than the number of peers.");
                    cancel();
                }
                List<Integer> fastestIndices = new ArrayList<>();
                // Find fastest indices
                for (int k = 0; k < numPrefNeighbors; k++) {
                    int minIndex = 0;
                    float minRate = Integer.MAX_VALUE;
                    for (int i = 0; i < peers.size(); i++) {
                        if (peers.get(i).downloadRate < minRate && !fastestIndices.contains(i)) {
                            minIndex = i;
                            minRate = peers.get(i).downloadRate;
                        }
                    }
                    fastestIndices.add(minIndex);
                }
                // Set new neighbors
                for (int i = 0; i < peers.size(); i++) {
                    if (fastestIndices.contains(i))
                        peers.get(i).choked = false;
                    else
                        peers.get(i).choked = true;
                }
            }
        };
        timer = new Timer();
        timer.schedule(redetermineNeighbors, 0, unchokeInterval * 1000);
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


}
