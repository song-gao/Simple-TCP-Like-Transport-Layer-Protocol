import java.net.DatagramPacket;

/**
 * Created by Song on 4/18/16.
 */
public class SendingThread extends Thread{
    @Override
    public void run(){
        while(!Sender.finFlag){
            if(!Sender.transmitQueue.isEmpty()){
                //if the queue is not empty, send the head of the queue to the receiver
                //System.out.println("SIZE OF QUEUE: "+String.valueOf(Sender.transmitQueue.size()));
                TCPPacket packet = Sender.transmitQueue.poll();

                DatagramPacket datagramPacket = new DatagramPacket(packet.packets,packet.packets.length,Sender.remoteAddress,Sender.remotePort);
                try{
                    Sender.senderSokcet.send(datagramPacket);
                    Sender.totalTransmit++;
                    Sender.bufferedWriter.write(Helper.getLogString(packet,Sender.sourceIP,Sender.ackPortNumber,Sender.remoteIP,Sender.remotePort));
                    Sender.bufferedWriter.newLine();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                //System.out.println("SIZE OF QUEUE: "+String.valueOf(Sender.transmitQueue.size()));
            }
        }

    }
}
