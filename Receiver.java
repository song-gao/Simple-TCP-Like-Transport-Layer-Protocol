import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Song on 4/19/16.
 */
public class Receiver {

    public static Map<Integer,TCPPacket> receiverHash = new HashMap<Integer, TCPPacket>();
    public static int currentNeed = 0;
    public static DatagramSocket receiverSocket = null;
    public static int biggestSeq = -1;
    public static int lastSeq = Integer.MAX_VALUE-2;
    public static byte[] dataBytes;
    public static boolean finFlag = false;
    public static Queue<TCPPacket> receiverBuffer = new ConcurrentLinkedQueue<TCPPacket>();
    public static int lastPacketBytes;
    public static boolean alreadyReceiveFIN = false;

    public static String fileName;
    public static int liseningPort;
    public static InetAddress senderAddress;
    public static String senderIp;
    public static int senderPort;
    public static String logFileName;
    public static File fout;
    public static BufferedWriter bufferedWriter;
    public static String localIP;

    public static void main(String[] args) throws Exception{
        System.out.println(InetAddress.getLocalHost());
        if(args.length != 5){
            System.out.println("Exactly five arguments are needed");
            return;
        }
        fileName = args[0];
        liseningPort = Integer.parseInt(args[1]);
        senderIp = args[2];
        senderPort = Integer.parseInt(args[3]);
        logFileName = args[4];
        receiverSocket = new DatagramSocket(liseningPort);
        //receiverSocket.setSoTimeout(5);
        System.out.println("listening on port: "+String.valueOf(liseningPort));
        try{
            localIP = InetAddress.getLocalHost().getHostAddress();
            fout = new File(logFileName);
            FileOutputStream fos = new FileOutputStream(fout);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fos));
            senderAddress = InetAddress.getByName(senderIp);
            AcknowledgeThread acknowledgeThread = new AcknowledgeThread();
            acknowledgeThread.start();
            while(!finFlag){
                byte[] buffer = new byte[1044];
                DatagramPacket datagramPacketReceive = new DatagramPacket(buffer,buffer.length);
                receiverSocket.receive(datagramPacketReceive);
                TCPPacket tcpPacket = new TCPPacket(buffer);
                receiverBuffer.offer(tcpPacket);
                bufferedWriter.write(Helper.getLogString(tcpPacket,datagramPacketReceive.getAddress().getHostAddress(),datagramPacketReceive.getPort(),localIP,liseningPort));
                bufferedWriter.newLine();
            }
            System.out.print("All packets received.");

        }
        catch (Exception e){
            if(finFlag){
                System.out.print("Delivery completed successfully");
                bufferedWriter.close();
            } else{
                e.printStackTrace();
            }
        }
    }


}
