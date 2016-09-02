import java.util.TimerTask;

/**
 * Created by Song on 4/18/16.
 */
public class SenderTimeout extends TimerTask {

    private int seqNum;
    public SenderTimeout(int seqNum){
        this.seqNum = seqNum;
    }
    @Override
    public void run(){
        Sender.send(seqNum);
    }

}
