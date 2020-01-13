import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class ReadingByteBuffer {
    private ByteBuffer byteBuffer;
    private String message;

    public ReadingByteBuffer() {
        byteBuffer = ByteBuffer.allocate(64);
        message = "";
    }
/*
    public boolean updateOnRead() {
        byteBuffer.flip();
        int bytesNum = byteBuffer.limit();
        byte[] data = new byte[bytesNum];
        byteBuffer.get(data);
        String result = new String(data);
        message += result;
        byteBuffer.clear();

        if (message.endsWith("_EOM")) {
            message = message.replace("_EOM", " - Echoed by the server");
            byteBuffer = Charset.forName("ISO-8859-1").encode(CharBuffer.wrap(message.toCharArray()));
            return true;
        }
        return false;
    }
*/

    // legge dal buffer e aggiorna il messaggio concatenando quello letto
    // ora a quello letto in precedenza
    public void updateOnRead() {
        byteBuffer.flip();
        int bytesNum = byteBuffer.limit();
        byte[] data = new byte[bytesNum];
        byteBuffer.get(data);
        String result = new String(data);
        message += result;
        byteBuffer.clear();
    }


    public String getMessage(){
        return message;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void updateMessagge(String risposta){
        byteBuffer.clear();
        message = risposta;
        byteBuffer.put(risposta.getBytes());
        byteBuffer.flip();
    }
}
