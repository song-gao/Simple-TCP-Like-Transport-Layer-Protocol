import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Song on 4/17/16.
 */
public class Helper {
    private static String getSequence(TCPPacket tcpPacket){
        byte[] bytes = new byte[4];
        System.arraycopy(tcpPacket.packets,4,bytes,0,4);
        return String.valueOf(fourBytesToInt(bytes));
    }
    private static String getACK(TCPPacket tcpPacket){
        byte[] bytes = new byte[4];
        System.arraycopy(tcpPacket.packets,8,bytes,0,4);
        return String.valueOf(fourBytesToInt(bytes));
    }
    private static String getFlag(TCPPacket tcpPacket){
        byte byte1 = tcpPacket.packets[12];
        byte b1 = tcpPacket.packets[13];
        String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
        return String.valueOf(byte1&1)+s1;
    }
    public static String getLogString(TCPPacket tcpPacket,String sourceIP,int sourcePort,String destIP,int destPort){
        String res;
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        res = timeStamp+", "+sourceIP+"/"+String.valueOf(sourcePort)+", "+destIP+"/"+String.valueOf(destPort);
        res = res + ", Sequence " +getSequence(tcpPacket)+", ACK "+getACK(tcpPacket)+", "+getFlag(tcpPacket);
        return res;
    }

    public static byte[] tcpDatagram(int sourcePort,int destinationPort, int seqNum,
                                         int ackNum, int dataOffset, boolean ACK, boolean FIN,
                                        int windowSize, int urgentPower, byte[] data){
        //data should be even due to the checksum
        byte[] res = new byte[20+data.length];
        byte[] sourcePortArr = positiveIntToTwoBytes(sourcePort);
        byte[] destinationPortArr = positiveIntToTwoBytes(destinationPort);
        byte[] seqNumArr = positiveLongToFourBytes(seqNum);
        byte[] ackNumArr = positiveLongToFourBytes(ackNum);
        byte dataOffsetByte = (byte)(dataOffset << 4);
        byte controlByte = controlBits(ACK,FIN);
        byte[] windowSizeArr = positiveIntToTwoBytes(windowSize);
        byte[] checksum = positiveIntToTwoBytes(0);
        byte[] urgentPowerArr = positiveIntToTwoBytes(urgentPower);

        int index = 0;
        System.arraycopy(sourcePortArr,0,res,index,sourcePortArr.length);
        index += sourcePortArr.length;
        System.arraycopy(destinationPortArr,0,res,index,destinationPortArr.length);
        index += destinationPortArr.length;
        System.arraycopy(seqNumArr,0,res,index,seqNumArr.length);
        index += seqNumArr.length;
        System.arraycopy(ackNumArr,0,res,index,ackNumArr.length);
        index += ackNumArr.length;
        res[index++] = dataOffsetByte;
        res[index++] = controlByte;
        System.arraycopy(windowSizeArr,0,res,index,windowSizeArr.length);
        index += windowSizeArr.length;
        System.arraycopy(checksum,0,res,index,checksum.length);
        index += checksum.length;
        System.arraycopy(urgentPowerArr,0,res,index,urgentPowerArr.length);
        index += urgentPowerArr.length;
        System.arraycopy(data,0,res,index,data.length);

        res[16] = getChecksum(res)[0];
        res[17] = getChecksum(res)[1];
        return res;
    }

    public static byte[] getChecksum(byte[] bytes){
        byte byte1 = (byte)0;
        byte byte2 = (byte)0;
        for(int i=0;i<bytes.length/2;i++){
            if(i == 8){
                //jump the checksum itself
                continue;
            }
            byte1 = unsignedByteSum(byte1,bytes[2*i]);
            byte2 = unsignedByteSum(byte2,bytes[2*i+1]);
        }
        byte[] res = new byte[2];
        res[0] = byte1;
        res[1] = byte2;
        return res;
    }
    private static byte unsignedByteSum(byte byte1,byte byte2){
        byte res = (byte)0;
        int int1 = (int)byte1;
        int int2 = (int)byte2;
        if((int1+int2)<127){
            return (byte)(int1+int2);
        } else{
            //just add a one to the leftmost digit and copy the rest digits from the sum
            return (byte)((byte)(int1+int2-128)|(byte)0x80);
        }

    }

    //Encoding methods
    public static byte controlBits(Boolean ACK, Boolean FIN){
        if(ACK && FIN){
            return (byte)17;
        } else if(ACK){
            return (byte)16;
        } else if(FIN){
            return (byte)1;
        } else {
            return (byte)0;
        }
    }
    public static boolean getFIN(byte input){
        if((int)input == 1 || (int)input == 17){
            return true;
        } else{
            return false;
        }
    }

    public static byte[] positiveIntToTwoBytes(int input){
        //input should be 0 - 65535
        return new byte[] {
                (byte)(input >>> 8),
                (byte)input};
    }
    public static byte[] positiveLongToFourBytes(int input){
        //the input should be less than 2^31-1, which is big enough for common use
        return new byte[] {
                (byte)(input >>> 24),
                (byte)(input >>> 16),
                (byte)(input >>> 8),
                (byte)(input)};
    }
    //Decoding methods
    private static int getDataOffset(byte b){
        return unsignedByteToInt(b)/16;
    }
    private static int unsignedByteToInt(byte b){
        int res = 0;
        int value = 1;
        for(int i=0;i<8;i++){
            res += ((b >> i)&1)*value;
            value *= 2;
        }
        return res;
    }
    public static int twoBytesToInt(byte[] bytes){
        return unsignedByteToInt(bytes[0])*256+unsignedByteToInt(bytes[1]);
    }
    public static int fourBytesToInt(byte[] bytes){
        return unsignedByteToInt(bytes[0])*16777216+unsignedByteToInt(bytes[1])*65536
                +unsignedByteToInt(bytes[2])*256+unsignedByteToInt(bytes[3]);
    }

}
