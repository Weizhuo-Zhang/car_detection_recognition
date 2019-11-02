package com.example.cardetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button process = (Button) findViewById(R.id.process);
        Button device = (Button) findViewById(R.id.device);
        Button support = (Button) findViewById(R.id.support);
        process.setOnClickListener(this);
        device.setOnClickListener(this);
        support.setOnClickListener(this);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        switch(item.getItemId()) {
//            case R.id.add_item:
//                Toast.makeText(this, "you click add", Toast.LENGTH_SHORT).show();
//                break;
//            case R.id.remove_item:
//                Toast.makeText(this, "you click remove", Toast.LENGTH_SHORT).show();
//                break;
//            default:
//        }
//        return true;
//    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.process:
                Intent process = new Intent(MainActivity.this, ProcessActivity.class);
                process.putExtra("activity", this.getLocalClassName());
                startActivity(process);
                break;
            case R.id.device:
                Intent device = new Intent(MainActivity.this, DetectorActivity.class);
                startActivity(device);
                break;
            case R.id.support:
                Intent support = new Intent(MainActivity.this, SupportActivity.class);
//                Intent intent = new Intent("com.example.cardetection.ACTION_START");
                startActivity(support);
                break;
            default:
                break;
        }
    }
}
