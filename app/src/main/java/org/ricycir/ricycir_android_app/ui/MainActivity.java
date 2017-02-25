package org.ricycir.ricycir_android_app.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.ricycir.ricycir_android_app.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button gotoWatch = (Button)findViewById(R.id.gotoWatch);
        gotoWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.print("click");
                Intent intent = new Intent(MainActivity.this, WatchActivity.class);
                startActivity(intent);
            }
        });
    }
}
