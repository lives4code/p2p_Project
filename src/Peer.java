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
    double downloadRate; // bytes per ms
    boolean amChokedBy;
    boolean isChokedByMe;
    boolean changeChoke;
    boolean optimistic;
    private boolean done;

    public Peer(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
        this.isChokedByMe = true;
        this.amChokedBy = true;
        this.changeChoke = false;
        this.isInterested = false;
        this.downloadRate = Double.MAX_VALUE;
    }
    public void setInterested(boolean bool){
        isInterested = bool;
    }
    public boolean getIsInterested(){
        return this.isInterested;
    }

    public void chokePeer() {isChokedByMe = true;}
    public void unchokePeer() {isChokedByMe = false;}
    public void chokeSelf() {amChokedBy = true;}
    public void unchokeSelf() {amChokedBy = false;}
    public boolean isPeerChoked() {return isChokedByMe;}
    public boolean amIChoked() {return amChokedBy;}

    public int getPeerId(){
        return this.peerId;
    }
    public String getHostName(){
        return this.hostName;
    }
    public  int getPort(){
        return this.port;
    }

    public boolean shouldChangeChoke(){return this.changeChoke;};
    public void changeChokeOfPeer(boolean val){this.changeChoke = val;}

    public boolean getOptimistic() {return this.optimistic;}
    public void setOptimistic(boolean val) {this.optimistic = val;}
    public boolean getDone(){return this.done;}
    public void setDone(){this.done = true;}
    
    @Override
    public String toString(){
        String r = "peerId:" + peerId;
        r+= "\nhostName:" + hostName;
        r+= "\nisinterested:" + isInterested;
        r+= "\nisChokedByMe:" + isChokedByMe;
        r+= "\namChokedBy:" + amChokedBy;
        r+= "\nchangeChoke" + changeChoke;
        r+= "\ndownloadRate" + downloadRate;
        r+= "\nhasFile" + hasFile;
        r+= "\noptimistic" + optimistic;
        return r;
    }

}
