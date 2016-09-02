import java.io.FileOutputStream;
import java.net.DatagramPacket;

/**
 * Created by Song on 4/19/16.
 */
public class AcknowledgeThread extends Thread {



    @Override
    public void run(){
        while(!Receiver.finFlag){
            if(!Receiver.receiverBuffer.isEmpty()){
                //if the buffer is not empty
                TCPPacket tcpPacket = Receiver.receiverBuffer.poll();
                byte[] seqNumBytes = new byte[4];
                System.arraycopy(tcpPacket.packets,4,seqNumBytes,0,4);
                int seqNum = Helper.fourBytesToInt(seqNumBytes);
                boolean FIN = Helper.getFIN(tcpPacket.packets[13]);
                byte[] checkSum = Helper.getChecksum(tcpPacket.packets);

                if((checkSum[0] != tcpPacket.packets[16]) || (checkSum[1]!=tcpPacket.packets[17])
                        || (seqNum < Receiver.currentNeed) || (Receiver.receiverHash.containsKey(seqNum))){
                    //the packet is corrupted or is already received
                    if((checkSum[0] != tcpPacket.packets[16]) || (checkSum[1]!=tcpPacket.packets[17])){
                        System.out.println("The packet is corrupted");
                    } else if(seqNum < Receiver.currentNeed){
                        System.out.println("The packet is already received");
                    } else if(Receiver.receiverHash.containsKey(seqNum)){
                        System.out.println("The packet is already received");
                    }
                    TCPPacket corruptACK = new TCPPacket(Helper.tcpDatagram(Receiver.liseningPort,Receiver.senderPort,0,Receiver.currentNeed,80,true,false,0,0,new byte[0]));
                    DatagramPacket corruptACKPacket = new DatagramPacket(corruptACK.packets,corruptACK.packets.length,Receiver.senderAddress,Receiver.senderPort);
                    try{
                        Receiver.receiverSocket.send(corruptACKPacket);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    continue;
                }

                if(FIN && (!Receiver.alreadyReceiveFIN)){
                    //This is the first time FIN = 1 is transmitted
                    Receiver.lastSeq = seqNum;
                    Receiver.alreadyReceiveFIN = true;
                    byte[] lastPacketDataBytes = new byte[2];
                    System.arraycopy(tcpPacket.packets,18,lastPacketDataBytes,0,2);
                    Receiver.lastPacketBytes = Helper.twoBytesToInt(lastPacketDataBytes);
                }
                if(Receiver.biggestSeq < seqNum){
                    Receiver.biggestSeq = seqNum;
                }
                //Copy the packet to the hash
                Receiver.receiverHash.put(seqNum,tcpPacket);


                if(Receiver.currentNeed == seqNum){
                    //the wanted packet is just received,need to change the wanted seq num
                    for(int i=Receiver.currentNeed+1;i<=Receiver.lastSeq+1;i++){
                        if(!Receiver.receiverHash.containsKey(i)){
                            Receiver.currentNeed = i;
                            break;
                        }
                    }
                }

                System.out.println("Successfully receiving packet No."+String.valueOf(seqNum));
                System.out.println("Next packet needed is: "+String.valueOf(Receiver.currentNeed));
                System.out.println("");

                if((Receiver.lastSeq+1) == Receiver.currentNeed){
                    //This is the case when every packets are received, need to terminate the link
                    TCPPacket validACK = new TCPPacket(Helper.tcpDatagram(Receiver.liseningPort,Receiver.senderPort,0,Receiver.currentNeed,80,true,true,0,0,new byte[0]));
                    DatagramPacket validACKPacket = new DatagramPacket(validACK.packets,validACK.packets.length,Receiver.senderAddress,Receiver.senderPort);
                    try{
                        Receiver.receiverSocket.send(validACKPacket);
                        Receiver.dataBytes = new byte[Receiver.lastSeq*1024+Receiver.lastPacketBytes];
                        System.out.println("Receiver receives "+String.valueOf(Receiver.lastSeq*1024+Receiver.lastPacketBytes)+" bytes");
                        for(int i = 0;i<Receiver.lastSeq;i++){
                            System.arraycopy(Receiver.receiverHash.get(i).packets,20,Receiver.dataBytes,i*1024,1024);
                        }
                        System.arraycopy(Receiver.receiverHash.get(Receiver.lastSeq).packets,20,Receiver.dataBytes,Receiver.lastSeq*1024,Receiver.lastPacketBytes);
                        FileOutputStream fileOutputStream = new FileOutputStream(Receiver.fileName);
                        fileOutputStream.write(Receiver.dataBytes);
                        fileOutputStream.close();
                        Receiver.finFlag = true;
                        Receiver.receiverSocket.close();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                } else{
                    //just a common ACK for a packet
                    TCPPacket validACK = new TCPPacket(Helper.tcpDatagram(Receiver.liseningPort,Receiver.senderPort,0,Receiver.currentNeed,80,true,false,0,0,new byte[0]));
                    DatagramPacket validACKPacket = new DatagramPacket(validACK.packets,validACK.packets.length,Receiver.senderAddress,Receiver.senderPort);
                    try{
                        Receiver.receiverSocket.send(validACKPacket);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
        }

    }
}
