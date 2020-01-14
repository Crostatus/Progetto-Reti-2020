import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class OnlineList{
    private CopyOnWriteArrayList<OnlineUser> list;

    public OnlineList(){
        list = new CopyOnWriteArrayList<>();
    }

    // returna true se ho aggiunto l'user, false altrimenti
    public synchronized boolean addUser(String nickname, SocketChannel userChannel){
        OnlineUser newUser = new OnlineUser(nickname, userChannel);

        if(containsUser(nickname) == null) {
            list.add(newUser);
            return true;
        }
        return false;
    }

    // returna true se ha rimosso l'utente, false altrimenti
    public synchronized boolean removeUser(String nickname){
        OnlineUser userToRemove;
        if((userToRemove = containsUser(nickname)) != null) {
            list.remove(userToRemove);
            return true;
        }
        return false;
    }

    public synchronized boolean removeUser(SocketChannel socketChannel){
        OnlineUser userToRemove;
        if((userToRemove = containsUser(socketChannel)) != null) {
            list.remove(userToRemove);
            return true;
        }
        return false;
    }

    //shutdown

    //returna true se l' utente è già presente nella lista, false altrimenti
    private synchronized OnlineUser containsUser(String nickName){

        OnlineUser currentUser = null;
        Iterator<OnlineUser> iterator = list.iterator();
        boolean userAlreadyOnline = false;
        while(iterator.hasNext() && !userAlreadyOnline){
            currentUser = iterator.next();
            userAlreadyOnline = currentUser.compare(nickName);
        }
        System.out.println("userAlreadyOnline: "+userAlreadyOnline);
        if(userAlreadyOnline)
            return currentUser;

        return null;
    }

    //returna true se il socketChannel è già presente nella lista, false altrimenti
    private synchronized OnlineUser containsUser(SocketChannel socketChannel){
        OnlineUser currentUser = null;
        Iterator<OnlineUser> iterator = list.iterator();
        boolean userAlreadyOnline = false;
        while(iterator.hasNext() && !userAlreadyOnline){
            currentUser = iterator.next();
            userAlreadyOnline = currentUser.compare(socketChannel);
        }
        System.out.println("userAlreadyOnline: "+userAlreadyOnline);
        if(userAlreadyOnline)
            return currentUser;

        return null;
    }

    public void printList(){
        System.out.println("\nLista utenti: ");
        for(int i=0;i<list.size();i++)
            list.get(i).printUser();
        System.out.println(":\n");

    }




private class OnlineUser {
    private SocketChannel userChannel;
    private String nickname;

    public OnlineUser(String nickname, SocketChannel userChannel) throws NullPointerException {
        if (nickname == null)
            throw new NullPointerException("nickname =null");
        if (userChannel == null)
            throw new NullPointerException("invalid socketChannel");

        this.nickname = nickname;
        this.userChannel = userChannel;
    }

    public SocketChannel getUserChannel() {
        return userChannel;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean compare(String nickname){
        return nickname.equals(this.nickname);
    }

    public boolean compare(SocketChannel socketChannel){
        return socketChannel.equals(this.userChannel);
    }

    public void printUser(){
        System.out.println("Username: " + nickname + " Indirizzo: " + userChannel);
    }
}

}