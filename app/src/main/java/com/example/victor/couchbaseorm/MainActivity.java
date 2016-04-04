package com.example.victor.couchbaseorm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.replicator.Replication;
import com.couchbaseorm.library.CouchBaseOrm;
import com.example.victor.couchbaseorm.model.User;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView txt =  ((TextView) findViewById(R.id.text));

        findViewById(R.id.fab).setOnClickListener(v -> {
            User user = new User();
            user.setAge(new Date().getTime() + "");
            user.setName("fds");
            user.save();
        });

        try {
            startReplications();
        } catch(Exception ex){
            ex.printStackTrace();
        }

        User.observeChanges(User.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(users -> txt.setText("Usuarios: " + users.size()), error -> error.printStackTrace());
    }

    private void startReplications() throws CouchbaseLiteException {
        Replication pull = CouchBaseOrm.getDatabase().createPullReplication(this.createSyncURL(false));
        Replication push = CouchBaseOrm.getDatabase().createPushReplication(this.createSyncURL(false));
        pull.setContinuous(true);
        push.setContinuous(true);
        pull.start();
        push.start();
    }

    private URL createSyncURL(boolean isEncrypted){
        URL syncURL = null;
        String host = "http://10.166.110.250";
        String port = "4984";
        String dbName = "sync_gateway";
        try {
            syncURL = new URL(host + ":" + port + "/" + dbName);
        } catch (MalformedURLException me) {
            me.printStackTrace();
        }
        return syncURL;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_chat:
                startActivity(new Intent(this, ChatActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
