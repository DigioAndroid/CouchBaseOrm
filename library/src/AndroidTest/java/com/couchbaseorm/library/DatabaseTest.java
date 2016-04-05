package com.couchbaseorm.library;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor on 14/12/2015.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTest extends InstrumentationTestCase {

    @Before
    public void setUp() throws Exception {
        CouchBaseOrm.initialize(InstrumentationRegistry.getContext());
    }

    @Test
    public void testSaveEntity(){
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());
    }

    @Test
    public void testSaveAllEntities(){

        //Clear table
        boolean result = User.deleteAll(User.class);
        assertTrue(result);

        List<User> users = givenAUserList();
        User.saveAll(User.class, users, success -> {

            assertTrue(success);

            //Count results
            List<User> queryUsers = User.loadAll(User.class);
            assertEquals(users.size(), queryUsers.size());
        });


    }

    @Test
    public void testDeleteEntity(){

        //First save entity
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Delete entity
        boolean result = user.delete();
        assertTrue(result);
    }

    @Test
    public void testDeleteAllEntitries(){

        List<User> users = givenAUserList();
        User.saveAll(User.class, users, success -> {

            assertTrue(success);

            //Delete entries
            boolean result = User.deleteAll(User.class);
            assertTrue(result);

            //Count entries
            List<User> usersAfterDelete = User.loadAll(User.class);
            assertEquals(0, usersAfterDelete.size());
        });

    }

    @Test
    public void testLoadEntity(){

        //First save entity
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Load entity
        user = User.load(User.class, user.getDocumentId());
        assertNotNull(user.getDocumentId());

    }


    @Test
    public void testLoadAllEntities(){

        //Clear table
        boolean result = User.deleteAll(User.class);
        assertTrue(result);

        //Save first entity
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Save second entity
        user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Load all entities
        List<User> users = User.loadAll(User.class);
        assertEquals(2, users.size());

    }

    @Test
    public void testUpdateEntity(){

        //First save entity
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Update entity
        user.setName("Alex");
        user.save();

        //Load updated entity
        user = User.load(User.class, user.getDocumentId());
        assertEquals("Alex", user.getName());
    }

    @Test
    public void testQueryEntity(){

        //First save entity
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Query entity
        List<User> find = User.findByField(User.class, "name", "Juan");
        assertTrue(find.size() > 0);
        assertNotNull(find.get(0).getDocumentId());

    }

    @Test
    public void testQueryFirstEntity(){

        //First save entity
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Query entity
        User result = User.findFirstByField(User.class, "name", "Juan");
        assertNotNull(result);
        assertNotNull(result.getDocumentId());

    }

    @Test
    public void testSeveralUpdatesInARow(){

        //First save entity
        User user = givenAUser();
        user.save();
        assertNotNull(user.getDocumentId());

        //Update entity
        user.setName("Alex");
        user.save();

        //Load updated entity
        user = User.load(User.class, user.getDocumentId());
        assertEquals("Alex", user.getName());

        //Update entity
        user.setAge("50");
        user.save();

        //Load updated entity
        user = User.load(User.class, user.getDocumentId());
        assertEquals("Alex", user.getName());
        assertEquals("50", user.getAge());

        //Update entity
        user.setAge(null);
        user.save();

        //Load updated entity
        user = User.load(User.class, user.getDocumentId());
        assertEquals("Alex",user.getName());
        assertNull(user.getAge());

        //Update entity
        user.setAge("60");
        user.setName("Andres");
        user.save();


        //Load updated entity
        user = User.load(User.class, user.getDocumentId());
        assertEquals("Andres", user.getName());
        assertEquals("60", user.getAge());

    }

    public User givenAUser(){
        User user = new User();
        user.setName("Juan");
        user.setAge("40");
        return user;
    }

    public List<User> givenAUserList(){
        List<User> users = new ArrayList<>();
        users.add(givenAUser());
        users.add(givenAUser());
        users.add(givenAUser());
        users.add(givenAUser());
        return users;

    }


}
