import java.net.SocketAddress;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class OnlineList{
    private CopyOnWriteArrayList<OnlineUser> list;

    public OnlineList(){
        list = new CopyOnWriteArrayList<>();
    }

    // returna true se ho aggiunto l'user, false altrimenti
    public boolean addUser(String nickname, SocketAddress userAddress){
        OnlineUser newUser = new OnlineUser(nickname, userAddress);

        if(containsUser(nickname) == null) {
            list.add(newUser);
            return true;
        }
        return false;
    }

    // returna true se ha rimosso l'utente, false altrimenti
    public boolean removeUser(String nickname){
        OnlineUser userToRemove;
        if((userToRemove = containsUser(nickname)) != null) {
            list.remove(userToRemove);
            return true;
        }
        return false;
    }

    //shutdown

    //returna true se l' utente è già presente nella lista, false altrimenti
    private OnlineUser containsUser(String nickName){

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

    public void printList(){
        System.out.println("Lista utenti:::");
        for(int i=0;i<list.size();i++)
            list.get(i).printUser();
        System.out.println(":::");

    }








private class OnlineUser {
    private SocketAddress userAddress;
    private String nickname;

    public OnlineUser(String nickname, SocketAddress userAddress) throws NullPointerException {
        if (nickname == null)
            throw new NullPointerException("nickname =null");
        if (userAddress == null)
            throw new NullPointerException("invalid socketAddress");

        this.nickname = nickname;
        this.userAddress = userAddress;
    }

    public SocketAddress getUserAddress() {
        return userAddress;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean compare(String nickname){
        return nickname.equals(this.nickname);
    }

    public void printUser(){
        System.out.println("Username: " + nickname + " Indirizzo: " + userAddress);
    }
}

}