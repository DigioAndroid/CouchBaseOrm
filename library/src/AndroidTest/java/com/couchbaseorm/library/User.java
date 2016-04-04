package com.couchbaseorm.library;

import com.couchbaseorm.library.annotation.Column;
import com.couchbaseorm.library.annotation.Table;

/**
 * Created by Oesia on 31/03/2016.
 */
@Table(name = "User")
public class User extends Model {

    @Column
    private String name;

    @Column
    private String age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }
}
