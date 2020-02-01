public class MainClient2 {

    public static void main(String[] args) throws Exception{

        Client test1 = new Client(8080, 6666);
        //test1.registra("alessandro","prova98");
        test1.login("alessandro", "prova98");
        //Thread.sleep(10000);
        while(test1.getBoolean()){

        }
        test1.lista_amici("alessandro");



    }
}
