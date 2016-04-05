package com.example.victor.couchbaseorm.model;

import com.couchbaseorm.library.Model;

/**
 * Created by Oesia on 31/03/2016.
 */
public class Message extends Model {

    public Message() {
    }

    public Message(String text) {
        this.text = text;
    }

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
