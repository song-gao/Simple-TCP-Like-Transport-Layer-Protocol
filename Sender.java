import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Song on 4/14/16.
 */
public class Sender {
    public static double alpha = 0.125;
    public static Map<Integer,Long> sendingTimeMap = new HashMap<Integer, Long>();
    public static int totalTransmit = 0;
    public static boolean finFlag = false;
    private static byte[] fileBytes;
    private static int numOfBytesInSeq = 1024;//number of bytes of data in a seq
    private static int numOfSeq;
    private static int dataOffset = 80;
    private static boolean ACK = false;
    private static boolean FIN = false;
    private static int windowSize = 1;
    private static int urgentPower = 0;
    private static long RTT = 10*1000;//measured in milliseconds
    public static Queue<TCPPacket> transmitQueue = new ConcurrentLinkedQueue<TCPPacket>();
    public static Queue<TCPPacket> ackBuffer = new ConcurrentLinkedQueue<TCPPacket>();
    public static Queue<String> logBuffer = new ConcurrentLinkedQueue<String>();

    private static Map<Integer,Integer> transmitMap = new HashMap<Integer, Integer>();
    public static Set<Timer> timerSet = new HashSet<Timer>();
    //0 means no transmission, 1 means transmitted with no ACK, 2 means successful transmission
    private static String fileName;
    public static String remoteIP;
    public static int remotePort;
    public static int ackPortNumber;
    private static String logFileName;
    public static InetAddress remoteAddress;
    public static String sourceIP;
    private static int windowStart = 0;
    public static DatagramSocket senderSokcet = null;
    public static File fout;
    public static BufferedWriter bufferedWriter;
    public static void main(String args[]){

        if(args.length < 5 || args.length > 6){
            System.out.println("Either 5 or 6 arguments should be specified");
            return;
        }
        if(args.length == 6){
            windowSize = Integer.parseInt(args[5]);
        }
        fileName = args[0];
        remoteIP = args[1];
        remotePort = Integer.parseInt(args[2]);
        ackPortNumber = Integer.parseInt(args[3]);
        logFileName = args[4];
        System.out.println("Listening on port for ack: "+String.valueOf(ackPortNumber));
        System.out.println("Window size is: "+String.valueOf(windowSize));


        File file = new File(fileName);
        byte[] test = new byte[(int) file.length()];
        FileInputStream fileInputStream=null;
        try {
            sourceIP = InetAddress.getLocalHost().getHostAddress();
            fout = new File(logFileName);
            FileOutputStream fos = new FileOutputStream(fout);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fos));

            remoteAddress = InetAddress.getByName(remoteIP);
            //convert file into array of bytes
            fileInputStream = new FileInputStream(file);
            fileBytes = new byte[(int)file.length()];
            fileInputStream.read(fileBytes);
            numOfSeq = fileBytes.length/numOfBytesInSeq;
            if((fileBytes.length%numOfBytesInSeq)!=0){
                numOfSeq++;
            }
            for(int i=0;i<numOfSeq;i++){
                transmitMap.put(i,0);
            }
            // System.out.println(fileBytes.length);
            // System.out.println(numOfSeq);

            LogWrittingThread logWrittingThread = new LogWrittingThread();
            logWrittingThread.start();

            senderSokcet = new DatagramSocket(ackPortNumber);
            for(int i=0;i<windowSize && i<numOfSeq;i++){
                send(i);
                transmitMap.put(i,1);
            }

