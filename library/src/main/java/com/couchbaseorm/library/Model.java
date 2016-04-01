package com.couchbaseorm.library;


import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbaseorm.library.util.Log;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;

public abstract class Model {

    private String documentId;

    private final static String DOCUMENT_ID_FIELD = "documentId";

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
     *
     * @return true if sucess, false in other case
     */
    public boolean save() {

        try {
            Database db = Cache.getDatabase();
            TableInfo info = Cache.getTableInfo(getClass());

            if (db != null && info != null) {
                if (!TextUtils.isEmpty(documentId) && db.getExistingDocument(documentId) != null) {
                    return update();
                } else {
                    return create();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * Create a new document in database
     *
     * @return true if sucess, false in other case
     */
    private boolean create() {
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
                return true;

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * Update an existing document in database
     *
     * @return true if sucess, false in other case
     */
    private boolean update() {

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
                Log.v("Model: " + getClass().getSimpleName(), "Document " + documentId + " updated");
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get document from database from its document id
     *
     * @return document or null if any error is thrown
     */
    public static <T extends Model> T load(@NotNull Class<T> type,@NotNull String documentId) {

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
     * Get all documents for the given type
     *
     * @return list of all documents with the type
     */
    public static <T extends Model> List<T> loadAll(@NotNull Class<T> type) {

        List<T> result = new ArrayList<>();

        try {
            TableInfo info = Cache.getTableInfo(type);
            Database db = Cache.getDatabase();

            if (db != null && info != null) {

                com.couchbase.lite.View view = db.getView(type.getName() + "_all");
                if (view.getMap() == null) {
                    Mapper map = (document, emitter) -> {
                        if (type.getName().equals(document.get("type"))) {
                            emitter.emit(document.get(DOCUMENT_ID_FIELD), null);
                        }
                    };
                    view.setMap(map, "1");
                }

                Query query = view.createQuery();
                QueryEnumerator enumerator = query.run();
                if (enumerator != null && enumerator.getCount() > 0) {
                    result = buildQuery(type, enumerator);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;

    }


    /**
     * Query all documents for the given type and notify changes to the listener
     */
    public static <T extends Model> Observable<List<T>> loadAllNotifyChanges(@NotNull Class<T> type) {

        return Observable.create(subscriber -> {
                try {
                    TableInfo info = Cache.getTableInfo(type);
                    Database db = Cache.getDatabase();

                    if (db != null && info != null) {

                        com.couchbase.lite.View view = db.getView(type.getName() + "_all");
                        if (view.getMap() == null) {
                            Mapper map = (document, emitter) -> {
                                if (type.getName().equals(document.get("type"))) {
                                    emitter.emit(document.get(DOCUMENT_ID_FIELD), null);
                                }
                            };
                            view.setMap(map, "1");
                        }

                        Query query = view.createQuery();

                        LiveQuery liveQuery = query.toLiveQuery();
                        liveQuery.addChangeListener(event -> {
                            if (event.getSource().equals(liveQuery)) {

                                QueryEnumerator enumerator = event.getRows();

                                if (enumerator != null && enumerator.getCount() > 0) {
                                    subscriber.onNext(buildQuery(type, enumerator));
                                }
                            }
                        });

                        liveQuery.start();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    subscriber.onError(ex);
                }
            });

    }

    private static <T extends Model> List<T> buildQuery(Class<T> type, QueryEnumerator query){
        List<T> result = new ArrayList<>();

        for (int i = 0; i < query.getCount(); i++) {
            T model = loadFromDocument(type, query.getRow(i).getDocument());
            if(model != null){
                result.add(model);
            }
        }

        return result;
    }

    /**
     * Build entity from document object
     *
     * @return entity
     */
    private static <T extends Model> T loadFromDocument(@NotNull Class<T> type,@NotNull Document doc) {

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
     * Save all entities in a single transacation
     *
     * @return document or null if any error is thrown
     */
    public static <T extends Model> void saveAll(@NotNull Class<T> type,@NotNull List<T> entities) {
        saveAll(type, entities, null);
    }

    /**
     * Save all entities in a single transacation
     *
     * @return document or null if any error is thrown
     * */
    public static <T extends Model> void saveAll(@NotNull Class<T> type,@NotNull List<T> entities,@Nullable TransactionListener listener) {

        try {
            TableInfo info = Cache.getTableInfo(type);
            Database db = Cache.getDatabase();

            if (db != null && info != null) {
                db.runInTransaction(() -> {
                    for (T entity : entities) {
                        if (!entity.save()) {
                            //Operation failed
                            if (listener != null) {
                                listener.onTransactionEnd(false);
                            }
                            return false;
                        }
                    }
                    if (listener != null) {
                        listener.onTransactionEnd(true);
                    }
                    return true;
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    /**
     * Delete all documents for the given type
     *
     * @return true if sucess, false in other case
     */
    public static boolean deleteAll(@NotNull Class type) {

        try {
            TableInfo info = Cache.getTableInfo(type);
            Database db = Cache.getDatabase();

            if (db != null && info != null) {

                com.couchbase.lite.View view = db.getView(type.getName() + "_all");
                if (view.getMap() == null) {
                    Mapper map = (document, emitter) -> {
                        if (type.getName().equals(document.get("type"))) {
                            emitter.emit(document.get(DOCUMENT_ID_FIELD), null);
                        }
                    };
                    view.setMap(map, "1");
                }

                Query query = view.createQuery();
                QueryEnumerator enumerator = query.run();
                if (enumerator != null && enumerator.getCount() > 0) {
                    for (int i = 0; i < enumerator.getCount(); i++) {
                        enumerator.getRow(i).getDocument().delete();
                    }
                }

                return true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;

    }

    /**
     * Get all entities with a concrete field value
     *
     * @return list of all coincidences
     */
    public static <T extends Model> List<T> findByField(@NotNull Class<T> type,@NotNull String field,@NotNull Object value) {

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
                    result = buildQuery(type, enumerator);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    /**
     * Get all entities with a concrete field value and listen changes with a listener
     *
     * */
    public static <T extends Model> Observable<List<T>> findByFieldNotifyChanges(@NotNull Class<T> type,@NotNull String field,@NotNull Object value) {

        return Observable.create(subscriber -> {
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
                    LiveQuery liveQuery = query.toLiveQuery();
                    liveQuery.addChangeListener(event -> {
                        if (event.getSource().equals(liveQuery)) {

                            QueryEnumerator enumerator = event.getRows();

                            if (enumerator != null && enumerator.getCount() > 0) {
                                subscriber.onNext(buildQuery(type, enumerator));
                            }
                        }
                    });

                    liveQuery.start();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                subscriber.onError(ex);
            }
        });

    }

    /**
     * Get first entity with a concrete field value
     *
     * @return list of all coincidences
     */
    public static <T extends Model> T findFirstByField(@NotNull Class<T> type,@NotNull String field,@NotNull Object value) {

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
