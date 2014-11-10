package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.VirtualServer;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.rules.Rule;

public class VirtualServerTest {

    @Test
    public void createWithDefaultContructor() {
        VirtualServer virtualserver = new VirtualServer();
        assertThat(String.format("%s", virtualserver)).isEqualTo("UNDEF");
    }

    @Test
    public void createWithContructorWithId() {
        String id = "NOT_UNDEF";
        VirtualServer virtualserver = new VirtualServer(id);
        assertThat(String.format("%s", virtualserver)).isEqualTo(id);
    }

    @Test
    public void createWithContructorWithJson() {
        String id = "NOT_UNDEF";
        JsonObject json = new JsonObject();
        json.putString(IJsonable.ID_FIELDNAME, id);
        VirtualServer virtualserver = new VirtualServer(json);

        assertThat(String.format("%s", virtualserver)).isEqualTo(id);
    }

    @Test
    public void areEqualsIfIdIsTheSame() {
        String sameId = "sameId";
        VirtualServer group1 = new VirtualServer(sameId);
        VirtualServer group2 = new VirtualServer(sameId);
        assertThat(group1).isEqualTo(group2);
    }

    @Test
    public void areEqualsIfIdIsNotDefined() {
        VirtualServer group1 = new VirtualServer();
        VirtualServer group2 = new VirtualServer();
        assertThat(group1).isEqualTo(group2);
    }

    @Test
    public void areNotEqualsIfIdIsDifferent() {
        String oneId = "oneId";
        String otherId = "otherId";

        VirtualServer group1 = new VirtualServer(oneId);
        VirtualServer group2 = new VirtualServer(otherId);
        assertThat(group1).isNotEqualTo(group2);
    }

    private boolean createRule(String ruleId, final VirtualServer virtualserver) {
        Rule rule = mock(Rule.class);
        return virtualserver.addEntity(rule);
    }

    @Test
    public void addNewRule() {
        String ruleId = "newrule";
        VirtualServer virtualserver = new VirtualServer();
        boolean ruleCreated = createRule(ruleId, virtualserver);

        assertThat(ruleCreated).isTrue();
        assertThat(virtualserver.getNumEntities()).isEqualTo(1);
    }

    @Test
    public void addExistingRule() {
        String ruleId = "newrule";
        VirtualServer virtualserver = new VirtualServer();
        createRule(ruleId, virtualserver);
        boolean ruleCreated = createRule(ruleId, virtualserver);

        assertThat(ruleCreated).isFalse();
        assertThat(virtualserver.getNumEntities()).isEqualTo(1);
    }

    private boolean removeRule(String ruleId, final VirtualServer virtualserver) {
        Rule rule = mock(Rule.class);
        return virtualserver.removeEntity(rule);
    }

    @Test
    public void removeExistingRule() {
        String ruleId = "myrule";
        VirtualServer virtualserver = new VirtualServer();
        createRule(ruleId, virtualserver);
        boolean ruleRemoved = removeRule(ruleId, virtualserver);

        assertThat(ruleRemoved).isTrue();
        assertThat(virtualserver.getNumEntities()).isEqualTo(0);
    }

    @Test
    public void removeAbsentRule() {
        String ruleId = "myrule";
        VirtualServer virtualserver = new VirtualServer();
        boolean ruleRemoved = removeRule(ruleId, virtualserver);

        assertThat(ruleRemoved).isFalse();
        assertThat(virtualserver.getNumEntities()).isEqualTo(0);
    }
}
