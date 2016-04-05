package com.couchbaseorm.library;


import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;


public final class CouchBaseOrm {

	private static Context sContext;

	private static Manager sManager;
	private static Database sDatabase;

	public static void initialize(Context context){
		CouchBaseOrm.sContext = context;
	}

	public static Manager getManager(){
		if(sManager == null) {
			try {
				sManager = new Manager(new AndroidContext(sContext), Manager.DEFAULT_OPTIONS);
			} catch(IOException ex){
				ex.printStackTrace();
			}
		}

		return sManager;
	}

	public static Database getDatabase(){
		if(sDatabase == null) {
			try {
				sDatabase = getManager().getDatabase("couchbase_db");
			} catch(CouchbaseLiteException ex){
				ex.printStackTrace();
			}
		}

		return sDatabase;

	}

}
