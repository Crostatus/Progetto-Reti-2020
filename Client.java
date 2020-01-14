import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    private  String EOM="_EOM";
    private  int CHUNKSIZE = 128;
    private InetSocketAddress address;
    private SocketChannel client;
    private ByteBuffer buffer;
    private String nickname;
    private int registerPort;
    private int tcpPort;

    public Client(int registerPort, int tcpPort){
        this.registerPort=registerPort;
        this.tcpPort = tcpPort;

    }

    /** operazione per registrare l'utente */
    public boolean registra(String nickname, String password) throws NotBoundException,IOException {
        Registry reg = LocateRegistry.getRegistry(registerPort);
        this.nickname = nickname;
        RegistrazioneInterface registra = (RegistrazioneInterface) reg.lookup("iscrizione");
        int risultato=registra.registra_utente(nickname, password);
        if(risultato==0) {
            System.out.println("Operazione avvenuta con successo");
            return true;
        }
        else if(risultato==1)
            System.out.println("Nickname già presente");
        else
            System.out.println("Password non valida, errore: "+risultato);
        return false;

    }

    /** operazione per loggare l'utente */
    public boolean login(String nickname, String password) throws IOException {
        address = new InetSocketAddress("localhost",tcpPort);
        client = SocketChannel.open(address);
        buffer = ByteBuffer.allocate(CHUNKSIZE);
        String messaggio = "login " + nickname + " " + password + EOM;
        byte[] data = messaggio.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println("Ho inviato: " + messaggio);
        buffer.clear();

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

        System.out.println("Risposta: " + result);
        return true;
    }

    /** operazione per sloggare l'utente*/
    public void logout()throws IOException{
        String richiesta = "logout " + nickname + EOM;
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println("Ho inviato: " + richiesta);
        buffer.clear();

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

        System.out.println("Risposta: " + result);

        // la close la devo fare solo quando effettuo l'operazione di logout
        client.close();
    }
}
