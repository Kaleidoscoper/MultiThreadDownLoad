package Demo;

/**
 * Created by lenovo on 2016/7/29.
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
import java.lang.Integer;

class DLFrame extends JFrame { //设计图形界面
    private JPanel panel1;
    private JPanel panel2;
    private static TextArea textArea = new TextArea();
    private TextField textField1 = new TextField();
    private TextField textField2 = new TextField();
    private TextField textField3 = new TextField();
    private Button button = new Button("DownLoad");
    private Button button2 = new Button("Confirm");
    private Label label1 = new Label();
    private Label label2 = new Label();
    private Label label3 = new Label();
    private String dlURL = null;
    private String saveFileAs = null;
    private int thread_number = 0;
    private String [] surl;//存放每个线程的URL信息
    private int flag=0;//标志位，用来跟踪URL初始化状态

    public DLFrame() throws HeadlessException {
        panel1 = new JPanel();
        panel2 = new JPanel();
        panel1.setLayout(new BorderLayout(5,10));
        panel2.setLayout(new GridLayout(4,4,10,10));
        textField1.setText("http://localhost:8080/ceshi.rar");//默认URL
        textField2.setText("Result.rar");//默认文件存放位置
        textField3.setText("1");//默认线程数
        label1.setText("File URL:");
        label2.setText("File Save As:");
        label3.setText("Thread Number:");
        textArea.setEditable(false);
        panel2.add(label3, null);
        panel2.add(textField3,null);
        panel1.add(textArea, BorderLayout.CENTER);
        panel2.add(label1,null);
        panel2.add(textField1,null);
        panel2.add(label2,null);
        panel2.add(textField2,null);
        panel2.add(button,null);
        panel2.add(button2,null);
        setLayout(new BorderLayout(5, 10));
        add(panel1,BorderLayout.CENTER);
        add(panel2,BorderLayout.SOUTH);
        button.addActionListener(new ActionListener() { //添加对下载键的监听器
            @Override
            public void actionPerformed(ActionEvent e) {
                button_actionPerformed(e);
            }
        });//添加DownLoad键用监听器
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button2_actionPerformed(e);
            }
        });//添加Confirm键监听器
    }

    public void button_actionPerformed(ActionEvent e){ //响应DownLoad键事件
        try {
            thread_number = Integer.parseInt(textField3.getText());
            if (flag == thread_number) {
                if (!ThrDownload.init(surl, saveFileAs, thread_number))//若URL不存在在服务器上则初始化失败
                    textArea.append("\n线程初始化失败！");
                else {
                    ThrControl p = new ThrControl();
                    p.start();
                    flag=thread_number-1;
                }
            } else {
                if (thread_number < 1)
                    textArea.setText("线程数最少为1,请重新输入！");
                else if (thread_number > 10) //若线程过多或者没有则警告
                    textArea.setText("线程数最多为10，请重新输入！");
                else if (thread_number >= 1 && thread_number <= 10) {
                    textArea.setText("请在URL框依次输入" + thread_number + "个URL地址，点击confirm确认,点击download开始下载");
                    surl = new String[thread_number];
                    flag = 0;
                }
            }
        } catch (Exception ee) {
            ee.printStackTrace();
        }

    }

    public void button2_actionPerformed(ActionEvent e){
        if(ThrDownload.Warning!=0){ //若某个URL不存在。则需要通过Confirm键来修改，Warning为标志位
            String correct = textField1.getText();
            ThrDownload.url_name[ThrDownload.Warning-1] = correct;
            textArea.append("\n第"+ThrDownload.Warning+"个URL已修改，请重新点击下载。");
            ThrDownload.Warning =0;
        }
        else if(ThrDownload.Warning==0){//响应用户对标签的设置
            if (flag < thread_number - 1) {
                dlURL = textField1.getText();

                if (dlURL.equals("") == true) {
                    textArea.setText("URL信息输入不完全，请重新输入！");
                } else {
                    surl[flag] = dlURL;
                    textArea.append("\n第" + (flag+1) + "个URL：" + surl[flag] + "请继续输入下一个。");
                    flag++;
                }
            } else if (flag == thread_number - 1) {
                dlURL = textField1.getText();
                surl[flag] = dlURL;
                textArea.append("\n第" + (flag+1) + "个URL：" + surl[flag] );
                saveFileAs = textField2.getText();
                if (saveFileAs.equals("") == true) { //若URL栏或者文件保存栏为空则警告
                    textArea.append("\n请输入文件保存信息！");
                } else {
                    textArea.append("\n各URL及文件保存信息初始化完毕。");
                    flag++;
                }
            }
        }
    }
    public static void setContent(String n){ //用来修改TextArea显示内容的入口
        textArea.setText(n);
    }
    public static void appendContent(String n){ //用来添加TextArea显示内容的入口
        textArea.append(n);
    }
}

class ThrDownload extends Thread { //下载线程类
    private int no;//线程号
    private int start;//线程下载起始位置
    private int end;//终止位置
    private URL url;
    private byte[] b;//读写缓冲区
    private InputStream in;
    //private BufferedInputStream bis;
    private RandomAccessFile out;
    public static int len;//当前下载字节数
    public static int file_len = 0;//下载文件总长度
    public static int buf_len = 8192;//缓冲区大小
    public static String[] url_name = null;//url名
    public static String save_name = null;//存盘文件名
    public static int thread_num = 1;//初始化线程数为1
    public static ThrDownload[] t;//线程数组
    public static int Warning = 0;

    public static boolean init(String[] url_name, String save_name, int thread_num) { // 初始化下载事项，包括url数组、存放地址、线程数等
        ThrDownload.url_name = url_name;
        ThrDownload.save_name = save_name;
        ThrDownload.thread_num = thread_num;
        int i;

        t = new ThrDownload[thread_num];
        // System.out.println(url_name[0]);
        for (i = 0; i < thread_num; i++) { //为每个线程确定URL并进行连接
            try {
                URL url = new URL(url_name[i]);
                HttpURLConnection conf = (HttpURLConnection) url.openConnection();
                if (conf.getResponseCode() >= 300) {
                    DLFrame.setContent("第" + (i + 1) + "个URL出现Http响应问题：" + conf.getResponseCode() + "请重新输入，按确认键继续。");
                    Warning = i + 1;//出现响应问题则重新输入对应的URL
                    return false;
                }

                file_len = conf.getContentLength();//连接URL获取下载内容总长度
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            if (file_len == -1) {
                return false;
            }
            t[i] = new ThrDownload(i + 1, url_name[i]);//初始化下载线程
        }
        return true;
    } //end of init

    public ThrDownload(int no, String url_name1) { //下载线程构造函数
        this.no = no;
        int block_lenth = file_len / thread_num;//每个线程需传输字节数
        this.start = (no - 1) * block_lenth;//该线程下载起始处
        this.end = start + block_lenth - 1;//该线程下载终止处
        if (no == thread_num) this.end = file_len - 1;//最后一个线程需调整结束位置
        try {
            url = new URL(url_name1);
            HttpURLConnection conf = (HttpURLConnection) url.openConnection();
            conf.setRequestProperty("Range", "bytes=" + start + "-" + end);//用HTTP协议头部参数来确定传输块起止位置
            in = conf.getInputStream();
            //bis = new BufferedInputStream(in);
            out = new RandomAccessFile(save_name, "rw");//输出到一个可读写文件中保存
            out.seek(start);
            b = new byte[buf_len];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() { //线程执行体
        int L;
        try {
            while (true) {
                L = in.read(b);//L为实际读出字节数
                if (L == -1) break;
                len += L;//当前下载字节数加上L
                out.write(b, 0, L);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
class ThrControl extends Thread{ //主控线程
    public void run() {
        int i;
        String Spercent;
        String Sspeed;
        String Stime;
        DLFrame.appendContent("\nSave File Name：" + ThrDownload.save_name);
        DLFrame.appendContent("\n一共有" + ThrDownload.file_len+"字节需要下载。");
        DLFrame.appendContent("\n线程数：" + ThrDownload.thread_num);
        double begin_time = (new Date()).getTime() / 1000.0;
        DecimalFormat dfpercent = new DecimalFormat();
        String style1 = "0.00%";
        dfpercent.applyPattern(style1);//统一在TextArea中展示的输出百分比的格式
        DecimalFormat dfspeed = new DecimalFormat();
        String style2 = "0.00KB/s";
        dfspeed.applyPattern(style2);//统一在TextArea中展示的平均速度的格式
        DecimalFormat dftime = new DecimalFormat();
        String style3 = "0.00s";
        dftime.applyPattern(style3);//统一在TextArea中展示的下载时间的格式


        for (i = 0; i < ThrDownload.thread_num; i++) {
            ThrDownload.t[i].start();
        }
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            double current_time = (new Date()).getTime() / 1000.0;
            double percent = 0, speed = 0;
            percent = ThrDownload.len / (double) ThrDownload.file_len;
            speed = ThrDownload.len / 1000.0 / (current_time - begin_time);//计算下载百分比和平均下载速度
            Spercent =dfpercent.format(percent);
            Sspeed =dfspeed.format(speed);
            DLFrame.appendContent("\n已下载："+Spercent+"          平均下载速度："+Sspeed);//一秒钟更新一次百分比与时间
            for (i = 0; i < ThrDownload.thread_num; i++)
                if (ThrDownload.t[i].isAlive()) break;
            if (i >= ThrDownload.thread_num) break;//若全部线程均完成工作，则跳出循环
        }
        Stime = dftime.format((new Date()).getTime() / 1000.0 - begin_time);
        DLFrame.appendContent("\n下载时间 = " + Stime);//显示下载花费的总时间
        ThrDownload.len =0;//每一次执行完主线程，则清零下载字节变量，以备下次任务到来
    }//end of run
}//end of ThrControl

public class ThreadDownLoad{
    public static void main(String[] args) throws Exception{
        JFrame frame = new DLFrame();//初始化图形界面窗口
        frame.setTitle("多线程下载");
        frame.setSize(new Dimension(600,500));
        frame.setLocation(200,100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);//显示图形界面
    }
}
