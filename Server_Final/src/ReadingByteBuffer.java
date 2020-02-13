import java.nio.ByteBuffer;

/*
OVERVIEW: Un oggetto di tipo ReadingByteBuffer contiene un ByteBuffer e una Stringa che viene aggiornata con il contenuto del buffer.
          Viene usato dal server per memorizzare i messaggi ricevuti nelle varie operazioni di lettura fino alla ricezione completa della frase,
          terminata dalla flag "_EOM" = "End Of Message"
 */

public class ReadingByteBuffer {
    private ByteBuffer byteBuffer;
    private String message;
    private static int CHUNKSIZE=128;
    private boolean codiceErroreLogin;

    public ReadingByteBuffer() {
        byteBuffer = ByteBuffer.allocateDirect(CHUNKSIZE);
        message = "";
        codiceErroreLogin = false;
    }

    public ReadingByteBuffer(String risposta){
        byteBuffer = ByteBuffer.allocateDirect(CHUNKSIZE);
        message = risposta;
        codiceErroreLogin = false;
    }

    /*Metodo per concatenare il contenuto del ByteBuffer alla stringa "message", che restituisce:
        true  => la stringa, dopo aver aggiornato il contenuto, termina con  "_EOM"
        false => altrimenti */
    public boolean updateOnRead() {
        byteBuffer.flip();
        int bytesNum = byteBuffer.limit();
        byte[] data = new byte[bytesNum];
        byteBuffer.get(data);
        String result = new String(data);
        message += result;
        byteBuffer.clear();

        if (message.endsWith("_EOM")) {
            message = message.replace("_EOM", "");
            return true;
        }
        return false;
    }

    public void setCodiceErroreLogin(){
        codiceErroreLogin = true;
    }

    public boolean getCodiceErroreLogin(){
        return codiceErroreLogin;
    }

    public void clear(){
        byteBuffer.clear();
    }

    public String getMessage(){
        return message;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void updateMessagge(String risposta){
        message=risposta;
    }
}
