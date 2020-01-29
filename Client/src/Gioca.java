import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Scanner;

public class Gioca extends Thread {
    private InetAddress serverAddress;
    String nickName;
    private  String EOM="_EOM";
    private SocketChannel client;
    private ByteBuffer buffer;

    public Gioca(InetAddress serverAddress, SocketChannel client, String nickName) throws IOException {
        buffer = ByteBuffer.allocate(64);
        this.nickName = nickName;
        this.serverAddress = serverAddress;

    }
    public void run(){

        int i = 0;
        System.out.println("IL GIOCO E' PARTITO");
        for(i=0; i<3; i++){
            try {
                riceviRisposta();
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
            Scanner scanner = new Scanner(System.in);
            String parolaTradotta = scanner.nextLine();
            try {
                inviaRichiesta(parolaTradotta + EOM);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void inviaRichiesta(String richiesta) throws IOException {
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println("Ho inviato: " + richiesta);
        buffer.clear();
    }

    // riceve la risposta dal server
    private String riceviRisposta() throws IOException {
        String result="";
        int bytesRead = 0;
        byte[] data2;
        while(!result.endsWith(EOM)){
            bytesRead = client.read(buffer);
            System.out.println("numero byte letti: "+bytesRead);
            buffer.flip();
            data2 = new byte[bytesRead];
            buffer.get(data2);
            result+= new String(data2);
            buffer.clear();
        }
        result = result.replace("_EOM", "");

        System.out.println("[" + nickName + "] Nuova parola da tradurre: " + result);
        return result;
    }
}
