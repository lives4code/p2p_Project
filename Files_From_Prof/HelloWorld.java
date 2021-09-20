import java.io.*;
public class HelloWorld {
    public static void main(String[] args)
    {
        String hexString = "P2PFILESHARINGPROJ";
        byte[] bytes = hexString.getBytes();
        System.out.println(bytes.length);
        System.out.println(bytes.toString());
        //File.WriteAllBytes("input.txt", StringToByteArray(hexString));
        System.out.println("Hello, World!");
    }
}