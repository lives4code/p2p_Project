

public class ClientSpawn extends Thread {
    public void run(){
        //do for each peer in peer list
        new Client("localhost").start();
    }
}
