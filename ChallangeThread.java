import java.io.IOException;
import java.net.*;

/*public class ChallangeThread extends Thread {
    private int UDP_PORT ;
    private DatagramSocket serverSocket;
    private String sfidante;
    private InetAddress indirizzoSfidato;

    public ChallangeThread(String sfidante, InetAddress indirizzoSfidato, int UDP_PORT) throws SocketException {
        serverSocket = new DatagramSocket();
        this.UDP_PORT = UDP_PORT;
        serverSocket.setSoTimeout(10000);

    }

    public void run(){
        String richiesta = sfidante + " ti vuole sfidare!";
        DatagramPacket packet;
        byte[] data = richiesta.getBytes();
        packet = new DatagramPacket(data, data.length, indirizzoSfidato, UDP_PORT);
        try {
            serverSocket.send(packet);
            serverSocket.receive(packet);
        }
        catch (SocketTimeoutException noAnswer){
            Thread.currentThread().interrupt();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        data = packet.getData();
        String risposta = new String(data);
        System.out.println(risposta);



    }


}*/
