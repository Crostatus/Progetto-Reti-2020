import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDP_Listener extends Thread{
    private DatagramSocket clientSocket;
    private AtomicBoolean inAMatch; //true => il client sta facendo una sfida. || false altrimenti
    private String nickname;
    private SocketChannel client;
    private ByteBuffer buffer;
    private  String EOM="_EOM";
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";

    public UDP_Listener(AtomicBoolean bool,int udp, String nickname, SocketChannel client)throws SocketException {
        this.clientSocket = new DatagramSocket(udp);
        this.buffer = ByteBuffer.allocate(64);
        this.client = client;
        this.inAMatch = bool;
        this.nickname = nickname;
        System.out.println(nickname+" " + inAMatch.get());
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
                message = new String(buffer, 0, packet.getLength());
                if(!inAMatch.get()) {
                    String risposta = "si";
                    byte[] buffer1 = risposta.getBytes();
                    packet = new DatagramPacket(buffer1, buffer1.length, serverAddress, serverPort);
                    clientSocket.send(packet);


                    System.out.println("Messaggio UDP: " + message);
                    if (risposta.equals("si")) {
                        inAMatch.set(true);
                        gioca(serverAddress);
                    }
                }else
                    System.out.print(" Sono già in una sfida");
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        clientSocket.close();
    }

    private void gioca(InetAddress serverAddress)throws IOException{
        int i = 0;
        System.out.println(RED+"IL GIOCO E' PARTITO"+RESET);
        ByteBuffer buffer = ByteBuffer.allocate(64);
        for(i=0; i<3; i++){
           riceviRisposta();
           Scanner scanner = new Scanner(System.in);
           String parolaTradotta = scanner.nextLine();
           inviaRichiesta(parolaTradotta + EOM);
        }
    }
    // invia la richiesta al server
    private void inviaRichiesta(String richiesta) throws IOException {
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println(BLUE+"Ho inviato: " + richiesta+RESET);
        buffer.clear();
    }

    // riceve la risposta dal server
    private String riceviRisposta() throws IOException {
        String result="";
        int bytesRead = 0;
        byte[] data2;
        while(!result.endsWith(EOM)){
            bytesRead = client.read(buffer);
            buffer.flip();
            data2 = new byte[bytesRead];
            buffer.get(data2);
            result+= new String(data2);
            buffer.clear();
        }
        result = result.replace("_EOM", "");

        System.out.println(GREEN + "[" + nickname + "] Nuova parola da tradurre: " + result+RESET);
        return result;
    }

}


