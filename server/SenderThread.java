package server;

import java.io.*;
import java.net.*;

public class SenderThread extends Thread {
    private final String RECEIVER_IP = "localhost";
    private final int RECEIVER_PORT = 8888;
    private File file = null;
    private Socket sckt = null;

    public SenderThread(String filePath) {
        try {
            this.file = new File(filePath);
            this.sckt = new Socket(RECEIVER_IP, RECEIVER_PORT);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            PrintWriter pw = new PrintWriter(sckt.getOutputStream());
            pw.write(file.getName() + "\n");
            pw.flush();
            System.out.println("File name sent");
            pw.write(file.length() + "\n");
            pw.flush();

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //Get socket's output stream
            OutputStream os = sckt.getOutputStream();

            //Read File Contents into contents array
            byte[] contents;
            long fileLength = file.length();
            int size = (int)file.length();
            System.out.println(size);
            long current = 0;
            while(current!=fileLength){
                if(fileLength - current >= size)
                    current += size;
                else{
                    size = (int)(fileLength - current);
                    current = fileLength;
                }
                contents = new byte[size];
                bis.read(contents, 0, size);
                os.write(contents);
                System.out.print("Sending file ... "+(current*100)/fileLength+"% complete!\n");
                os.flush();
            }
            os.flush();
            System.out.println("File sent successfully!");

            BufferedReader br = new BufferedReader(new InputStreamReader(sckt.getInputStream()));
            String processed_name = br.readLine();
            System.out.println("File name receive: " + processed_name);
            String processed_size = br.readLine();
            System.out.println("File size receive: " + processed_size);

            File processed_file = new File("test.mp4");
            int new_size = Integer.parseInt(processed_size);
            byte[] content = new byte[new_size];
            //Initialize the FileOutputStream to the output file's full path.
            FileOutputStream fos = new FileOutputStream(processed_file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            InputStream is = sckt.getInputStream();
            //No of bytes read in one read() call
            int bytesRead = 0;
            int count = 0;
            while(count < new_size)
            {
                if(new_size - count > content.length){
                    bytesRead=is.read(content, 0, content.length);
                }
                else{
                    bytesRead = is.read(content, 0, new_size - count);
                }
                bos.write(content, 0, bytesRead);
                System.out.println(bytesRead + " bytes read");
                count += bytesRead;
                System.out.println(count + " bytes read in total");
                bos.flush();
            }
            bos.close();
            sckt.close();
            System.out.println("file save successfully");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
