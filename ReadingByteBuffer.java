import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class ReadingByteBuffer {
    private ByteBuffer byteBuffer;
    private String message;
    private static int CHUNKSIZE=128;

    public ReadingByteBuffer() {
        byteBuffer = ByteBuffer.allocateDirect(CHUNKSIZE);
        message = "";
    }

    // legge dal buffer e aggiorna il messaggio concatenando quello letto
    // ora a quello letto in precedenza
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

    public void generateNewBuffer(int sizeOfMessage){
        byteBuffer = ByteBuffer.allocateDirect(sizeOfMessage);
    }

    public void put(byte []data){
        byteBuffer.put(data);
    }

    public void clear(){
        byteBuffer.clear();
    }

    public void rewind(){
        byteBuffer.rewind();
    }

    public void setLimit(int size){
        byteBuffer.limit(size);
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
