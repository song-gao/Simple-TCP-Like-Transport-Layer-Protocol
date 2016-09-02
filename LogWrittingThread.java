/**
 * Created by Song on 4/21/16.
 */
public class LogWrittingThread extends Thread {

    public void run(){
        while(!Sender.finFlag){
            if(!Sender.logBuffer.isEmpty()){
                String str = Sender.logBuffer.poll();
                try{
                    Sender.bufferedWriter.write(str);
                    Sender.bufferedWriter.newLine();
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }
}