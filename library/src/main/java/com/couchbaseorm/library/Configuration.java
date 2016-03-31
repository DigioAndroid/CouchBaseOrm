package com.couchbaseorm.library;



import android.content.Context;


import com.couchbaseorm.library.util.Log;
import com.couchbaseorm.library.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Configuration {

	private Context mContext;
	private String mDatabaseName;
	private List<Class<? extends Model>> mModelClasses;


	private Configuration(Context context) {
		mContext = context;
	}


	public Context getContext() {
		return mContext;
	}

	public String getDatabaseName() {
		return mDatabaseName;
	}

	public List<Class<? extends Model>> getModelClasses() {
		return mModelClasses;
	}

	public boolean isValid() {
		return mModelClasses != null && mModelClasses.size() > 0;
	}

	public static class Builder {


		private static final String DB_NAME = "DB_NAME";
		private static final String DB_VERSION = "DB_VERSION";
		private final static String DB_MODELS = "DB_MODELS";
		private static final String DEFAULT_DB_NAME = "Application.db";


		private Context mContext;

		private String mDatabaseName;
		private List<Class<? extends Model>> mModelClasses;


		public Builder(Context context) {
			mContext = context.getApplicationContext();
		}


		public Builder setDatabaseName(String databaseName) {
			mDatabaseName = databaseName;
			return this;
		}


		public Builder addModelClass(Class<? extends Model> modelClass) {
			if (mModelClasses == null) {
				mModelClasses = new ArrayList<Class<? extends Model>>();
			}

			mModelClasses.add(modelClass);
			return this;
		}

		public Builder addModelClasses(Class<? extends Model>... modelClasses) {
			if (mModelClasses == null) {
				mModelClasses = new ArrayList<Class<? extends Model>>();
			}

			mModelClasses.addAll(Arrays.asList(modelClasses));
			return this;
		}

		public Builder setModelClasses(Class<? extends Model>... modelClasses) {
			mModelClasses = Arrays.asList(modelClasses);
			return this;
		}


		public Configuration create() {
			Configuration configuration = new Configuration(mContext);

			// Get database name from meta-data
			if (mDatabaseName != null) {
				configuration.mDatabaseName = mDatabaseName;
			} else {
				configuration.mDatabaseName = getMetaDataDatabaseNameOrDefault();
			}


			// Get model classes from meta-data
			if (mModelClasses != null) {
				configuration.mModelClasses = mModelClasses;
			} else {
				final String modelList = ReflectionUtils.getMetaData(mContext, DB_MODELS);
				if (modelList != null) {
					configuration.mModelClasses = loadModelList(modelList.split(","));
				}
			}


			return configuration;
		}

		//////////////////////////////////////////////////////////////////////////////////////
		// PRIVATE METHODS
		//////////////////////////////////////////////////////////////////////////////////////

		// Meta-data methods

		private String getMetaDataDatabaseNameOrDefault() {
			String aaName = ReflectionUtils.getMetaData(mContext, DB_NAME);
			if (aaName == null) {
				aaName = DEFAULT_DB_NAME;
			}

			return aaName;
		}


		private List<Class<? extends Model>> loadModelList(String[] models) {
			final List<Class<? extends Model>> modelClasses = new ArrayList<Class<? extends Model>>();
			final ClassLoader classLoader = mContext.getClass().getClassLoader();
			for (String model : models) {
				try {
					Class modelClass = Class.forName(model.trim(), false, classLoader);
					if (ReflectionUtils.isModel(modelClass)) {
						modelClasses.add(modelClass);
					}
				} catch (ClassNotFoundException e) {
					Log.e("Couldn't create class.", e);
				}
			}

			return modelClasses;
		}
	}


}
