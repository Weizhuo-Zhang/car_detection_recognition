package com.example.cardetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ProcessActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView imageView;
    private MediaMetadataRetriever mmr;
    private int frame = 0;
    private String path;
    private String title;
    private String size;
    private String processed_path;
    private String processed_title;
    private String processed_size;
    private Socket socket;
    private static final int SERVERPORT = 8888;
    private static final String SERVER_IP = "10.12.136.160";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);
        Button process = (Button) findViewById(R.id.process_file);
        process.setOnClickListener(this);
        Intent intent = getIntent();
        String activity_name = intent.getStringExtra("activity");
        Log.e("ProcessActivity","传过来的字符串是"+activity_name);
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork()
                .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
                .build());
        try {
            if(activity_name.equals(VideoSelectActivity.class.getSimpleName())) {
                title = intent.getStringExtra("title");
                path = intent.getStringExtra("path");
                size = intent.getStringExtra("size");
                connect(SERVER_IP, SERVERPORT);
                Log.e("ProcessActivity","processed_path: " + processed_path);
                mmr = new MediaMetadataRetriever();
                mmr.setDataSource(processed_path);
                TextView processed_info = (TextView) findViewById(R.id.processed_info);
                processed_info.setVisibility(View.VISIBLE);
//                Button next_frame = (Button) findViewById(R.id.next_frame);
                Button play_video = (Button) findViewById(R.id.play_video);
                imageView = (ImageView) findViewById(R.id.video_frame);
                imageView.setImageBitmap(mmr.getFrameAtTime(frame*1000, MediaMetadataRetriever.OPTION_CLOSEST));
//                next_frame.setVisibility(View.VISIBLE);
//                next_frame.setOnClickListener(this);
                play_video.setVisibility(View.VISIBLE);
                play_video.setOnClickListener(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.process_file:
                Intent process = new Intent(ProcessActivity.this, VideoSelectActivity.class);
                startActivity(process);
                break;
//            case R.id.next_frame:
//                frame ++;
//                long videolength = Long.valueOf(processed_size);
//                if (frame < videolength/1000) {
//                    imageView.setImageBitmap(mmr.getFrameAtTime(frame*1000000, MediaMetadataRetriever.OPTION_CLOSEST));
//                }
//                break;
            case R.id.play_video:
                Intent play_video = new Intent(ProcessActivity.this, SimplePlayer.class);
                play_video.putExtra("title", processed_title);
                play_video.putExtra("screenshot", bitmap2Bytes(Bitmap.CompressFormat.JPEG, 75,
                        mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)));
                play_video.putExtra("path", processed_path);
                startActivity(play_video);
                break;
            default:
                break;
        }
    }

    private byte[] bitmap2Bytes(Bitmap.CompressFormat type, int quality, Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(type, quality, baos);
        return baos.toByteArray();
    }

    private void connect(String ip, int port) throws Exception {
        Log.e("ProcessActivity", "start connect");
        InetAddress serverAddr = InetAddress.getByName(ip);
        socket = new Socket(serverAddr, port);
        Log.e("ProcessActivity", "socket connected");
        PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())),
                true);
        out.println(title);
        out.flush();
        out.println(size);
        out.flush();
        File file = new File(path);
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int i = 0;
        while ((i = in.read(b)) != -1) {
            baos.write(b, 0,i);
        }
        baos.close();
        in.close();
        OutputStream os = socket.getOutputStream();
        os.write(baos.toByteArray());
        Log.e("ProcessActivity", "send length: " + baos.toByteArray().length);
//        Thread.sleep(1500);
        os.flush();
        Log.e("ProcessActivity", "File sent successfully!");

        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        processed_title = br.readLine();
        Log.e("ProcessActivity", "File to receive:" + processed_title);
        processed_size = br.readLine();
        Log.e("ProcessActivity", "File size: " + processed_size);

        processed_path = path.substring(0, path.length()-title.length()) + processed_title;
        File processed_file = new File(processed_path);
        int new_size = Integer.parseInt(processed_size);
        byte[] contents = new byte[new_size];
        //Initialize the FileOutputStream to the output file's full path.
        Log.e("ProcessActivity", "start to create file output stream");
        FileOutputStream fos = new FileOutputStream(processed_file);
        Log.e("ProcessActivity", "start to create buffered output stream");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        Log.e("ProcessActivity", "start to create inputStream");
        InputStream is = socket.getInputStream();
        //No of bytes read in one read() call
        int bytesRead = 0;
        int count = 0;
        Log.e("ProcessActivity", "start to receive");
        while(count < new_size)
        {
            if(new_size - count > contents.length){
                bytesRead=is.read(contents, 0, contents.length);
            }
            else{
                bytesRead = is.read(contents, 0, new_size - count);
            }
            bos.write(contents, 0, bytesRead);
            count += bytesRead;
            bos.flush();
        }
        bos.flush();
        bos.close();
        socket.close();
        Log.e("ProcessActivity", "file save successfully");
    }


}