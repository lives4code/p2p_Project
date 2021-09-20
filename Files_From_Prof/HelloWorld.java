import java.io.*;
public class HelloWorld {
    public static void main(String[] args)
    {
        byte[] bytes = new byte[32];
        String hexString = "P2PFILESHARINGPROJ";
        byte[] byteString = hexString.getBytes();
        for(int i = 0; i < byteString.length; i++){
            bytes[i] = byteString[i];
        }
        for(int i=18; i < 27; i++){
            bytes[i] = 0x00;
        }
        bytes[29] = 0x00;
        bytes[29] = 0x04;
        bytes[30] = 0x02;
        bytes[31] = 0x04;

        System.out.println(bytes.length);
        System.out.println(bytes.toString());
        //bytes
        //File.WriteAllBytes("input.txt", StringToByteArray(hexString));
        //obviously this is the absolute path and will need to be replaced with the relative path.
        File file = new File("input.txt");
        try{
            OutputStream outputStream = new FileOutputStream("./input.txt");
            outputStream.write(bytes);
        }
        catch (IOException e){
            System.out.println("error");
        }

    }
}