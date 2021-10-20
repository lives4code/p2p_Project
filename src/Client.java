import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Client extends Thread {

    private Socket requestSocket;           //socket connect to the server
    private String message;                //message send to the server
    private String MESSAGE;                //capitalized message read from the server
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket

    public Client() {}

    public void run()
    {
        try{
            //create a socket to connect to the server
            requestSocket = new Socket("localhost", 8000);
            System.out.println("Connected to localhost in port 8000");
            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                System.out.print("Hello, please input a sentence: ");
                //read a sentence from the standard input
                message = bufferedReader.readLine();
                //Send the sentence to the server
                sendMessage(message);
                //Receive the upperCase sentence from the server
                MESSAGE = (String)in.readObject();
                //show the message to the user
                System.out.println("Receive message: " + MESSAGE);
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    //send a message to the output stream
    private void sendMessage(String msg)
    {
        try{
            //stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private static byte[] createHandshake(int peerID) {
        byte[] bytes = new byte[32];
        String hexString = "P2PFILESHARINGPROJ";
        byte[] byteString = hexString.getBytes();
        for (int i = 0; i < byteString.length; i++) {
            bytes[i] = byteString[i];
        }
        for (int i = 18; i < 27; i++) {
            bytes[i] = 0x00;
        }
        bytes[28] = (byte) (peerID >> 24);
        bytes[29] = (byte) (peerID >> 16);
        bytes[30] = (byte) (peerID >> 8);
        bytes[31] = (byte) (peerID /*>> 0*/);


        //File.WriteAllBytes("input.txt", StringToByteArray(hexString));
        /*
        try {
            File info = new File("../Files_From_Prof/project_config_file_large/project_config_file_large/PeerInfo.cfg");
            Scanner myReader = new Scanner(info);
            if (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                for (int i = 0; i < 4; ++i) {
                    byte l = (byte) Character.getNumericValue(data.charAt(i));
                    System.out.println("data:" + data.charAt(i));
                    bytes[28 + i] = l;
                }
            }
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                System.out.println(data);
            }
            myReader.close();

        }
        catch (FileNotFoundException e) {
            System.out.println("error");
            e.printStackTrace();
        }

         */
        return bytes;
    }
}