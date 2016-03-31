package com.couchbaseorm.library;


import android.content.Context;

import com.couchbase.lite.Database;
import com.couchbaseorm.library.util.Log;


public final class CouchBaseOrm {


	public static void initialize(Context context) {
		initialize(new Configuration.Builder(context).create());
	}

	public static void initialize(Configuration configuration) {
		initialize(configuration, false);
	}

	public static void initialize(Context context, boolean loggingEnabled) {
		initialize(new Configuration.Builder(context).create(), loggingEnabled);
	}

	public static void initialize(Configuration configuration, boolean loggingEnabled) {
		// Set logging enabled first
		setLoggingEnabled(loggingEnabled);
		Cache.initialize(configuration);
	}


	public static void setLoggingEnabled(boolean enabled) {
		Log.setEnabled(enabled);
	}

	public static Database getDatabase(){
		return Cache.getDatabase();
	}
}
