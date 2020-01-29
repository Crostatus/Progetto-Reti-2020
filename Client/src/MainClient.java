public class MainClient {

    public static void main(String[] args) throws Exception {
        Client test = new Client(8080, 6666);
        test.login("alessandro", "prova98");
    }
}
