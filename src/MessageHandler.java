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
    public static void receiveHandshake(DataInputStream in, byte[] msg) {
        try{
            in.read(msg, 0, 32);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
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
        //still not really checking id.
        int id = ByteBuffer.wrap(b).getInt();
        if(peerId != id){
            throw new Exception("id is not equal to peerID");
        }
            //so I'm thinking of maybe jsut seeing if it's one of the legal cases 1001 - 1008
        // and returning the result of it just being one of the possible values.
        //the alternative is to get the ip adress from the packet and use a map to compate whether the key value pairs are equal.

//        System.out.println("SERVER DEBUG: msg id :" + ByteBuffer.wrap(b).getInt());
//        System.out.println("SERVER DEBUG: msg id :" +
//                Byte.toUnsignedInt(b[0]) +
//                Byte.toUnsignedInt(b[1]) +
//                Byte.toUnsignedInt(b[2]) +
//                Byte.toUnsignedInt(b[3]));
        return id;

        // this doesn't work. return ByteBuffer.wrap(b).getInt() == peerId;
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
    public static byte[] handleMessage(DataInputStream in) {

        byte[] msg = null;
        byte[] sizeB = new byte[4];
        int type = -1; // <- wut
        try{

            in.read(sizeB);
            int size = ByteBuffer.wrap(sizeB).getInt();
            msg = new byte[size];
            type = in.read();
            in.read(msg);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }

        switch(type){
            case 0:
                //choke
                break;
            case 1:
                //unchoke
                break;
            case 2:
                //interested
                break;
            case 3:
                //not intrested
                break;
            case 4:
                break;
                //have
            case 5:
                return msg;
                //bitfield
            case 6:
                break;
                //request
            case 7:
                //piece
                break;
            default:
                System.out.println("invalid type " + type);
                break;
        }
        return msg;
    }
}
