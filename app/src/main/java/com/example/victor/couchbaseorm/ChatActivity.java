package com.example.victor.couchbaseorm;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import com.example.victor.couchbaseorm.adapter.ChatAdapter;
import com.example.victor.couchbaseorm.model.Message;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;

public class ChatActivity extends AppCompatActivity {

    @Bind(R.id.list)
    RecyclerView mList;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.input)
    EditText mEdit;

    @Bind(R.id.sendButton)
    Button sendButton;

    private ChatAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        initList();
    }

    public void initList(){
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        mAdapter = new ChatAdapter();
        mList.setLayoutManager(manager);
        mList.setAdapter(mAdapter);

        Message.observeChanges(Message.class).observeOn(AndroidSchedulers.mainThread()).subscribe(messages -> mAdapter.update(messages));
    }

    @OnClick(R.id.sendButton)
    public void sendButtonClicked(){

        String text = mEdit.getText().toString();

        if(!TextUtils.isEmpty(text)){
            new Message(text).save();
        }
    }


}
