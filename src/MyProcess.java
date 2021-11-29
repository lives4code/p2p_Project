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

    // Handle peers
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

    public MyProcess(int peerId) {
        myId = peerId;
        peers = new ArrayList<>();
        loadCommonConfig();
        loadPeerInfo();

        // start timers
        startDeterminingNeighbors();
        startDeterminingOptimistic();

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
            String pieceRead = "";
            System.out.println("write successful index:" + ByteBuffer.wrap(pieceIndex).getInt()  + " piece:" + pieceRead);

        }
        catch (Exception e){
            System.out.println("error occurred");
        }
    }

    public static byte[] readPiece(byte[] pieceIndex ){
        System.out.println("piecesize" + pieceSize);
        byte[] ret = new byte[(int)pieceSize];
        int numPieces = (int) Math.ceil(fileSize/pieceSize);
        try {
            RandomAccessFile file = new RandomAccessFile("../Files_From_Prof/project_config_file_small/project_config_file_small/1001/thefile", "r");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            System.out.println("attempting to read piece with index:" + index);
            int skip = (int)pieceSize * index;
            file.seek(skip);
            //file.skipBytes(skip);
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
            System.out.println("error occurred while reading piece");
        }
        //System.out.println("read successful");
        String pieceRead = "";
        for(int i =0; i < ret.length; i++){
            pieceRead += (char)ret[i] + " ";
        }
        System.out.println("read successful index:" + ByteBuffer.wrap(pieceIndex).getInt()  + "size:" + ret.length + " piece:" + pieceRead);
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

        // Start Server
        ServerSocket listener = new ServerSocket(port);
        int clientNum = 1;
        try {
            while (true) {
                new Server(listener.accept(), clientNum, myId).start();
                System.out.println("PEER " + myId + ": Client " + clientNum + " is connected!");
                clientNum++;
            }
        } finally {
            listener.close();
        }
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

    private void startDeterminingNeighbors(){
        System.out.println("starting Determining neighbors. my id is:" + myId);
        TimerTask redetermineNeighbors = new TimerTask() {
            public void run() {
                System.out.println("Redetermining neighbors...");
                if (numPrefNeighbors > peers.size()) {
                    System.out.println("Error: Num preferred neighbors is greater than num of peers");
                    cancel();
                }
                List<Integer> fastestIndices = new ArrayList<>();
                // Find fastest indices
                for (int k = 0; k < numPrefNeighbors; k++) {
                    int minIndex = 0;
                    double minRate = Double.MAX_VALUE;
                    for (int i = 0; i < peers.size(); i++) {
                        if (peers.get(i).downloadRate < minRate && !fastestIndices.contains(i) && peers.get(i).getIsInterested()) {
                            minIndex = i;
                            minRate = peers.get(i).downloadRate;
                        }
                    }
                    fastestIndices.add(minIndex);
                }
                // Set new neighbors
                for (int i = 0; i < peers.size(); i++) {
                    // Unchoke new neighbor // Choke old neighbor
                    if ((fastestIndices.contains(i) && peers.get(i).getIsChoked())
                            || (!fastestIndices.contains(i) && !peers.get(i).getIsChoked())) {
                        peers.get(i).setChangeChoke(true);
                    }
                }

                // Debug
                for (int i = 0; i < peers.size(); i++) {
                    System.out.println("Peer " + i + ": " + peers.get(i).downloadRate + ", " + peers.get(i).choked);
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(redetermineNeighbors, 0, unchokeInterval * 1000);
    }

    private void startDeterminingOptimistic(){
        TimerTask redetermineOptimistic = new TimerTask() {
            public void run() {
                System.out.println("Redetermining optimistic...");
                // Randomly select optimistic neighbor
                List<Integer> chokedIndices = new ArrayList<>();
                for (int i = 0; i < peers.size(); i++) {
                    if (peers.get(i).getIsChoked() && peers.get(i).getIsInterested()) {
                        chokedIndices.add(i);
                    }
                }
                Random rand = new Random();
                int randomIndex = chokedIndices.size() > 0 ? chokedIndices.get(rand.nextInt(chokedIndices.size())) : -1;
                // Set new neighbors
                for (int i = 0; i < peers.size(); i++) {
                    // Unchoke optimistic
                    if (randomIndex == i) {
                        peers.get(i).setChangeChoke(true);
                    }
                }

                // Debug
                for (int i = 0; i < peers.size(); i++) {
                    System.out.println("Peer " + i + ": " + peers.get(i).downloadRate + ", " + peers.get(i).choked);
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(redetermineOptimistic, 0, optUnchokeInterval * 1000);
    }
}
