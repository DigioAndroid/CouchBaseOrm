package com.couchbaseorm.library;

/**
 * Created by Oesia on 01/04/2016.
 */
public interface TransactionListener {

    void onTransactionEnd(boolean success);
}
