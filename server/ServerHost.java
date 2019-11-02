package server;

import java.io.*;
import java.net.*;

public class ServerHost extends Thread{
    private Socket sckt = null;

    public ServerHost(Socket socket){
        this.sckt = socket;
    }

    public String receive() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(sckt.getInputStream()));
            String fileName = br.readLine();
            System.out.println("File to receive:" + fileName);
            String fileSize = br.readLine();
            System.out.println("File size: " + fileSize);
            Integer size = Integer.parseInt(fileSize);
            byte[] contents = new byte[size];

            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            InputStream is = sckt.getInputStream();

            int bytesRead = 0;
            int count = 0;
            System.out.println("File receive start");
            while(count < size)
            {
                System.out.println("Reading " + count + "th byte");
                if(size - count > contents.length){
                    bytesRead=is.read(contents, 0, contents.length);
                }
                else{
                    bytesRead = is.read(contents, 0, size - count);
                }
                System.out.println(bytesRead + " bytes read");
                bos.write(contents, 0, bytesRead);
                count += bytesRead;
                System.out.println(count + " bytes read in total");
                bos.flush();
            }
            bos.flush();
            bos.close();
            System.out.println("File saved successfully!");
            return file.getAbsolutePath();
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String process(String filePath){
        File file = new File(filePath);
        String directory = file.getParent();
        String fileName = file.getName();
        String [] info = fileName.split("\\." ,2);
        String name = info[0];
        String processedFilePath = directory + "/" + name + "_processed.mp4";
        System.out.println(processedFilePath);
        String model = "nasnetlarge";
        String pythonFilePath = directory + "/video_process.py";

        try {
            Process p = Runtime.getRuntime().exec("python" + pythonFilePath + ' ' + filePath + ' ' + model);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()), 10);
            System.out.println("Start processing");
            String line = null;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            // wait for the process to finish
            p.waitFor();
            System.out.println("File processed");
            in.close();
            return processedFilePath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Error");
        return "";
    }


    public void send(String filePath){
        try {
            File file = new File(filePath);
            PrintWriter pw = new PrintWriter(sckt.getOutputStream());
            pw.println(file.getName());
            pw.flush();
            System.out.println("File name sent");
            pw.println(file.length());
            pw.flush();
            System.out.println("File size sent");

            FileInputStream in = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int i = 0;
            while ((i = in.read(b)) != -1) {
                baos.write(b, 0,i);
            }
            baos.close();
            in.close();
            OutputStream os = sckt.getOutputStream();
            os.write(baos.toByteArray());
            os.flush();
            os.close();
            //File transfer done. Close the socket connection!
            sckt.close();
            System.out.println("File sent successfully!");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


    public void run() {
        String fileName = receive();
        System.out.println("File received, start processing:" + fileName);
        String processFile = process(fileName);
        System.out.println("Process complete, file name:" + processFile);
        String newFilePath = processFile;
        send(newFilePath);
    }

    public static void main(String[] args) {
        int RECEIVER_PORT = 8888;
        try {
            ServerSocket ssckt = new ServerSocket(RECEIVER_PORT);
            while (true) {
                Socket socket = ssckt.accept();
                ServerHost rt = new ServerHost(socket);
                rt.start();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
