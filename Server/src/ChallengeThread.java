import java.io.IOException;
import java.net.*;

public class ChallengeThread extends Thread{
   private DatagramSocket DSocket;
   private OnlineList.OnlineUser sfidante;
   private OnlineList.OnlineUser sfidato;

   public ChallengeThread(OnlineList.OnlineUser sfidante, OnlineList.OnlineUser sfidato)throws SocketException {
       this.sfidato = sfidato;
       this.sfidante = sfidante;
       int port = sfidante.getUserChannel().socket().getLocalPort();
       System.out.println(port);
       DSocket = new DatagramSocket(port);
       // controllare caso in cui la porta del datagramPacket è uguale ad un altra porta della sfida
       DSocket.setSoTimeout(10000);
   }

   public void run(){
       System.out.println("RUN");
       String richiesta = sfidante.getNickname()+" ti vuole sfidare! Vuoi accettare, si o no?";
       System.out.println("Thread Sfida invia: "+richiesta);
       byte[] dati= richiesta.getBytes();

       int porta = sfidato.getUserChannel().socket().getPort();
       System.out.println("     Porta: "+porta);
       DatagramPacket packet = new DatagramPacket(dati, dati.length, sfidato.getUserChannel().socket().getInetAddress(), porta);
       try{
        DSocket.send(packet);
        DSocket.receive(packet);
        }
       catch (SocketTimeoutException e){
            Thread.currentThread().interrupt();
       }
       catch (IOException e){
           e.printStackTrace();
       }
       String risposta=null;
       dati = packet.getData();
       risposta = new String(dati, 0, packet.getLength());
       System.out.println("Pacchetto ricevuto: "+risposta);
       DSocket.close();
   }

}
