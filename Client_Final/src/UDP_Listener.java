import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

/*
OVERVIEW: Un UDP_Listener è un tipo di thread dedicato alla ricezione di richieste di sfida tramite UDP.
          Quando un utente registrato al servizio effettua una operazione di login, questo thread viene
          avviato dal thread Client. Ogni pacchetto UDP di sfida viene elaborato, e se la sfida supera
          i controlli di validita (es: l' utente che riceve la sfida non è impegnato in un altra sfida)
          risponde al server l' esito deciso dall' utente sfidato e, in caso positivo, anche il thread Client.
 */

public class UDP_Listener extends Thread{

    private DatagramSocket clientSocket;
    private AtomicBoolean inAMatch;
    private String nickname;
    private SocketChannel client;
    private ByteBuffer buffer;
    private  String EOM="_EOM";

    private ChallengeFrame challengeFrame;
    private String challengeMessage;
    private InetAddress destAddress;
    private int destPort;
    private ClientUI clientUI;


    public UDP_Listener(AtomicBoolean bool,int udp, String nickname, SocketChannel client, ClientUI clientUI)throws SocketException {
        this.clientSocket = new DatagramSocket(udp);
        this.buffer = ByteBuffer.allocate(64);
        this.clientUI = clientUI;
        this.client = client;
        this.inAMatch = bool;
        this.nickname = nickname;
        System.out.println(nickname+" " + inAMatch.get());
    }

   public String getChallengeMessage(){
       return challengeMessage;
   }
    //Ciclo che rimane in ascolto, finchè non viene interrotto dal thread Client, di pacchetti UDP di sfida.
    public void run() {
        byte [] buffer = new byte[100];
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        while (!Thread.currentThread().isInterrupted()){
            try {
                clientSocket.receive(packet);
                if(!inAMatch.get()) {
                    destAddress = packet.getAddress();
                    destPort = packet.getPort();
                    System.out.println(packet.getPort());
                    buffer = packet.getData();
                    challengeMessage = new String(buffer, 0, packet.getLength());
                    //Creazione finestra con richiesta di sfida
                    challengeFrame = new ChallengeFrame(this);
                }else
                    System.out.print(" Sono già in una sfida");
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        clientSocket.close();
    }

    //Invio messaggio al server tramite UDP. Il primo messaggio TCP dopo l'invio della mia risposta al server
    // viene elaborato qui e, in caso di inizio partita, aggiornati elementi della UI dell' utente.
    public void inviaRisposta(String risposta, String friendNickname)throws IOException{
        byte[] buffer1 = risposta.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer1, buffer1.length, destAddress, destPort);
        clientSocket.send(packet);

        System.out.println("Messaggio UDP: " + challengeMessage);
        if (risposta.equals("si")) {
            inAMatch.set(true);
            risposta = riceviRisposta();
            if(risposta.contains("Via alla sfida di traduzione:")){
                //Aggiornamento elementi UI
                clientUI.switchToGamePage();
                clientUI.getResultPanel().setStartTimer();
                GamePanel gamePanel = clientUI.getGamePanel();
                gamePanel.setInfo(nickname,friendNickname);
                risposta = risposta.replace("Via alla sfida di traduzione:","");
                StringTokenizer token = new StringTokenizer(risposta);
                risposta = token.nextToken();
                gamePanel.setNewWord(risposta);
                return;
            }
            else {
                // Ho ricevuto una stringa di errore
                inAMatch.set(false);
            }
        }
    }

    // Metodo per la ricezione e formattazione risposta TCP dal server.
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

        System.out.println("[" + nickname + "] Nuova parola da tradurre: " + result);
        return result;
    }
}