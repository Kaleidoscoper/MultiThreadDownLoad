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

class DLFrame extends JFrame { //���ͼ�ν���
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
    private String [] surl;//���ÿ���̵߳�URL��Ϣ
    private int flag=0;//��־λ����������URL��ʼ��״̬

    public DLFrame() throws HeadlessException {
        panel1 = new JPanel();
        panel2 = new JPanel();
        panel1.setLayout(new BorderLayout(5,10));
        panel2.setLayout(new GridLayout(4,4,10,10));
        textField1.setText("http://localhost:8080/ceshi.rar");//Ĭ��URL
        textField2.setText("Result.rar");//Ĭ���ļ����λ��
        textField3.setText("1");//Ĭ���߳���
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
        button.addActionListener(new ActionListener() { //��Ӷ����ؼ��ļ�����
            @Override
            public void actionPerformed(ActionEvent e) {
                button_actionPerformed(e);
            }
        });//���DownLoad���ü�����
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button2_actionPerformed(e);
            }
        });//���Confirm��������
    }

    public void button_actionPerformed(ActionEvent e){ //��ӦDownLoad���¼�
        try {
            thread_number = Integer.parseInt(textField3.getText());
            if (flag == thread_number) {
                if (!ThrDownload.init(surl, saveFileAs, thread_number))//��URL�������ڷ����������ʼ��ʧ��
                    textArea.append("\n�̳߳�ʼ��ʧ�ܣ�");
                else {
                    ThrControl p = new ThrControl();
                    p.start();
                    flag=thread_number-1;
                }
            } else {
                if (thread_number < 1)
                    textArea.setText("�߳�������Ϊ1,���������룡");
                else if (thread_number > 10) //���̹߳������û���򾯸�
                    textArea.setText("�߳������Ϊ10�����������룡");
                else if (thread_number >= 1 && thread_number <= 10) {
                    textArea.setText("����URL����������" + thread_number + "��URL��ַ�����confirmȷ��,���download��ʼ����");
                    surl = new String[thread_number];
                    flag = 0;
                }
            }
        } catch (Exception ee) {
            ee.printStackTrace();
        }

    }

    public void button2_actionPerformed(ActionEvent e){
        if(ThrDownload.Warning!=0){ //��ĳ��URL�����ڡ�����Ҫͨ��Confirm�����޸ģ�WarningΪ��־λ
            String correct = textField1.getText();
            ThrDownload.url_name[ThrDownload.Warning-1] = correct;
            textArea.append("\n��"+ThrDownload.Warning+"��URL���޸ģ������µ�����ء�");
            ThrDownload.Warning =0;
        }
        else if(ThrDownload.Warning==0){//��Ӧ�û��Ա�ǩ������
            if (flag < thread_number - 1) {
                dlURL = textField1.getText();

                if (dlURL.equals("") == true) {
                    textArea.setText("URL��Ϣ���벻��ȫ�����������룡");
                } else {
                    surl[flag] = dlURL;
                    textArea.append("\n��" + (flag+1) + "��URL��" + surl[flag] + "�����������һ����");
                    flag++;
                }
            } else if (flag == thread_number - 1) {
                dlURL = textField1.getText();
                surl[flag] = dlURL;
                textArea.append("\n��" + (flag+1) + "��URL��" + surl[flag] );
                saveFileAs = textField2.getText();
                if (saveFileAs.equals("") == true) { //��URL�������ļ�������Ϊ���򾯸�
                    textArea.append("\n�������ļ�������Ϣ��");
                } else {
                    textArea.append("\n��URL���ļ�������Ϣ��ʼ����ϡ�");
                    flag++;
                }
            }
        }
    }
    public static void setContent(String n){ //�����޸�TextArea��ʾ���ݵ����
        textArea.setText(n);
    }
    public static void appendContent(String n){ //�������TextArea��ʾ���ݵ����
        textArea.append(n);
    }
}

class ThrDownload extends Thread { //�����߳���
    private int no;//�̺߳�
    private int start;//�߳�������ʼλ��
    private int end;//��ֹλ��
    private URL url;
    private byte[] b;//��д������
    private InputStream in;
    //private BufferedInputStream bis;
    private RandomAccessFile out;
    public static int len;//��ǰ�����ֽ���
    public static int file_len = 0;//�����ļ��ܳ���
    public static int buf_len = 8192;//��������С
    public static String[] url_name = null;//url��
    public static String save_name = null;//�����ļ���
    public static int thread_num = 1;//��ʼ���߳���Ϊ1
    public static ThrDownload[] t;//�߳�����
    public static int Warning = 0;

