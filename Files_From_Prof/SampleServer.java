import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class SampleServer {

	private static final int sPort = 8000;   //The server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running."); 
        	ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
        	try {
            		while(true) {
                		new Handler(listener.accept(),clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
            			}
        	} finally {
            		listener.close();
        	} 
 
    	}

	/**
     	* A handler thread class.  Handlers are spawned from the listening
     	* loop and are responsible for dealing with a single client's requests.
     	*/
    	private static class Handler extends Thread {
        	private String message;    //message received from the client
		private String MESSAGE;    //uppercase message send to the client
		private Socket connection;
        	private ObjectInputStream in;	//stream read from the socket
        	private ObjectOutputStream out;    //stream write to the socket
		private int no;		//The index number of the client

        	public Handler(Socket connection, int no) {
            		this.connection = connection;
	    		this.no = no;
        	}

        public void run() {
 		try{
			//initialize Input and Output streams
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());
			try{
				while(true)
				{
					//receive the message sent from the client
					message = (String)in.readObject();
					//show the message to the user
					System.out.println("Receive message: " + message + " from client " + no);
					//Capitalize all letters in the message
					MESSAGE = message.toUpperCase();
					//send MESSAGE back to the client
					sendMessage(MESSAGE);
				}
			}
			catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
		}
		catch(IOException ioException){
			System.out.println("Disconnect with Client " + no);
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				connection.close();
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
			}
		}
	}

	//send a message to the output stream
	public void sendMessage(String msg)
	{
		try{
			out.writeObject(msg);
			out.flush();
			System.out.println("Send message: " + msg + " to Client " + no);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	
	private void handleMessage(byte[] msg){
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

}
