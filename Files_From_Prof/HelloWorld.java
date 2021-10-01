import java.io.*;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.Scanner; // Import the Scanner class to read text files

public class HelloWorld {
    public static byte[] createHandshake () {
        byte[] bytes = new byte[32];
        String hexString = "P2PFILESHARINGPROJ";
        byte[] byteString = hexString.getBytes();
        for (int i = 0; i < byteString.length; i++) {
            bytes[i] = byteString[i];
        }
        for (int i = 18; i < 27; i++) {
            bytes[i] = 0x00;
        }



        //bytes
        //File.WriteAllBytes("input.txt", StringToByteArray(hexString));
        //obviously this is the absolute path and will need to be replaced with the relative path.

        try {
            File info = new File("./project_config_file_large/project_config_file_large/PeerInfo.cfg");
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

        } catch (FileNotFoundException e) {
            System.out.println("error");
            e.printStackTrace();
        }
        return bytes;

    }
    public static void main(String[] args) {
        byte[] bytes = new byte[32];
        bytes = createHandshake();
        System.out.println(bytes.length);
        System.out.println(bytes.toString());


    }
}