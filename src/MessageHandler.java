import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

public class MessageHandler {

    public static void sendMessage(DataOutputStream out, byte[] msg) {
        try{
            out.write(msg, 0, msg.length);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    public static byte[] createInterestedMessage(){
        return createMsg(2, new byte[]{});
    }
    public static byte[] createUninterestedMessage(){
        return createMsg(3, new byte[]{});
    }
    public static byte[] createPieceMessage(byte[] pieceIndex){
        return createMsg(7, MyProcess.readPiece(pieceIndex));
    }
    public static byte[] createHaveMessage(byte[] pieceIndex){
        return createMsg(4, pieceIndex);
    }
    public static byte[] createRequestMessage(byte[] pieceIndex){
        return createMsg(6, pieceIndex);
    }
    public static byte[] createChokeMessage(){
        return createMsg(0, new byte[]{});
    }
    public static byte[] createunChokeMessage(){
        return createMsg(1, new byte[]{});
    }
    public static byte[] createHandshake(int peerID) {
        byte[] bytes = new byte[32];
        String hexString = "P2PFILESHARINGPROJ";
        byte[] byteString = hexString.getBytes();
        for (int i = 0; i < byteString.length; i++) {
            bytes[i] = byteString[i];
        }
        for (int i = 18; i < 28; i++) {
            bytes[i] = 0x00;
        }

        byte[] peer_id = ByteBuffer.allocate(4).putInt(peerID).array();
        for ( int i = 0; i < 4; i++){
            bytes[28 + i] = peer_id[i];
        }
        return bytes;
    }

    public static int validateHandshake(byte[] msg, int peerId) throws Exception {
        byte[] header = ("P2PFILESHARINGPROJ").getBytes();
        // Check for appropriate header
        if (!Arrays.equals(header, Arrays.copyOfRange(msg, 0, 18))) {
            throw new Exception("string not equal.");
        }
        // Check for zeros
        for (int i = 18; i < 28; i++) {
            if (msg[i] != 0x00)
                throw new Exception("zeros not equal");
        }
        // Check for peer id
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++){
            b[i] = msg[i + 28];
        }
        int id = ByteBuffer.wrap(b).getInt();
        System.out.println("Validate handshake returns ID: " + id);
        return id;
    }

    public static byte[] createMsg(int mType, byte[] payload){

        byte[] bytes = new byte[5 + payload.length];
        byte[] messageLength = ByteBuffer.allocate(4).putInt(payload.length).array();

        //message length
        for(int i = 0; i < 4; i++){
            bytes[i] = messageLength[i];
        }
        //message type
        bytes[4] = (byte)mType;
        //message payload
        for(int i = 0; i < payload.length; i++){
            bytes[i+5] = payload[i];
        }
        /*
        if(ByteBuffer.wrap(messageLength).getInt() > 0) {
            int intpayload = ByteBuffer.wrap(payload).getInt();
            System.out.println("payload is " + intpayload);
        }

         */
        return bytes;
    }
    public static void printMessageHandlerDebug(int type, int clientId, int myId, char s){
        String operation;
        switch (type){
            case 0: operation = "choke";
            break;
            case 1: operation = "unchoke";
            break;
            case 2: operation = "interested";
            break;
            case 3: operation = "not interested";
            break;
            case 4: operation = "have";
            break;
            case 5: operation = "bitfield";
            break;
            case 6: operation = "request";
            break;
            case 7: operation = "piece";
            break;
            default: operation = "invalid";
            break;

        }
        String clientOrServer;
        if(s == 'S' || s == 's'){
            clientOrServer = "server";
        }
        else {
            clientOrServer = "client";
        }
        System.out.println("MES HANDLER:" + myId + " Handling type " + type + " message operation: " + operation + " client id:" + clientId + " from " + clientOrServer);
    }

    //handle message
    public static byte[] handleMessage(byte[] msg, int type, int clientId, int myId, char s) {
        switch(type){
            case -1:
                //break;
            case 0:
                //choke
                printMessageHandlerDebug(0, clientId, myId, s);
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setChoked(true);
                return null;
            case 1:
                //unchoke
                printMessageHandlerDebug(1, clientId, myId, s);
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setChoked(false);
                System.out.println("MES " + "client:" + clientId + "ischoked:" + MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).getIsChoked());
                return null;
            case 2:
                //interested
                printMessageHandlerDebug(2, clientId, myId, s);
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setInterested(true);
                return null;
            case 3:
                //not intrested
                printMessageHandlerDebug(3, clientId, myId, s);
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setInterested(false);
                return null;
            case 4:
                //have
                //first update the bitfield to reflect the new piece.
                //then return either an interested or not interested method after comparison.
                int i = ByteBuffer.wrap(msg).getInt();
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField.set(i);
                printMessageHandlerDebug(4, clientId, myId, s);
                if(MyProcess.bitField.get(i) == false){
                    //send interested
                    return createInterestedMessage();
                }
                else {
                    //send not interested
                    return createUninterestedMessage();
                }
            case 5:
                //bitfield
                printMessageHandlerDebug(5, clientId, myId, s);
                if(msg.length == 0){
                    System.out.println("bitfield is empty.");
                    BitSet b = new BitSet(MyProcess.bitField.size());
                    b.clear();
                    MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField = b;
                    System.out.println("empty- size: " +b.size());
                    return createUninterestedMessage();
                }
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField = BitSet.valueOf(msg);
                System.out.println("about to check for interest mybitfield:" + MyProcess.bitField + "incoming bitfield" + BitSet.valueOf(msg));
                if(checkForInterest(BitSet.valueOf(msg), MyProcess.bitField)){
                    return createInterestedMessage();
                }
                else {
                    return createUninterestedMessage();
                }
            case 6:
                //request
                printMessageHandlerDebug(6, clientId, myId, s);
                return createPieceMessage(msg);
            case 7:
                //piece
                printMessageHandlerDebug(7, clientId, myId, s);
                byte[] pieceIndex = new byte[4];
                for( i =0; i < 4; i++){
                    pieceIndex[i] = msg[i];
                }
                MyProcess.bitField.set(ByteBuffer.wrap(pieceIndex).getInt());
                System.out.println("writing piece index:" + ByteBuffer.wrap(pieceIndex).getInt());
                byte[] arr = Arrays.copyOfRange(msg, 4, msg.length);
                MyProcess.writePiece(pieceIndex,arr);
                return createPieceMessage(pieceIndex);
            default:
                // invalid
                System.out.println("invalid type " + type);
                break;
        }
        return msg;
    }

