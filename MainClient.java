import java.io.IOException;
import java.rmi.NotBoundException;

public class MainClient {


    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException {
        Client client = new Client(8080, 6666);
        client.registra("Elkosta", "puppasedani69");
        client.login("Elkosta", "puppasedani6969");
        client.login("Elkosta", "puppasedani69");
        Thread.sleep(10000);
        client.logout();
        client.login("Elkosta", "puppasedani69");


    }

}
