import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class ChallengeThread extends Thread{
   private DatagramSocket DSocket;
   private OnlineList.OnlineUser sfidante;
   private OnlineList.OnlineUser sfidato;
    //private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private  String EOM="_EOM";
    private boolean sfidaAvviata;

   public ChallengeThread(OnlineList.OnlineUser sfidante, OnlineList.OnlineUser sfidato)throws SocketException {
       this.sfidato = sfidato;
       this.sfidante = sfidante;
       this.sfidaAvviata = true;
       int port = sfidante.getUserChannel().socket().getLocalPort();
       //System.out.println(port);
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
       //System.out.println("     Porta: "+porta);
       DatagramPacket packet = new DatagramPacket(dati, dati.length, sfidato.getUserChannel().socket().getInetAddress(), porta);
       try{
        DSocket.send(packet);
        DSocket.receive(packet);
        }
       catch (SocketTimeoutException e){
           System.out.println("Timeoot scaduto");
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
       try {
           selector = Selector.open();
       } catch (IOException e) {
           e.printStackTrace();
       }
       if(risposta.equals("si")){
           risposta = "Via alla sfida di traduzione"+EOM;
           SocketChannel channelSfidato = sfidato.getUserChannel();
           SelectionKey keySfidato = sfidato.getKey();
           keySfidato.interestOps(0);
           try {
               channelSfidato.register(selector, SelectionKey.OP_WRITE, new ReadingByteBuffer(risposta));
           } catch (ClosedChannelException e) {
               e.printStackTrace();
           }
       }else {
           risposta = "Richiesta di sfida non accettata"+EOM;
       }

       try {
           SocketChannel channelSfidante = sfidante.getUserChannel();
           channelSfidante.register(selector,SelectionKey.OP_WRITE,new ReadingByteBuffer(risposta));
           startThreadSfida();
           //if(risposta.equals("si"))
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   private void startThreadSfida()throws IOException{
       while(sfidaAvviata){
           selector.select();

           Set<SelectionKey> selectedKeys = selector.selectedKeys();
           Iterator<SelectionKey> iterator = selectedKeys.iterator();

           while(iterator.hasNext()){
               SelectionKey currentKey = iterator.next();
               iterator.remove();
               try {
                   if (!currentKey.isValid())
                       continue;
                   if (currentKey.isReadable()) {
                       System.out.println("------READ REQUEST------");
                       //readRequest(currentKey);
                   }
                   if (currentKey.isWritable()) {
                       System.out.println("-------WRITE REQUEST------");
                       writeRequest(currentKey);
                   }
               }catch (IOException  e){
                   System.out.println("------TERMINE CONNESSIONE------");
                   SocketChannel client = (SocketChannel) currentKey.channel();
                   //listaUtentiOnline.printList();
                   currentKey.cancel();
                   client.close();
               }
           }
       }
   }

   private void writeRequest(SelectionKey currentKey)throws IOException{
       SocketChannel client = (SocketChannel) currentKey.channel();
       ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
       System.out.println(attachment.getMessage());
       String risposta=attachment.getMessage();
       ByteBuffer buffer=attachment.getByteBuffer();

       buffer=ByteBuffer.wrap(risposta.getBytes());
       while(buffer.hasRemaining()){
           System.out.println("-----sto scrivendo e sono il thread");
           client.write(buffer);
       }

       if(risposta.contains("Richiesta di sfida non accettata")){
           sfidante.getKey().interestOps(SelectionKey.OP_READ);
           // sveglio il selettore del server e gli dico che ha una nuova
           // chiave pronta per una lattura
           sfidante.getKey().selector().wakeup();
           System.out.println(" interestOps messo a READ " + sfidante.getKey().isReadable());
           currentKey.cancel();
           selector.close();
           sfidaAvviata = false;
           Thread.currentThread().interrupt();
       }

       attachment.updateMessagge("");

       System.out.println("Messaggio che invia il ThreadSfida: "+risposta);
   }

}