    public static BitSet getComplement(BitSet input){
        BitSet ret = new BitSet(input.size());
        if(input.isEmpty()){
            for(int i =0; i < ret.size(); i++){
                ret.set(i);
            }
        }
        else {
            for (int i = 0; i < input.length(); i++) {
                ret.set(i, input.get(i));
                ret.flip(i);
            }
        }

        return ret;
    }
    public static BitSet getNeededPieces(BitSet received, BitSet mine){
        if(mine == null || mine.isEmpty()){
            //System.out.println("mine is null");
            mine = new BitSet(received.length());
        }
        if(received == null || received.isEmpty()){
            //System.out.println("received is null");
            received = new BitSet(mine.length());
        }
        BitSet comp = getComplement(mine);
        comp.and(received);
        //System.out.println("pieces Needed:" + comp);
        return comp;
    }
    public static boolean checkForInterest(BitSet received, BitSet mine) {
        BitSet comp = getNeededPieces(received, mine);
        //System.out.println("checking for interest" + comp);
        boolean interested = false;
        for (int i = 0; i < comp.length(); i++) {
            if (comp.get(i) == true) {
                interested = true;
                break;
            }
        }
        return interested;
    }
    public static int[] getIndecesOfInterest(BitSet input){
        int size = 0;
        for(int i = 0; i < input.size(); i++){
            if(input.get(i)){
                size++;
            }
        }
        int[] ret = new int[size];
        for (int i =0; i < input.size(); i++){
            int index = 0;
            if(input.get(i)){
                ret[index] = i;
                index++;
            }
        }
        return ret;
    }


    public static byte[] getRandomPiece(BitSet received, BitSet mine){
        int randomNum;
        BitSet neededPieces = getNeededPieces(received, mine);
        int[] neededPieceIndexes = getIndecesOfInterest(neededPieces);
        randomNum = ThreadLocalRandom.current().nextInt(0, neededPieceIndexes.length + 1);
        System.out.println("random piece index is:" + randomNum);
        return ByteBuffer.allocate(4).putInt(randomNum).array();
    }

    private static void printBitfield(byte[] bytes, String s) {
        for (byte b : bytes) {
            s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + ", ";
        }
        System.out.println(s);
    }
}