    public static boolean init(String[] url_name, String save_name, int thread_num) { // ��ʼ�������������url���顢��ŵ�ַ���߳�����
        ThrDownload.url_name = url_name;
        ThrDownload.save_name = save_name;
        ThrDownload.thread_num = thread_num;
        int i;

        t = new ThrDownload[thread_num];
        // System.out.println(url_name[0]);
        for (i = 0; i < thread_num; i++) { //Ϊÿ���߳�ȷ��URL����������
            try {
                URL url = new URL(url_name[i]);
                HttpURLConnection conf = (HttpURLConnection) url.openConnection();
                if (conf.getResponseCode() >= 300) {
                    DLFrame.setContent("��" + (i + 1) + "��URL����Http��Ӧ���⣺" + conf.getResponseCode() + "���������룬��ȷ�ϼ�������");
                    Warning = i + 1;//������Ӧ���������������Ӧ��URL
                    return false;
                }

                file_len = conf.getContentLength();//����URL��ȡ���������ܳ���
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            if (file_len == -1) {
                return false;
            }
            t[i] = new ThrDownload(i + 1, url_name[i]);//��ʼ�������߳�
        }
        return true;
    } //end of init

    public ThrDownload(int no, String url_name1) { //�����̹߳��캯��
        this.no = no;
        int block_lenth = file_len / thread_num;//ÿ���߳��贫���ֽ���
        this.start = (no - 1) * block_lenth;//���߳�������ʼ��
        this.end = start + block_lenth - 1;//���߳�������ֹ��
        if (no == thread_num) this.end = file_len - 1;//���һ���߳����������λ��
        try {
            url = new URL(url_name1);
            HttpURLConnection conf = (HttpURLConnection) url.openConnection();
            conf.setRequestProperty("Range", "bytes=" + start + "-" + end);//��HTTPЭ��ͷ��������ȷ���������ֹλ��
            in = conf.getInputStream();
            //bis = new BufferedInputStream(in);
            out = new RandomAccessFile(save_name, "rw");//�����һ���ɶ�д�ļ��б���
            out.seek(start);
            b = new byte[buf_len];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() { //�߳�ִ����
        int L;
        try {
            while (true) {
                L = in.read(b);//LΪʵ�ʶ����ֽ���
                if (L == -1) break;
                len += L;//��ǰ�����ֽ�������L
                out.write(b, 0, L);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
class ThrControl extends Thread{ //�����߳�
    public void run() {
        int i;
        String Spercent;
        String Sspeed;
        String Stime;
        DLFrame.appendContent("\nSave File Name��" + ThrDownload.save_name);
        DLFrame.appendContent("\nһ����" + ThrDownload.file_len+"�ֽ���Ҫ���ء�");
        DLFrame.appendContent("\n�߳�����" + ThrDownload.thread_num);
        double begin_time = (new Date()).getTime() / 1000.0;
        DecimalFormat dfpercent = new DecimalFormat();
        String style1 = "0.00%";
        dfpercent.applyPattern(style1);//ͳһ��TextArea��չʾ������ٷֱȵĸ�ʽ
        DecimalFormat dfspeed = new DecimalFormat();
        String style2 = "0.00KB/s";
        dfspeed.applyPattern(style2);//ͳһ��TextArea��չʾ��ƽ���ٶȵĸ�ʽ
        DecimalFormat dftime = new DecimalFormat();
        String style3 = "0.00s";
        dftime.applyPattern(style3);//ͳһ��TextArea��չʾ������ʱ��ĸ�ʽ


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
            speed = ThrDownload.len / 1000.0 / (current_time - begin_time);//�������ذٷֱȺ�ƽ�������ٶ�
            Spercent =dfpercent.format(percent);
            Sspeed =dfspeed.format(speed);
            DLFrame.appendContent("\n�����أ�"+Spercent+"          ƽ�������ٶȣ�"+Sspeed);//һ���Ӹ���һ�ΰٷֱ���ʱ��
            for (i = 0; i < ThrDownload.thread_num; i++)
                if (ThrDownload.t[i].isAlive()) break;
            if (i >= ThrDownload.thread_num) break;//��ȫ���߳̾���ɹ�����������ѭ��
        }
        Stime = dftime.format((new Date()).getTime() / 1000.0 - begin_time);
        DLFrame.appendContent("\n����ʱ�� = " + Stime);//��ʾ���ػ��ѵ���ʱ��
        ThrDownload.len =0;//ÿһ��ִ�������̣߳������������ֽڱ������Ա��´�������
    }//end of run
}//end of ThrControl

public class ThreadDownLoad{
    public static void main(String[] args) throws Exception{
        JFrame frame = new DLFrame();//��ʼ��ͼ�ν��洰��
        frame.setTitle("���߳�����");
        frame.setSize(new Dimension(600,500));
        frame.setLocation(200,100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);//��ʾͼ�ν���
    }
}
