import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDP_Listener extends Thread{
    private DatagramSocket clientSocket;
    private AtomicBoolean inAMatch; //true => il client sta facendo una sfida. || false altrimenti


    public UDP_Listener(int port, AtomicBoolean playerStatus)throws SocketException {
        this.clientSocket = new DatagramSocket(port);
        inAMatch = playerStatus;
    }

    public void run() {
        DatagramPacket receivePacket = null;
        byte [] buffer = null;
        String message = null;
        for(;;){
            try {
                clientSocket.receive(receivePacket);
                buffer = receivePacket.getData();
                message = new String(buffer);
                if(!inAMatch.get())
                   System.out.println("Richiesta rivevuta: " + message);

            System.out.println("Messaggio UDP: "+message);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}


