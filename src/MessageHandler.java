import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

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
        return bytes;
    }

    //handle message
    public static byte[] handleMessage(byte[] msg, int type, int clientId, int myId, char s) {
        switch(type){
            case -1:
                //break;
            case 0:
                //choke
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 0 message, choke");
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setChoked(true);
                break;
            case 1:
                //unchoke
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 1 message, unchoke");
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setChoked(false);
                break;
            case 2:
                //interested
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 2 message, interested");
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setInterested(true);
                return null;
            case 3:
                //not intrested
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 3 message, not interested clientID: " + clientId);
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).setInterested(false);
                return null;
            case 4:
                //have
                //first update the bitfield to reflect the new piece.
                //then return either an interested or not interested method after comparison.
                int i = ByteBuffer.wrap(msg).getInt();
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField.set(i);
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 4 message, have");
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
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 5 message, bitfield");
                if(msg.length == 0){
                    System.out.println("MES HANDLER " + s  + myId + ": bitfield is empty.");
                    BitSet b = new BitSet(MyProcess.bitField.size());
                    b.clear();
                    MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField = b;
                    System.out.println("MES HANDLER " + s  + myId + ": empty- size: " +b.size());
                    return createUninterestedMessage();
                }
                printBitfield(msg, "MES HANDLER " + s  + myId + ": printing bitfield given. ");
                MyProcess.peers.get(MyProcess.getPeerIndexById(clientId)).bitField = BitSet.valueOf(msg);
                if(checkForInterest(BitSet.valueOf(msg), MyProcess.bitField)){
                    return createInterestedMessage();
                }
                else {
                    return createUninterestedMessage();
                }
            case 6:
                //request
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 6 message, request");
                return createPieceMessage(msg);
            case 7:
                //piece
                System.out.println("MES HANDLER " + s  + myId + ": Handling type 7 message, piece");
                byte[] pieceIndex = new byte[4];
                for( i =0; i < 4; i++){
                    pieceIndex[i] = msg[i];
                }
                MyProcess.bitField.set(ByteBuffer.wrap(pieceIndex).getInt());
                byte[] arr = Arrays.copyOfRange(msg, 4, msg.length);
                MyProcess.writePiece(pieceIndex,arr);
                return createPieceMessage(pieceIndex);
            default:
                // invalid
                System.out.println("MES HANDLER " + s  + myId + "invalid type " + type);
                break;
        }
        return msg;
    }

    public static boolean checkForInterest(BitSet received, BitSet mine) {
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

    private static void printBitfield(byte[] bytes, String s) {
        for (byte b : bytes) {
            s += "0x" + Integer.toHexString(Byte.toUnsignedInt(b)).toUpperCase() + ", ";
        }
        System.out.println(s);
    }
}

