package com.example.victor.couchbaseorm;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.couchbaseorm.library.Model;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        User user = new User();
        user.setAge("43");
        user.setName("fds");

        user.save();

        String id = user.getDocumentId();
        User u2 = User.load(User.class, id);

        String age = u2.getAge();
        u2.setAge("44");
        u2.save();

        id = u2.getDocumentId();

        User u3 = User.load(User.class, id);
        String age3 = u3.getAge();

        List<User> find = User.findByField(User.class, "age", "44");
        User find2 = User.findFirstByField(User.class, "age", "44");


    }


}
