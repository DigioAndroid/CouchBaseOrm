package com.couchbaseorm.library;

/**
 * Created by Oesia on 31/03/2016.
 */
public class User extends Model {

    private String name;

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
