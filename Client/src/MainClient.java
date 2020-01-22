import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.rmi.NotBoundException;

public class MainClient {


    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, ParseException {
        try{
            //DatagramSocket socket = new DatagramSocket(9999);
            Client test = new Client(8080, 6666);
            Client test1 = new Client(8080, 6666);
            test.login("alessandro", "prova98");
            test1.login("andrea", "prova98");
            test.lista_amici("alessandro");
            /*System.out.println("linea 15");
            Client test1 = new Client(8080, 6666);
            System.out.println("linea 17");
            test1.login("andrea", "prova98");
            System.out.println("linea 19");
            test1.sfida("andrea","alessandro");*/
            test.sfida("alessandro", "andrea");
            test.logout();
            test1.logout();



            /*byte[]buffer = new byte[20];
            DatagramPacket ricever = new DatagramPacket(buffer,buffer.length);

            socket.receive(ricever);
            System.out.println("CIAO");
            buffer= ricever.getData();
            String messaggio = new String(buffer);


            System.out.println("UDP message: "+messaggio);*/



        }catch (IOException e){
            System.out.print("Server non disponibile riprovare più tardi");
            e.printStackTrace();
        }


    }

}
