package com.couchbaseorm.library;


import android.text.TextUtils;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbaseorm.library.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Model {


    private String documentId;

    private final static String TYPE_FIELD = "type";

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    private String getType() {
        return getClass().getName();
    }

    /**
     * Create or update a document in database
     */
    public void save() {

        try {
            Database db = Cache.getDatabase();
            TableInfo info = Cache.getTableInfo(getClass());

            if (db != null && info != null) {
                if (!TextUtils.isEmpty(documentId) && db.getExistingDocument(documentId) != null) {
                    update();
                } else {
                    create();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create a new document in database
     */
    private void create() {
        try {
            Database db = Cache.getDatabase();
            TableInfo info = Cache.getTableInfo(getClass());

            if (db != null && info != null) {
                // Create a new document and add data
                Document document = null;

                if (!TextUtils.isEmpty(documentId)) {
                    document = db.getDocument(documentId);
                } else {
                    document = db.createDocument();
                }

                Map<String, Object> map = new HashMap<String, Object>();
                for (Field field : info.getFields()) {
                    field.setAccessible(true);
                    final String fieldName = info.getColumnName(field);
                    if (field.get(this) != null) {
                        map.put(fieldName, field.get(this));
                    }
                }

                map.put(TYPE_FIELD, getType());

                // Save the properties to the document
                document.putProperties(map);
                documentId = document.getId();
                Log.v("Model: " + getClass().getSimpleName(), "Document " + documentId + " created");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Update an existing document in database
     */
    private void update() {

        try {

            Database db = Cache.getDatabase();
            final TableInfo info = Cache.getTableInfo(getClass());

            if (db != null && info != null) {
                // Create a new document and add data
                final Document document = db.getDocument(documentId);
                SavedRevision latestRevision = document.getCurrentRevision();
                UnsavedRevision newUnsavedRevision = latestRevision.createRevision();

                Map<String, Object> map = new HashMap<>();
                for (Field field : info.getFields()) {
                    field.setAccessible(true);
                    final String fieldName = info.getColumnName(field);
                    if (field.get(this) != null) {
                        map.put(fieldName, field.get(this));
                    }
                }
                map.put(TYPE_FIELD, getType());

                newUnsavedRevision.setUserProperties(map);
                newUnsavedRevision.save();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get document from database from its document id
     *
     * @return document or null if any error is thrown
     */
    public static <T extends Model> T load(Class<T> type, String documentId) {

        try {
            TableInfo info = Cache.getTableInfo(type);
            Database db = Cache.getDatabase();

            if (db != null && info != null) {
                Document doc = db.getExistingDocument(documentId);

                if (doc != null) {
                    T model = type.newInstance();
                    for (Field field : info.getFields()) {
                        Object value = doc.getProperty(field.getName());
                        if (value != null) {
                            boolean access = field.isAccessible();
                            field.setAccessible(true);
                            field.set(model, value);
                            field.setAccessible(access);
                        }
                    }
                    model.setDocumentId(doc.getId());
                    return model;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

    }

    /**
     * Build entity from document object
     *
     * @return entity
     */
    private static <T extends Model> T loadFromDocument(Class<T> type, Document doc) {

        try {
            TableInfo info = Cache.getTableInfo(type);
            Database db = Cache.getDatabase();

            if (db != null && info != null) {

                T model = type.newInstance();
                for (Field field : info.getFields()) {
                    Object value = doc.getProperty(field.getName());
                    if (value != null) {
                        boolean access = field.isAccessible();
                        field.setAccessible(true);
                        field.set(model, value);
                        field.setAccessible(access);
                    }
                }
                model.setDocumentId(doc.getId());
                return model;

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Delete document in database
     *
     * @return true if sucess, false if fails
     */
    public final boolean delete() {

        try {
            Database db = Cache.getDatabase();

            if (db != null && !TextUtils.isEmpty(getDocumentId())) {
                Document d = db.getDocument(getDocumentId());
                d.delete();
                return true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }


    public static <T extends Model> List<T> findByField(Class<T> type, String field, Object value) {

        List<T> result = new ArrayList<>();

        try {
            TableInfo info = Cache.getTableInfo(type);
            Database db = Cache.getDatabase();

            if (db != null && info != null) {

                com.couchbase.lite.View view = db.getView(type.getName() + "_" + field);
                if (view.getMap() == null) {
                    Mapper map = (document, emitter) -> {
                        if (type.getName().equals(document.get("type"))) {
                            emitter.emit(document.get(field), null);
                        }
                    };
                    view.setMap(map, "1");
                }

                Query query = view.createQuery();
                List<Object> keys = new ArrayList<>();
                keys.add(value);
                query.setKeys(keys);
                QueryEnumerator enumerator = query.run();
                if (enumerator != null && enumerator.getCount() > 0) {
                    for (int i = 0; i < enumerator.getCount(); i++) {
                        T model = loadFromDocument(type, enumerator.getRow(i).getDocument());
                        if(model != null){
                            result.add(model);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    public static <T extends Model> T findFirstByField(Class<T> type, String field, Object value) {

        try {
            TableInfo info = Cache.getTableInfo(type);
            Database db = Cache.getDatabase();

            if (db != null && info != null) {

                com.couchbase.lite.View view = db.getView(type.getName() + "_" + field);
                if (view.getMap() == null) {
                    Mapper map = (document, emitter) -> {
                        if (type.getName().equals(document.get("type"))) {
                            emitter.emit(document.get(field), null);
                        }
                    };
                    view.setMap(map, "1");
                }

                Query query = view.createQuery();
                List<Object> keys = new ArrayList<>();
                keys.add(value);
                query.setKeys(keys);
                QueryEnumerator enumerator = query.run();
                if (enumerator != null && enumerator.getCount() > 0) {
                    return loadFromDocument(type, enumerator.getRow(0).getDocument());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
