import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

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
    public static byte[] receiveMessage(DataInputStream in, byte[] msg) {
        try{
            in.read(msg);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        return msg;
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

//        System.out.println("CREATE HANDSHAKE DEBUG: ");
//        for (byte b : bytes) {
//            System.out.format("0x%x ", b);
//        }

        return bytes;
    }

    public static boolean validateHandshake(byte[] msg, int peerId) {
        byte[] header = ("P2PFILESHARINGPROJ").getBytes();
        // Check for appropriate header
        if (!Arrays.equals(header, Arrays.copyOfRange(msg, 0, 18))) {
            return false;
        }
        // Check for zeros
        for (int i = 18; i < 28; i++) {
            if (msg[i] != 0x00)
                return false;
        }
        // Check for peer id
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++){
            b[i] = msg[i + 28];
        }

//        System.out.println("SERVER DEBUG: msg id :" + ByteBuffer.wrap(b).getInt());
//        System.out.println("SERVER DEBUG: msg id :" +
//                Byte.toUnsignedInt(b[0]) +
//                Byte.toUnsignedInt(b[1]) +
//                Byte.toUnsignedInt(b[2]) +
//                Byte.toUnsignedInt(b[3]));

        return ByteBuffer.wrap(b).getInt() == peerId;
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
    public static void handleMessage(byte[] msg) {
        byte[] msgLength  = new byte[4];
        System.arraycopy(msg, 0, msgLength, 4, 4);
        int mLength = ByteBuffer.wrap(msgLength).getInt();

        byte[] msgType = new byte[1];
        System.arraycopy(msg, 4, msgType, 5, 1);
        int mType = ByteBuffer.wrap(msgType).getInt();

        byte[] msgPayload = new byte[mLength];
        System.arraycopy(msg, 5, msgType, mLength + 5, mLength);

        switch(mType){
            case 0:
                //choke
            case 1:
                //unchoke
            case 2:
                //interested
            case 3:
                //not intrested
            case 4:
                //have
            case 5:
                //bitfield
            case 6:
                //request
            case 7:
                //piece
        }
    }
}
