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
    public static byte[] handleMessage(byte[] msg, int type) {
        switch(type){
            case -1:
                //break;
            case 0:
                //choke
                System.out.println("MES HANDLER: Handling type 0 message");
                break;
            case 1:
                //unchoke
                System.out.println("MES HANDLER: Handling type 1 message");
                break;
            case 2:
                //interested
                System.out.println("MES HANDLER: Handling type 2 message");
                break;
            case 3:
                //not intrested
                System.out.println("MES HANDLER: Handling type 3 message");
                break;
            case 4:
                //have
                System.out.println("MES HANDLER: Handling type 4 message");
                break;
            case 5:
                //bitfield
                System.out.println("MES HANDLER: Handling type 5 message");
                return msg;
            case 6:
                //request
                System.out.println("MES HANDLER: Handling type 6 message");
                break;
            case 7:
                //piece
                System.out.println("MES HANDLER: Handling type 7 message");
                break;
            default:
                // invalid
                System.out.println("invalid type " + type);
                break;
        }
        return msg;
    }
}
