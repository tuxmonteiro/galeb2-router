package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.entity.impl.frontend.Virtualhost;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.metrics.ICounter;

public class VirtualhostTest {

    Virtualhost virtualserver = new Virtualhost();

    @Test
    public void createWithDefaultContructor() {
        assertThat(String.format("%s", virtualserver)).isEqualTo(IJsonable.UNDEF);
    }

    @Test
    public void createWithContructorWithId() {
        String id = "NOT_UNDEF";
        Virtualhost virtualserver2 = new Virtualhost(id);
        assertThat(String.format("%s", virtualserver2)).isEqualTo(id);
    }

    @Test
    public void createWithContructorWithJson() {
        String id = "NOT_UNDEF";
        JsonObject json = new JsonObject();
        json.putString(IJsonable.ID_FIELDNAME, id);
        Virtualhost virtualserver2 = new Virtualhost(json);

        assertThat(String.format("%s", virtualserver2)).isEqualTo(id);
    }

    @Test
    public void areEqualsIfIdIsTheSame() {
        String sameId = "sameId";
        Virtualhost virtualserver1 = new Virtualhost(sameId);
        Virtualhost virtualserver2 = new Virtualhost(sameId);
        assertThat(virtualserver1).isEqualTo(virtualserver2);
    }

    @Test
    public void areEqualsIfIdIsNotDefined() {
        Virtualhost virtualserver1 = new Virtualhost();
        Virtualhost virtualserver2 = new Virtualhost();
        assertThat(virtualserver1).isEqualTo(virtualserver2);
    }

    @Test
    public void areNotEqualsIfIdIsDifferent() {
        String oneId = "oneId";
        String otherId = "otherId";

        Virtualhost virtualserver1 = new Virtualhost(oneId);
        Virtualhost virtualserver2 = new Virtualhost(otherId);
        assertThat(virtualserver1).isNotEqualTo(virtualserver2);
    }

    private boolean createRule(String ruleId, final Virtualhost virtualserver) {
        Rule rule = mock(Rule.class);

        when(rule.setLogger((SafeLogger) anyObject())).thenReturn(rule);
        when(rule.setCounter((ICounter) anyObject())).thenReturn(rule);
        when(rule.setQueueService((IQueueService) anyObject())).thenReturn(rule);
        when(rule.setFarm((Farm) anyObject())).thenReturn(rule);
        when(rule.setPlataform(anyObject())).thenReturn(rule);
        when(rule.setStaticConf((JsonObject) anyObject())).thenReturn(rule);

        return virtualserver.addEntity(rule);
    }

    @Test
    public void addNewRule() {
        String ruleId = "newrule";
        boolean ruleCreated = createRule(ruleId, virtualserver);

        assertThat(ruleCreated).isTrue();
        assertThat(virtualserver.getNumEntities()).isEqualTo(1);
    }

    @Test
    public void addExistingRule() {
        String ruleId = "newrule";
        createRule(ruleId, virtualserver);
        boolean ruleCreated = createRule(ruleId, virtualserver);

        assertThat(ruleCreated).isFalse();
        assertThat(virtualserver.getNumEntities()).isEqualTo(1);
    }

    private boolean removeRule(String ruleId, final Virtualhost virtualserver) {
        Rule rule = mock(Rule.class);

        when(rule.setLogger((SafeLogger) anyObject())).thenReturn(rule);
        when(rule.setCounter((ICounter) anyObject())).thenReturn(rule);
        when(rule.setQueueService((IQueueService) anyObject())).thenReturn(rule);
        when(rule.setFarm((Farm) anyObject())).thenReturn(rule);
        when(rule.setPlataform(anyObject())).thenReturn(rule);
        when(rule.setStaticConf((JsonObject) anyObject())).thenReturn(rule);

        return virtualserver.removeEntity(rule);
    }

    @Test
    public void removeExistingRule() {
        String ruleId = "myrule";
        createRule(ruleId, virtualserver);
        boolean ruleRemoved = removeRule(ruleId, virtualserver);

        assertThat(ruleRemoved).isTrue();
        assertThat(virtualserver.getNumEntities()).isEqualTo(0);
    }

    @Test
    public void removeAbsentRule() {
        String ruleId = "myrule";
        boolean ruleRemoved = removeRule(ruleId, virtualserver);

        assertThat(ruleRemoved).isFalse();
        assertThat(virtualserver.getNumEntities()).isEqualTo(0);
    }
}
