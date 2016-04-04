package com.example.victor.couchbaseorm.model;

import com.couchbaseorm.library.Model;
import com.couchbaseorm.library.annotation.Column;
import com.couchbaseorm.library.annotation.Table;

/**
 * Created by Oesia on 31/03/2016.
 */
@Table(name = "Message")
public class Message extends Model {

    public Message() {
    }

    public Message(String text) {
        this.text = text;
    }

    @Column
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
