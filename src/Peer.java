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
    double downloadRate; // bytes per ms
    boolean choked;
    boolean changeChoke;
    boolean optimistic;

    public Peer(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
        this.choked = true;
        this.changeChoke = false;
        this.isInterested = false;
        this.downloadRate = Double.MAX_VALUE;
        this.optimistic = false;
    }
    public void setInterested(boolean bool){
        isInterested = bool;
    }
    public boolean getIsInterested(){
        return this.isInterested;
    }
    public void setChoked(boolean bool){
        choked = bool;
    }
    public boolean getIsChoked(){
        return this.choked;
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
    public boolean getChangeChoke(){return this.changeChoke;};
    public void setChangeChoke(boolean val){this.changeChoke = val;}
    public boolean getOptimistic() {return this.optimistic;}
    public void setOptimistic(boolean val) {this.optimistic = val;}

    @Override
    public String toString(){
        String r = "peerId:" + peerId;
        r+= "\nhostName:" + hostName;
        r+= "\nisinterested:" + isInterested;
        r+= "\nchoked:" + choked;
        r+= "\nchangeChoke" + changeChoke;
        r+= "\ndownloadRate" + downloadRate;
        r+= "\nhasFile" + hasFile;
        r+= "\noptimistic" + optimistic;
        return r;
    }

}
