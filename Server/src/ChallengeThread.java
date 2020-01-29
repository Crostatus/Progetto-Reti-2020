import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class ChallengeThread extends Thread{
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";
    private ArrayList<String> traduzioneSfidante;
    private ArrayList<String> traduzioneSfidato;
    private int indiceSfidante;
    private int indiceSfidato;
   private DatagramSocket DSocket;
   private OnlineList.OnlineUser sfidante;
   private OnlineList.OnlineUser sfidato;
    //private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private  String EOM="_EOM";
    private boolean sfidaAvviata;
    private ArrayList<String> randomWords;
   public ChallengeThread(OnlineList.OnlineUser sfidante, OnlineList.OnlineUser sfidato, ArrayList<String> randomWords)throws SocketException {
       this.sfidato = sfidato;
       this.indiceSfidante = 0;
       this.indiceSfidato = 0;
       this.traduzioneSfidante  = new ArrayList<>();
       this.traduzioneSfidato = new ArrayList<>();
       this.randomWords = randomWords;
       this.sfidante = sfidante;
       this.sfidaAvviata = true;
       int port = sfidante.getUserChannel().socket().getLocalPort();
       //System.out.println(port);
       DSocket = new DatagramSocket(port);
       // controllare caso in cui la porta del datagramPacket è uguale ad un altra porta della sfida
       DSocket.setSoTimeout(10000);
   }

   public void run(){
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
           // controllo che vada tutto bene
           System.out.println("Timeout scaduto");
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
           risposta = "Richiesta di sfida accettata"+EOM;
           SocketChannel channelSfidante = sfidante.getUserChannel();
           try{
               channelSfidante.register(selector,SelectionKey.OP_WRITE,new ReadingByteBuffer(risposta));
           }catch (ClosedChannelException e){
               e.printStackTrace();
           }

           try {
               startThreadSfida();
           } catch (IOException e) {
               e.printStackTrace();
           }

           risposta = "Via alla sfida di traduzione:\n"+randomWords.get(0)+EOM;
           ArrayList<String> paroleTradotte = null;
           SocketChannel channelSfidato = sfidato.getUserChannel();
           SelectionKey keySfidato = sfidato.getKey();
           keySfidato.interestOps(0);
           try {
               paroleTradotte = traduci();
               channelSfidato.register(selector, SelectionKey.OP_WRITE, new ReadingByteBuffer(risposta));

           } catch (ClosedChannelException e) {
               e.printStackTrace();
           } catch (InterruptedException e){
               e.printStackTrace();
           } catch (ParseException e){
               e.printStackTrace();
           } catch (IOException e){
               e.printStackTrace();
           }

       }else {
           try {
               responseAndTerminate();
           } catch (IOException e) {

               e.printStackTrace();
           }
           return;
       }


   }

   private void responseAndTerminate()throws IOException{
       String risposta = "Richiesta di sfida non accettata"+EOM;
       SocketChannel channelSfidante = sfidante.getUserChannel();

       sfidante.getKey().interestOps(SelectionKey.OP_READ);
       // sveglio il selettore del server e gli dico che ha una nuova
       // chiave pronta per una lattura
       sfidante.getKey().selector().wakeup();
       System.out.println(" interestOps messo a READ " + sfidante.getKey().isReadable());
       ByteBuffer buffer = ByteBuffer.wrap(risposta.getBytes());
       channelSfidante.write(buffer);
       sfidaAvviata = false;
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
                       readRequest(currentKey);
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

   private void readRequest(SelectionKey newReadRequest)throws IOException{
       SocketChannel client = (SocketChannel) newReadRequest.channel();
       ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
       attachment.getByteBuffer().clear();
       int num = client.read(attachment.getByteBuffer());
       System.out.println("numero byte letti: "+num);

       //vedo quando il client termina improvvisamente
       if(num==-1)throw new IOException();

       // Entro dentro quando ho finito di leggere
       if(attachment.updateOnRead()) {
           String risposta = attachment.getMessage()+EOM;
           attachment.clear();
           System.out.println(GREEN+"Messaggio arrivato al server: " + risposta +RESET);
           if(sfidante.getKey().equals(newReadRequest)) {
               indiceSfidante++;
               traduzioneSfidante.add(risposta);
               if(indiceSfidante < 3)
                   attachment.updateMessagge(randomWords.get(indiceSfidante)+EOM);
           }
           else {
               indiceSfidato++;
               traduzioneSfidato.add(risposta);
               if(indiceSfidato < 3)
                   attachment.updateMessagge(randomWords.get(indiceSfidato) + EOM);
           }

           newReadRequest.interestOps(SelectionKey.OP_WRITE);
           attachment.clear();
       }

   }

   private void writeRequest(SelectionKey currentKey)throws IOException{
       SocketChannel client = (SocketChannel) currentKey.channel();
       ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
       String nuovaParola=attachment.getMessage();
       System.out.println("Parola da mandare: " + nuovaParola);
       System.out.println("BB: " + attachment.getByteBuffer());
       ByteBuffer buffer=attachment.getByteBuffer();

       buffer=ByteBuffer.wrap(nuovaParola.getBytes());
       while(buffer.hasRemaining()){
           //System.out.println("-----sto scrivendo e sono il thread");
           client.write(buffer);
       }
       if(nuovaParola.contains("Richiesta di sfida accettata")){
           attachment.updateMessagge("Via alla sfida di traduzione:\n"+randomWords.get(0)+EOM);
           attachment.clear();
           currentKey.interestOps(SelectionKey.OP_WRITE);
           return;
       }

       attachment.updateMessagge("");
       attachment.clear();

       System.out.println(BLUE+"Messaggio che invia il ThreadSfida: "+nuovaParola+RESET);
       currentKey.interestOps(SelectionKey.OP_READ);
   }

   private ArrayList<String> traduci()throws IOException,InterruptedException, ParseException {
       ArrayList<String> paroleTradotte = new ArrayList<>();
       BufferedReader in = null;
       URL url = null;
       HttpURLConnection connection = null;
       JSONParser parser = new JSONParser();
       JSONObject jsonObject = null;

       for(int i = 0; i < 3; i++){
           System.out.println("Parola inviata: " + randomWords.get(i));
           url = new URL("https://api.mymemory.translated.net/get?q="+ randomWords.get(i) + "&langpair=it|en");
           connection = (HttpURLConnection) url.openConnection();
           connection.setDoOutput(true);
           PrintWriter out = new PrintWriter(connection.getOutputStream());
           out.close();
           in = new BufferedReader( new InputStreamReader(connection.getInputStream()));
           String inputLine,risposta="";
           while((inputLine = in.readLine()) != null) {
               risposta +=inputLine;
               System.out.println(inputLine);
           }

           jsonObject = (JSONObject) parser.parse(risposta);
           jsonObject = (JSONObject) jsonObject.get("responseData");
           //System.out.println(" responseData: "+jsonObject.toJSONString());
           risposta = (String) jsonObject.get("translatedText");
           //System.out.println("La risposta è: " + risposta);
           paroleTradotte.add(risposta);

       }

       in.close();
       connection.disconnect();

       return paroleTradotte;

   }

}
