import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDP_Listener extends Thread{
    private DatagramSocket clientSocket;
    private AtomicBoolean inAMatch; //true => il client sta facendo una sfida. || false altrimenti


    public UDP_Listener( AtomicBoolean playerStatus, int udp)throws SocketException {
        this.clientSocket = new DatagramSocket(udp);
        inAMatch = playerStatus;
    }

    public void run() {
        byte [] buffer = new byte[100];
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        String message = null;
        while (!Thread.currentThread().isInterrupted()){
            try {
                clientSocket.receive(packet);
                InetAddress serverAddress = packet.getAddress();
                int serverPort = packet.getPort();
                System.out.println(packet.getPort());
                buffer = packet.getData();
                message = new String(buffer);
                if(!inAMatch.get())
                   System.out.println("Richiesta rivevuta: " + message);
                String risposta="si";
                if(risposta.equals("si"))
                    inAMatch.set(true);
                byte[] buffer1 = risposta.getBytes();
                packet = new DatagramPacket(buffer1, buffer1.length, serverAddress, serverPort);
                clientSocket.send(packet);


            System.out.println("Messaggio UDP: "+message);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        clientSocket.close();
    }

}


