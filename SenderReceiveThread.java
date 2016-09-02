import java.util.Timer;

/**
 * Created by Song on 4/20/16.
 */
public class SenderReceiveThread extends Thread {



    @Override
    public void run(){
        while(!Sender.finFlag){
            if(!Sender.ackBuffer.isEmpty()){
                //if ackBuffer is not empty
                TCPPacket tcpPacket = Sender.ackBuffer.poll();
                byte[] ackBytes = new byte[4];
                System.arraycopy(tcpPacket.packets,8,ackBytes,0,4);
                int ackNum = Helper.fourBytesToInt(ackBytes);
                boolean FIN = Helper.getFIN(tcpPacket.packets[13]);
                System.out.println("receives ACK = "+String.valueOf(ackNum));
                Sender.receive(ackNum);

                if(FIN){
                    Sender.finFlag = true;
                    Sender.senderSokcet.close();
                    for(Timer timer:Sender.timerSet){
                        if(timer != null){
                            timer.cancel();
                            timer.purge();
                        }
                    }
                }
            }
        }

    }

}