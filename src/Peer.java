import java.util.ArrayList;
import java.util.BitSet;

public class Peer {
    // From Peer Info Cfg
    private int peerId;
    private String hostName;
    int port;
    boolean hasFile;

    // Other
    boolean isInterested;
    BitSet bitField;
    ArrayList<byte[]> piecesNeeded;
    float downloadRate; // bytes per ms
    boolean choked;

    public Peer(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
        downloadRate = 100000;
    }
    public void setInterested(boolean bool){
        isInterested = bool;
    }
    public boolean getIsInterested(){
        return isInterested;
    }
    public void setChoked(boolean bool){
        choked = bool;
    }
    public boolean getIsChoked(){
        return choked;
    }
    public int getPeerId(){
        return this.peerId;
    }
    public String getHostName(){
        return this.hostName;
    }
    public  int getPort(){
        return this.port;
    }

}
