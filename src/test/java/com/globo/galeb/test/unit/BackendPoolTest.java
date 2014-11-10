package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.BackendPool;
import com.globo.galeb.core.entity.IJsonable;

public class BackendPoolTest {

    private Vertx vertx = mock(Vertx.class);

    @Test
    public void createPoolWithDefaultContructor() {
        BackendPool backendPool = new BackendPool();
        assertThat(String.format("%s", backendPool)).isEqualTo("UNDEF");
    }

    @Test
    public void createPoolWithContructorWithId() {
        String id = "NOT_UNDEF";
        BackendPool backendPool = new BackendPool(id);
        assertThat(String.format("%s", backendPool)).isEqualTo(id);
    }

    @Test
    public void createPoolWithContructorWithJson() {
        String id = "NOT_UNDEF";
        JsonObject json = new JsonObject();
        json.putString(IJsonable.ID_FIELDNAME, id);
        BackendPool backendPool = new BackendPool(json);

        assertThat(String.format("%s", backendPool)).isEqualTo(id);
    }

    @Test
    public void poolsAreEqualsIfIdIsTheSame() {
        String sameId = "sameId";
        BackendPool pool1 = new BackendPool(sameId);
        BackendPool pool2 = new BackendPool(sameId);
        assertThat(pool1).isEqualTo(pool2);
    }

    @Test
    public void poolsAreEqualsIfIdIsNotDefined() {
        BackendPool pool1 = new BackendPool();
        BackendPool pool2 = new BackendPool();
        assertThat(pool1).isEqualTo(pool2);
    }

    @Test
    public void poolsAreNotEqualsIfIdIsDifferent() {
        String oneId = "oneId";
        String otherId = "otherId";

        BackendPool pool1 = new BackendPool(oneId);
        BackendPool pool2 = new BackendPool(otherId);
        assertThat(pool1).isNotEqualTo(pool2);
    }

    private boolean createBackend(String backendId, BackendPool backendPool) {
        return createBackend(backendId, backendPool, true);
    }

    private boolean createBackend(String backendId, BackendPool backendPool, boolean isElegible) {
        JsonObject json = new JsonObject();
        json.putString(IJsonable.ID_FIELDNAME, backendId);

        Backend backend = (Backend) new Backend(json).setPlataform(vertx);
        if (isElegible) {
            return backendPool.addEntity(backend);
        } else {
            return backendPool.addBadBackend(backend);
        }
    }

    private boolean removeBackend(String backendId, BackendPool backendPool) {
        return removeBackend(backendId, backendPool, true);
    }

    private boolean removeBackend(String backendId, BackendPool backendPool, boolean isElegible) {
        JsonObject json = new JsonObject();
        json.putString(IJsonable.ID_FIELDNAME, backendId);

        Backend backend = (Backend) new Backend(json).setPlataform(vertx);
        if (isElegible) {
            return backendPool.removeEntity(backend);
        } else {
            return backendPool.removeBadBackend(backend);
        }
    }

    @Test
    public void addNewBackend() {
        String backendId = "newbackend";
        BackendPool backendPool = new BackendPool();
        boolean backedCreated = createBackend(backendId, backendPool);

        assertThat(backedCreated).isTrue();
        assertThat(backendPool.getNumEntities()).isEqualTo(1);
        assertThat(backendPool.getEntityById(backendId)).isNotNull();
    }

    @Test
    public void addExistingBackend() {
        String backendId = "newbackend";
        BackendPool backendPool = new BackendPool();
        createBackend(backendId, backendPool);
        boolean backedCreated = createBackend(backendId, backendPool);

        assertThat(backedCreated).isFalse();
        assertThat(backendPool.getNumEntities()).isEqualTo(1);
        assertThat(backendPool.getEntityById(backendId)).isNotNull();
    }

    @Test
    public void removeExistingBackend() {
        String backendId = "mybackend";
        BackendPool backendPool = new BackendPool();
        createBackend(backendId, backendPool);
        boolean backedRemoved = removeBackend(backendId, backendPool);

        assertThat(backedRemoved).isTrue();
        assertThat(backendPool.getNumEntities()).isEqualTo(0);
        assertThat(backendPool.getEntityById(backendId)).isNull();
    }

    @Test
    public void removeAbsentBackend() {
        String backendId = "mybackend";
        BackendPool backendPool = new BackendPool();
        boolean backedRemoved = removeBackend(backendId, backendPool);

        assertThat(backedRemoved).isFalse();
        assertThat(backendPool.getNumEntities()).isEqualTo(0);
        assertThat(backendPool.getEntityById(backendId)).isNull();
    }

    @Test
    public void addNewBadBackend() {
        String backendId = "newbackend";
        BackendPool backendPool = new BackendPool();
        boolean backedCreated = createBackend(backendId, backendPool, false);

        assertThat(backedCreated).isTrue();
        assertThat(backendPool.getNumBadBackend()).isEqualTo(1);
        assertThat(backendPool.getBadBackendById(backendId)).isNotNull();
    }

    @Test
    public void addExistingBadBackend() {
        String backendId = "newbackend";
        BackendPool backendPool = new BackendPool();
        createBackend(backendId, backendPool, false);
        boolean backedCreated = createBackend(backendId, backendPool, false);

        assertThat(backedCreated).isFalse();
        assertThat(backendPool.getNumBadBackend()).isEqualTo(1);
        assertThat(backendPool.getBadBackendById(backendId)).isNotNull();
    }

    @Test
    public void removeExistingBadBackend() {
        String backendId = "mybackend";
        BackendPool backendPool = new BackendPool();
        createBackend(backendId, backendPool, false);
        boolean backedRemoved = removeBackend(backendId, backendPool, false);

        assertThat(backedRemoved).isTrue();
        assertThat(backendPool.getNumEntities()).isEqualTo(0);
        assertThat(backendPool.getEntityById(backendId)).isNull();
    }

    @Test
    public void removeAbsentBadBackend() {
        String backendId = "mybackend";
        BackendPool backendPool = new BackendPool();
        boolean backedRemoved = removeBackend(backendId, backendPool, false);

        assertThat(backedRemoved).isFalse();
        assertThat(backendPool.getNumEntities()).isEqualTo(0);
        assertThat(backendPool.getEntityById(backendId)).isNull();
    }
}
