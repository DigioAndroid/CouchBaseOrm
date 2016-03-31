package com.couchbaseorm.library;


import android.content.Context;
import android.text.TextUtils;


import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbaseorm.library.util.Log;

import java.io.IOException;
import java.util.Collection;

public final class Cache {


	private static Context sContext;
	private static ModelInfo sModelInfo;
	private static boolean sIsInitialized = false;
	private static Manager sManager;
	private static Database sDatabase;

	private Cache() {
	}

	public static synchronized void initialize(Configuration configuration) {
		if (sIsInitialized) {
			Log.v("CouchBaseOrm already initialized.");
			return;
		}

		sContext = configuration.getContext();
		sModelInfo = new ModelInfo(configuration);

		sIsInitialized = true;

		Log.v("CouchBaseOrm initialized successfully.");
	}


	// Database access
	
	public static boolean isInitialized() {
		return sIsInitialized;
	}

	// Context access

	public static Context getContext() {
		return sContext;
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

	public static synchronized Collection<TableInfo> getTableInfos() {
		return sModelInfo.getTableInfos();
	}

	public static synchronized TableInfo getTableInfo(Class<? extends Model> type) {
		return sModelInfo.getTableInfo(type);
	}

	public static synchronized String getTableName(Class<? extends Model> type) {
		return sModelInfo.getTableInfo(type).getTableName();
	}
}
