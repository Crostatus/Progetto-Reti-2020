
public class MainServer {
    public static void main(String[] args) throws Exception{
        Server server=new Server();
        server.startServer();


    }





    /*private static void readRequest(SelectionKey newReadRequest) throws IOException{
        System.out.println("Read request!");
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();
        client.read(attachment.getByteBuffer());

        if(attachment.updateOnRead()) {
            //newReadRequest.interestOps(SelectionKey.OP_WRITE);
            SocketAddress clientAddress = client.getRemoteAddress();
            String richiesta = attachment.getMessage();
            System.out.println("Messaggio arrivato al server: " + richiesta);
            StringTokenizer tokenizer = new StringTokenizer(richiesta);
            String token = tokenizer.nextToken();
            System.out.println("Token arrivato al server: " + token);
            if(token.equals("login")) {
                String nickname = tokenizer.nextToken();
                String password = tokenizer.nextToken();
                int codice = controllo_credenziali(nickname, password);
                if (codice == 0) {
                    //Aggiungi alla lista persone online
                    System.out.println("PUPPASEDANIIIIIIII");
                } else {
                    System.out.println(codice);
                }
            }
        }
        //controllo di sicurezza

        // converto data, cerco su json, controllo pw e user
        //salvare informazioni del client che è online
    }
*/


}





