package com.example.victor.couchbaseorm;

import com.couchbaseorm.library.CouchBaseOrm;

/**
 * Created by Oesia on 31/03/2016.
 */
public class Application extends android.app.Application {

    @Override public void onCreate() {
        super.onCreate();

        CouchBaseOrm.initialize(this);
    }
}