            SendingThread sendingThread = new SendingThread();
            sendingThread.start();
            SenderReceiveThread senderReceive = new SenderReceiveThread();
            senderReceive.start();
            while(!finFlag){
                byte[] buffer = new byte[20];
                DatagramPacket datagramPacketReceive = new DatagramPacket(buffer,20);
                senderSokcet.receive(datagramPacketReceive);
                TCPPacket tcpPacket = new TCPPacket(buffer);
                ackBuffer.offer(tcpPacket);
                logBuffer.offer(Helper.getLogString(tcpPacket,datagramPacketReceive.getAddress().getHostAddress(),datagramPacketReceive.getPort(),sourceIP,ackPortNumber));
            }
            System.out.println("Delivery completed successfully");
            System.out.println("Total bytes sent = "+String.valueOf(fileBytes.length));
            System.out.println("Segments sent = "+String.valueOf(numOfSeq));
            System.out.println("Segments retransmitted = "+String.valueOf(100*(totalTransmit - numOfSeq)/totalTransmit)+" %");
                

        }catch(Exception e){
            if(finFlag){
                System.out.println("Delivery completed successfully");
                System.out.println("Total bytes sent = "+String.valueOf(fileBytes.length));
                System.out.println("Segments sent = "+String.valueOf(numOfSeq));
                System.out.println("Segments retransmitted = "+String.valueOf(100*(totalTransmit - numOfSeq)/totalTransmit)+" %");
                try{
                    bufferedWriter.close();
                }
                catch (Exception e2){
                    e.printStackTrace();
                }
                return;
            } else{
                e.printStackTrace();
            }
        }
    }

    public static void send(int seqNum){
        //first check the hash whether this seq is transmitted, if it is 2, it is already sent
        
        if(transmitMap.get(seqNum) == 0){
            //first transmission
            sendingTimeMap.put(seqNum,System.currentTimeMillis());

        } else{
            sendingTimeMap.put(seqNum,0L);
        }
        if(transmitMap.get(seqNum) != 2){
            byte[] data = new byte[numOfBytesInSeq];
            if(seqNum<(numOfSeq-1)){
                for(int i = 0;i<numOfBytesInSeq;i++){
                    data[i] = fileBytes[seqNum*numOfBytesInSeq+i];
                }
            } else{
                //the case of the last packet, squeezing with 0s at the end.
                for(int i = 0;i<numOfBytesInSeq;i++){
                    if((seqNum*numOfBytesInSeq+i) >= fileBytes.length){
                        data[i] = 0;
                        continue;
                    }
                    data[i] = fileBytes[seqNum*numOfBytesInSeq+i];
                }
                FIN = true;
                urgentPower = fileBytes.length%numOfBytesInSeq;
            }

            TCPPacket packet = new TCPPacket(Helper.tcpDatagram(ackPortNumber,remotePort,seqNum,0,dataOffset,ACK,FIN,windowSize,urgentPower,data));
            FIN = false;// avoid
            transmitQueue.offer(packet);
            System.out.println("send Seq = "+String.valueOf(seqNum));
            Timer timer = new Timer();
            timer.schedule(new SenderTimeout(seqNum),RTT);//set timeout
            timerSet.add(timer);
        }


    }
    public static void receive(int ackNum){
        if(ackNum<windowStart){
            //this is the case when nothing should be done
        } else{
            //move the window and update the transitMap and sampleRTT
            int count = 0;
            long time = 0L;
            for(int i=windowStart;i<ackNum;i++){
                if(transmitMap.get(i) == 1 && sendingTimeMap.get(i) != 0L){
                    count++;
                    time+=(System.currentTimeMillis()-sendingTimeMap.get(i));
                }
                transmitMap.put(i,2);
            }
            if(count != 0){
                //update RTT
                time = time/(long)count;
                RTT = RTT - (long)(alpha*(double)(RTT - time));
                System.out.println("RTT updated to: "+String.valueOf(RTT)+" milliseconds");
            }

            windowStart = ackNum;

            for(int i=windowStart;(i<windowStart+windowSize)&&(i<numOfSeq);i++){
                if(transmitMap.get(i) == 0){
                    //if the this seq hasn't been transmitted yet, transmit it
                    send(i);
                    transmitMap.put(i,1);
                }
            }
        }
        System.out.println("window start is:"+String.valueOf(windowStart));
    }

}
