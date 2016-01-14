package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AdvancedTools extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_tools);
        Button snapshot = (Button) findViewById(R.id.snapshot_button);
        snapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZooGate.popupMessage("Backing up all your data ...");
                ZooGate.readShellCommandNotify("5", "/data/media/0/ZooKeeper/backup.sh");
            }
        });
    }
}
