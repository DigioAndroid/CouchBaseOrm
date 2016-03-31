package com.couchbaseorm.library;


import android.content.Context;


import com.couchbaseorm.library.util.Log;
import com.couchbaseorm.library.util.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexFile;

final class ModelInfo {


	private Map<Class<? extends Model>, TableInfo> mTableInfos = new HashMap<Class<? extends Model>, TableInfo>();


	public ModelInfo(Configuration configuration) {
		if (!loadModelFromMetaData(configuration)) {
			try {
				scanForModel(configuration.getContext());
			}
			catch (IOException e) {
				Log.e("Couldn't open source path.", e);
			}
		}

		Log.i("ModelInfo loaded.");
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public Collection<TableInfo> getTableInfos() {
		return mTableInfos.values();
	}

	public TableInfo getTableInfo(Class<? extends Model> type) {
		return mTableInfos.get(type);
	}


	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	private boolean loadModelFromMetaData(Configuration configuration) {
		if (!configuration.isValid()) {
			return false;
		}

		final List<Class<? extends Model>> models = configuration.getModelClasses();
		if (models != null) {
			for (Class<? extends Model> model : models) {
				mTableInfos.put(model, new TableInfo(model));
			}
		}

		return true;
	}

	private void scanForModel(Context context) throws IOException {
		String packageName = context.getPackageName();
		String sourcePath = context.getApplicationInfo().sourceDir;
		List<String> paths = new ArrayList<String>();

		if (sourcePath != null && !(new File(sourcePath).isDirectory())) {
			DexFile dexfile = new DexFile(sourcePath);
			Enumeration<String> entries = dexfile.entries();

			while (entries.hasMoreElements()) {
				paths.add(entries.nextElement());
			}
		}
		// Robolectric fallback
		else {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Enumeration<URL> resources = classLoader.getResources("");

			while (resources.hasMoreElements()) {
				String path = resources.nextElement().getFile();
				if (path.contains("bin") || path.contains("classes")) {
					paths.add(path);
				}
			}
		}

		for (String path : paths) {
			File file = new File(path);
			scanForModelClasses(file, packageName, context.getClassLoader());
		}
	}

	private void scanForModelClasses(File path, String packageName, ClassLoader classLoader) {
		if (path.isDirectory()) {
			for (File file : path.listFiles()) {
				scanForModelClasses(file, packageName, classLoader);
			}
		}
		else {
			String className = path.getName();

			// Robolectric fallback
			if (!path.getPath().equals(className)) {
				className = path.getPath();

				if (className.endsWith(".class")) {
					className = className.substring(0, className.length() - 6);
				}
				else {
					return;
				}

				className = className.replace(System.getProperty("file.separator"), ".");

				int packageNameIndex = className.lastIndexOf(packageName);
				if (packageNameIndex < 0) {
					return;
				}

				className = className.substring(packageNameIndex);
			}

			try {
				Class<?> discoveredClass = Class.forName(className, false, classLoader);
				if (ReflectionUtils.isModel(discoveredClass)) {
					@SuppressWarnings("unchecked")
					Class<? extends Model> modelClass = (Class<? extends Model>) discoveredClass;
					mTableInfos.put(modelClass, new TableInfo(modelClass));
				}

			}
			catch (Exception e) {
				Log.e("Couldn't create class.", e);
			}

		}
	}
}
