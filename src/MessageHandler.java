import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class MessageHandler {

    public static void sendMessage(DataOutputStream out, byte[] msg) {
        try{
            out.write(msg, 0, msg.length);
            out.flush();
        } catch (SocketException e){
            System.out.println("ERROR IN MES HANDLER SEND MESSAGE: SocketException. likely broken pipe");
        } catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    public static void sendMessage(DataOutputStream out, byte[] msg, int myId) {
        try{
            out.write(msg, 0, msg.length);
            out.flush();
        } catch (SocketException e){
            System.out.println("ERROR MES HANDLER SEND MESSAGE " + myId + ": SocketException. likely broken pipe");
        } catch(IOException ioException){
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
        byte[] payload = createMsg(7, MyProcess.readPiece(pieceIndex));
        byte[] index = new byte[4];
        for(int i = 0; i < 4; i++){
            index[i] = payload[i + 5];
        }
        System.out.println("for piece message index is:" + ByteBuffer.wrap(index).getInt());
        return payload;
    }

    public static byte[] createHaveMessage(byte[] pieceIndex){
        return createMsg(4, pieceIndex);
    }

    public static byte[] createRequestMessage(byte[] pieceIndex){
        byte[] payload = createMsg(6, pieceIndex);
        byte[] index = new byte[4];
        for(int i = 0; i < index.length; i++){
            index[i] = payload[i + 5];
        }
        System.out.println("for request message wrapped payload is: " + ByteBuffer.wrap(index).getInt());

        return payload;
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

    public static int validateHandshake(byte[] msg) throws Exception {
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
        System.out.println("MES HANDLER " + clientOrServer + " " + myId + ": Handling type " + type + " message operation: " + operation + " from: " + clientId);
    }

    public static byte[] handleMessage(byte[] msg, int type, int clientId, int myId, char s, Logger log) {
        //Peer peer = MyProcess.peers.get(MyProcess.getPeerIndexById(clientId));
        switch(type){
            case -1:
                //break;
                return null;
            case 0:
                //choke
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).chokeSelf();
                printMessageHandlerDebug(0, clientId, myId, s);
                log.info("Peer " + myId + " is 'choked' by " + clientId + ". " + s);
                return null;
            case 1:
                //unchoke
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).unchokeSelf();
                printMessageHandlerDebug(1, clientId, myId, s);
                log.info("Peer " + myId + " is 'unchoked' by " + clientId + ".");
                if(MessageHandler.checkForInterest(MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField, MyProcess.bitField)) {
                    return createRequestMessage(MessageHandler.getRandomPiece(MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField, MyProcess.bitField));
                }
                return null;
            case 2:
                //interested
                printMessageHandlerDebug(2, clientId, myId, s);
                log.info("Peer " + myId + " received the 'interested' message from " + clientId + ".");
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setInterested(true);
                return null;
            case 3:
                //not intrested
                printMessageHandlerDebug(3, clientId, myId, s);
                log.info("Peer " + myId + " received the 'not interested' message from " + clientId + ".");
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setInterested(false);
                return null;
            case 4:
                //have
                //first update the bitfield to reflect the new piece.
                //then return either an interested or not interested method after comparison.
                int integer = ByteBuffer.wrap(msg).getInt();
                log.info("Peer " + myId + " received the 'have' message from " + clientId + " for the piece " + integer + ".");
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField.set(integer);
                printMessageHandlerDebug(4, clientId, myId, s);
                if(MessageHandler.checkForInterest(MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField, MyProcess.bitField)){
                    return createInterestedMessage();
                }
                else {
                    return createUninterestedMessage();
                }
            case 5:
                //bitfield
                printMessageHandlerDebug(5, clientId, myId, s);
                log.info("Peer " + myId + " received 'bitfield' from " + clientId + ".");
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
                byte[] pieceIndexArray = new byte[4];
                for( int i =0; i < 4; i++){
                    pieceIndexArray[i] = msg[i];
                }
                int pieceInd = ByteBuffer.wrap(pieceIndexArray).getInt();
                log.info("Peer " + myId + " received a 'request' from " + clientId + " for piece " + pieceInd + ".");
                return createPieceMessage(msg);
            case 7:
                //piece
                printMessageHandlerDebug(7, clientId, myId, s);
                byte[] pieceIndexArr = new byte[4];
                for( int i =0; i < 4; i++){
                    pieceIndexArr[i] = msg[i];
                }
                int pieceIndex = ByteBuffer.wrap(pieceIndexArr).getInt();

                // save new piece
                byte[] arr = Arrays.copyOfRange(msg, 4, msg.length);
                MyProcess.writePiece(pieceIndexArr,arr);

                // log
                int numPieces = 0;
                for(int i = 0; i < MyProcess.bitField.size(); i++){
                    if(MyProcess.bitField.get(i))
                        numPieces++;
                }
                log.info("Peer " + myId + " has downloaded the 'piece' " + pieceIndex + " from " + clientId + ". Now the number of pieces it has is " + numPieces + ".");

//<<<<<<< Updated upstream
                // keeping sending requests if still unchoked and interested
                boolean shouldBeInterested = MessageHandler.checkForInterest(MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField, MyProcess.bitField);
                if(!MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).amIChoked() && shouldBeInterested) {
//=======
//                if(MessageHandler.checkForInterest(peer.bitField, MyProcess.bitField) ) {
//>>>>>>> Stashed changes
                    return createRequestMessage(MessageHandler.getRandomPiece(MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField, MyProcess.bitField));
                }
                // no longer interested
                else if (!shouldBeInterested) {
                    return createUninterestedMessage();
                }
                // do nothing if choked
                return null;
            case 8:
                // server complete tell client to stop
                System.out.println("CLIENT " + myId + ": received kill request");
                return msg;
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
            for (int i = 0; i < input.size(); i++) {
                ret.set(i, input.get(i));
                ret.flip(i);
            }
        }
        return ret;
    }

    public static BitSet getNeededPieces(BitSet received, BitSet mine){
        if(mine == null || mine.isEmpty()){
            mine = new BitSet(received.length());
        }
        if(received == null || received.isEmpty()){
            received = new BitSet(mine.length());
        }
        BitSet comp = getComplement(mine);
        comp.and(received);
        return comp;
    }

    public static boolean checkForInterest(BitSet received, BitSet mine) {
        BitSet comp = getNeededPieces(received, mine);
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
        int index = 0;
        int size = 0;
        for(int i = 0; i < input.size(); i++){
            if(input.get(i)){
                size++;
            }
        }
        int[] ret = new int[size];
        for (int i =0; i < input.size(); i++){
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
        System.out.println("get random piece needed pieces"  + neededPieces);
        int[] neededPieceIndexes = getIndecesOfInterest(neededPieces);
        if(neededPieceIndexes.length == 0){
            System.out.println("don't need anymore pieces.");
        }
        randomNum = ThreadLocalRandom.current().nextInt(0, neededPieceIndexes.length);
        System.out.println("get random piece returns"  + neededPieceIndexes[randomNum]);
        return ByteBuffer.allocate(4).putInt(neededPieceIndexes[randomNum]).array();
    }
}

