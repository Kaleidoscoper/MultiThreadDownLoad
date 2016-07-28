package Demo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by lenovo on 2016/7/28.
 */
public class MultiThreadDownLoad {
    public static Object object = new Object();
    private URL downloadUrl;
    private File localFile;
    private int block;
    public static void main(String[] args)throws Exception{
        String path = "http://localhost:8099/123.PNG";
        new MultiThreadDownLoad().download(path,5);
    }
    public void download(String path,int threadCount) throws Exception{
        int threadid,startPosition,endPosition;
        downloadUrl = new URL(path);
        HttpURLConnection conn = (HttpURLConnection)downloadUrl.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5*1000);
        conn.connect();
        int dataLen = conn.getContentLength();
        if(dataLen<0){
            System.out.println("获取数据失败");
            return;
        }
        String filename = parseFilename(path);
        localFile = new File(filename);
        RandomAccessFile raf = new RandomAccessFile(localFile,"rwd");
        raf.setLength(dataLen);
        raf.close();
        int blocksize = dataLen/threadCount;
        System.out.println(conn.getContentLength());
        for (int i=1;i<=threadCount;i++){
            threadid =i;
            startPosition = blocksize*(threadid-1);
            endPosition = startPosition+blocksize-1;
            if(i==threadCount)
                endPosition = dataLen;
            new Thread(new DownloadThread(i,startPosition,endPosition)).start();
        }
    }
    private String parseFilename(String path){
        return path.substring(path.lastIndexOf("/")+1);
    }
    private final class DownloadThread implements Runnable{
        private int threadid;
        private int startPosition;
        private int endPosition;
        public DownloadThread(int threadid,int startPosition,int endPosition){
            this.threadid = threadid;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }

        @Override
        public void run() {
            RandomAccessFile raf = null;
            try {
                HttpURLConnection conn = (HttpURLConnection)downloadUrl.openConnection();
                conn.setConnectTimeout(5*1000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range","bytes="+startPosition+"-"+endPosition);
                System.out.println("起止地址："+startPosition+" "+endPosition);
                raf = new RandomAccessFile(localFile,"rwd");
                raf.seek(startPosition);
                writeTo(raf, conn);

            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    if(raf!=null)
                        raf.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        private void  writeTo(RandomAccessFile accessFile,HttpURLConnection conn){
            BufferedInputStream bis =null;
            BufferedOutputStream bos = null;
            try {
                int length;
                bis = new BufferedInputStream(conn.getInputStream());
                byte[] bytes = new byte[1024];
                while ((length=bis.read(bytes))!=-1){
                    accessFile.write(bytes,0,length);
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    bis.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
