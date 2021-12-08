
import java.io.*;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class MyProcess {
    // From Peer Info Cfg
    private static int myId;
    private String myHostName;
    int port;
    static boolean hasFile;
    static BitSet bitField;
    private static int optUnchokeId;


    // Handle peers
    static List<Peer> peers;
    static boolean checkDone;
    static boolean done;

    // Common variables
    static int numPrefNeighbors;
    static int unchokeInterval;
    static int optUnchokeInterval;
    static String filename;
    static long fileSize;
    static long pieceSize;

    // Logging
    Logger log = Logger.getLogger("Log");
    FileHandler fh;

    public MyProcess(int peerId) {
        myId = peerId;
        filename = "../peers/" + String.valueOf(myId) + "/";
        peers = new ArrayList<>();
        checkDone = false;

        loadCommonConfig();
        loadPeerInfo();

        done = false;

        //debug
        System.out.println("PEER: piece size: " + pieceSize);
        System.out.println("PEER: file size: " + fileSize);

        // configure logger
        try {
            fh = new FileHandler("../log/log_peer_" + peerId + ".log");
            MyFormatter formatter = new MyFormatter();
            fh.setFormatter(formatter);
            log.addHandler(fh);
            log.setUseParentHandlers(false);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // start timers
        try {
            startDeterminingNeighbors();
            startDeterminingOptimistic();
        }catch (Exception e){
            System.out.println("ERROR 3");
        }
    }

    public static void writePiece(byte[] pieceIndex, byte[] piece){
        try {
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            if(bitField.get(index) == false) {
                int skip = (int) pieceSize * index;
                file.seek(skip);
                file.write(piece);
                bitField.set(index);
                System.out.println("bitfield is now" + bitField);
                file.close();
                //System.out.println("write successful index:" + index  + " piece:" + pieceRead);
            }

        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("error occurred");
        }
    }

    public static byte[] readPiece(byte[] pieceIndex ){
        byte[] piece = new byte[(int) pieceSize];
        int numPieces = (int) Math.ceil(fileSize/pieceSize);
        byte[] pieceInd = new byte[4];
        byte[] ret = new byte[(int)pieceSize + 4];
        for(int i = 0; i < 4; i++){
            pieceInd[i] = pieceIndex[i];
        }
        try{
            RandomAccessFile file = new RandomAccessFile(filename, "r");
            int index = ByteBuffer.wrap(pieceIndex).getInt();
            int skip = (int)pieceSize * index;
            file.seek(skip);
            if(index == numPieces){
                int lastPieces = (int) (fileSize - (Math.floor(fileSize/pieceSize) * pieceSize));
                byte[] lastPiece = new byte[lastPieces];
                file.read(lastPiece);
                for(int i = 0; i < lastPieces; i++){
                    piece[i] = lastPiece[i];
                }
                for(int i = lastPieces; i < (int) pieceSize; i++){
                    piece[i] = 0;
                }
            }
            else {
                file.read(piece);
            }
            file.close();
        }
        catch (Exception e){
            System.out.println("error occurred while reading piece");
        }
        for(int i = 0; i < 4; i++){
            ret[i] = pieceInd[i];
        }
        for(int i =4; i < ret.length; i++){
            ret[i] = piece[i - 4];
        }
        return ret;
    }

    public void start() throws Exception {
        System.out.println("PEER " + myId + ": Peer is running");
        // Start client
        for (Peer peer: peers){
            if (peer.getPeerId() != myId) {
                new Client(myId, peer.getHostName(), peer.getPort(), peer.getPeerId(), log).start();
                log.info("Peer " + myId + " makes a connection to Peer " + peer.getPeerId() + ".");
            }
        }

        // Start Server
        ServerSocket listener = new ServerSocket(port);
        int clientNum = 1;
        try {
            outerloop:
            while (true) {
                try {
                    listener.setSoTimeout(3000);
                    new Server(listener.accept(), clientNum, myId, log).start();
                    System.out.println("PEER " + myId + ": Client " + clientNum + " is connected!");
                    clientNum++;
                } catch (SocketTimeoutException e) {
                    String s = "PEER CHECK " + myId + ": TIMEOUT done: " + done + " print bitfield: ";
                    for (int i = 0; i < MyProcess.bitField.size(); i ++){
                        s += MyProcess.bitField.get(i) + ", ";
                    }
                    System.out.println(s);
                    s = "PEER CHECK " + myId + ": TIMEOUT peers done: ";
                    for(Peer peer : peers){
                        s += "(" + peer.getPeerId() + ", " + peer.getDone() + "), ";
                    }
                    System.out.println(s);
                }

                //check for children done
                if (checkDone){
                    //am i done
                    if (!done){
                        System.out.println("PEER CHECK " + myId + ": not done");
                        checkDone = false;
                        System.out.println("PEER CHECK " + myId + ": continue");
                        continue outerloop;
                    }

                    //see if each peer is done if so quit
                    for (Peer peer: peers){
                        if (!peer.getDone()){
                            checkDone = false;
                            System.out.println("PEER CHECK " + myId + ": continue");
                            continue outerloop;
                        }
                        // all other peers are done
                    }
                    System.out.println("PEER CHECK " + myId + ": break");
                    break;
                }
            }
        } finally {
            listener.close();
            System.out.println("PEER CHECK " + myId + ": exit in 5 sec");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("PEER CHECK " + myId + ": exit");
            System.exit(1);
        }
    }

    public void loadPeerInfo() {
        try {
            int numPieces = (int) Math.ceil(fileSize / pieceSize);
            // load file
            File myObj = new File("../cfg/PeerInfo.cfg");
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
            for(Peer p: peers){
                System.out.println("myid: "+ myId + " peer id: "+ p.getPeerId());
                log.info("myid: "+ myId + " peer id: "+ p.getPeerId());
            }
            //bitfield is initialized to false by default if the file is present set all the values to true.
            bitField = new BitSet(numPieces);
            if(this.hasFile){
                for(int i = 0; i < bitField.size(); i++){
                    bitField.set(i);
                }
            }
            if(this.hasFile == false){
                File theFile = new File(filename);
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
            File myObj = new File("../cfg/Common.cfg");
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
                filename += fileReader.next();
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


    private void startDeterminingNeighbors(){
        System.out.println("starting Determining neighbors. my id is:" + myId);
        TimerTask redetermineNeighbors = new TimerTask() {
            public void run() {
                System.out.println("Redetermining neighbors...");
                if (numPrefNeighbors > peers.size()) {
                    System.out.println("Error: Num preferred neighbors is greater than num of peers");
                    cancel();
                }

                //find fastest indices
                ArrayList<double[]> down = new ArrayList<double[]>();
                for(int i = 0; i < peers.size(); i++){
                    Peer p = peers.get(i);
                    if(p.getIsInterested()){
                        System.out.println("PEER " + myId + ": adding to down: " + p.getPeerId());
                        double[] d = new double[2];
                        d[0] = (double)p.getPeerId();
                        d[1] = p.downloadRate;
                        down.add(d);
                    }
                }
                Collections.sort(down, (o1, o2) -> {
                    //sign is flipped so it sorts largest to smallest
                    if(o1[1] > o2[1]) return -1;
                    else if(o1[1] == o2[1]) return 0;
                    else return 1;
                });
                System.out.println("DEBUG " + myId + " " + down.size());
                for(int i = 0; i < down.size(); i++){
                    Peer p = peers.get(getPeerIndexById((int)down.get(i)[0]));
                    System.out.println("PEER " + myId + ": checking change choke for: " + p.getPeerId());
                    if(i < numPrefNeighbors) {
                        if(p.isPeerChoked()){
                            p.changeChokeOfPeer(true);
                        }
                    }
                    else {
                        if(!p.isPeerChoked() && (p.getPeerId() != optUnchokeId)) {
                            p.changeChokeOfPeer(true);
                        }
                    }
                }

                // Debug
                for (int i = 0; i < peers.size(); i++) {
                    System.out.println("Peer " + peers.get(i).getPeerId() + ": " + peers.get(i).downloadRate + ", " + (peers.get(i).changeChoke ? "changing choke" + myId: "" + myId));
                }

                // Log
                if (down.size() == 0)
                    log.info("Peer " + myId + " has no preferred neighbors.");
                else {
                    String msg = "Peer " + myId + " has the preferred neighbors ";
                    if(numPrefNeighbors < down.size()){
                        for(int i = 0; i < numPrefNeighbors; i++) {
                            msg += down.get(i)[0];

                        }
                    }
                    msg = msg.substring(0, msg.length() - 2);
                    log.info(msg + ".");
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
                if (numPrefNeighbors + 1 > peers.size()) {
                    System.out.println("NumPrefNeighbors == num of peers => all neighbors always unchoked");
                    cancel();
                }
                Random rand = new Random();
                ArrayList<Peer> candidates = new ArrayList<>();
                for(Peer p: peers){
                    if(p.isPeerChoked() && p.getIsInterested()){
                        candidates.add(p);
                    }
                }
                if(candidates.size() < 1){
                    System.out.println("Process:" + myId + " not enough inteerested choked neighbors to do random unchoke");
                    log.info("Peer " + myId + " has no optimistically unchoked neighbors.");
                }
                else {
                    int randomId = 0;
                    rand.nextInt(candidates.size());
                    System.out.println("PEER " + myId + ": random id " + randomId);
                    int randomPeerId = candidates.get(randomId).getPeerId();
                    System.out.println("PEER " + myId + ": random peer " + randomPeerId);
                    candidates.get(randomId).changeChokeOfPeer(true);
                    optUnchokeId = randomPeerId;
                    String msg = "Peer " + myId + " has the optimistically unchoked neighbor ";
                    msg += randomPeerId;
                    log.info(msg + ".");
                }
                // Debug
                for (int i = 0; i < candidates.size(); i++) {
                    System.out.println("PEER " + myId + ": candidate " + candidates.get(i).getPeerId());
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(redetermineOptimistic, 0, optUnchokeInterval * 1000);
    }
}