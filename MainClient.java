import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class MainClient {
    public static void main(String[] args) throws Exception, NotBoundException {
        Registry reg = LocateRegistry.getRegistry(8080);
        RegistrazioneInterface registra = (RegistrazioneInterface) reg.lookup("iscrizione");
        int risultato=registra.registra_utente("bastianich11", "ciaociao98");
        if(risultato==0)
            System.out.println("Operazione avvenuta con successo");
        else if(risultato==1)
            System.out.println("Nickname già presente");
        else
            System.out.println("Password non valida, errore: "+risultato);

        InetSocketAddress address = new InetSocketAddress("localhost", 6666);
        SocketChannel client = SocketChannel.open(address);
        ByteBuffer buffer = ByteBuffer.allocate(512);
        String messaggio = "login bastianich11 ciaociao98";
        byte[] data = messaggio.getBytes();
        buffer.put(data);
        buffer.flip();
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println("Ho inviato: " + messaggio);
        buffer.clear();
        client.close();


    }


    /*InetSocketAddress address = new InetSocketAddress(hostName,port);
        SocketChannel client = SocketChannel.open(address);
        ByteBuffer buffer=ByteBuffer.allocateDirect(256);

        // Mando il messaggio al server
        buffer.put(data.getBytes());
        buffer.flip();
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println("Ho inviato: "+data);
        buffer.clear();
        Thread.sleep(100);

        // Leggo dal server
        int num=client.read(buffer);
        byte []data=new byte[num];
        buffer.flip();
        buffer.get(data);
        System.out.println("Ho ricevuto: "+new String(data));
        buffer.clear();
        client.close();
        */


}
