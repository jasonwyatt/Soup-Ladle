package jwf.soupladle.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import jwf.soupladle.Bind;
import jwf.soupladle.SoupLadle;

public class MainActivity extends AppCompatActivity {
    @Bind(R.id.hello_world)
    public TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SoupLadle.bind(this);
        textView.setText("The binding worked!");
    }
}
