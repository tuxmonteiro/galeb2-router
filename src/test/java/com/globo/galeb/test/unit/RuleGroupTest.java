package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.rules.Rule;
import com.globo.galeb.rules.RuleGroup;

public class RuleGroupTest {

    @Test
    public void createWithDefaultContructor() {
        RuleGroup ruleGroup = new RuleGroup();
        assertThat(String.format("%s", ruleGroup)).isEqualTo("UNDEF");
    }

    @Test
    public void createWithContructorWithId() {
        String id = "NOT_UNDEF";
        RuleGroup ruleGroup = new RuleGroup(id);
        assertThat(String.format("%s", ruleGroup)).isEqualTo(id);
    }

    @Test
    public void createWithContructorWithJson() {
        String id = "NOT_UNDEF";
        JsonObject json = new JsonObject();
        json.putString(IJsonable.ID_FIELDNAME, id);
        RuleGroup ruleGroup = new RuleGroup(json);

        assertThat(String.format("%s", ruleGroup)).isEqualTo(id);
    }

    @Test
    public void areEqualsIfIdIsTheSame() {
        String sameId = "sameId";
        RuleGroup group1 = new RuleGroup(sameId);
        RuleGroup group2 = new RuleGroup(sameId);
        assertThat(group1).isEqualTo(group2);
    }

    @Test
    public void areEqualsIfIdIsNotDefined() {
        RuleGroup group1 = new RuleGroup();
        RuleGroup group2 = new RuleGroup();
        assertThat(group1).isEqualTo(group2);
    }

    @Test
    public void areNotEqualsIfIdIsDifferent() {
        String oneId = "oneId";
        String otherId = "otherId";

        RuleGroup group1 = new RuleGroup(oneId);
        RuleGroup group2 = new RuleGroup(otherId);
        assertThat(group1).isNotEqualTo(group2);
    }

    private boolean createRule(String ruleId, final RuleGroup ruleGroup) {
        Rule rule = mock(Rule.class);
        return ruleGroup.addEntity(rule);
    }

    @Test
    public void addNewRule() {
        String ruleId = "newrule";
        RuleGroup ruleGroup = new RuleGroup();
        boolean ruleCreated = createRule(ruleId, ruleGroup);

        assertThat(ruleCreated).isTrue();
        assertThat(ruleGroup.getNumEntities()).isEqualTo(1);
    }

    @Test
    public void addExistingRule() {
        String ruleId = "newrule";
        RuleGroup ruleGroup = new RuleGroup();
        createRule(ruleId, ruleGroup);
        boolean ruleCreated = createRule(ruleId, ruleGroup);

        assertThat(ruleCreated).isFalse();
        assertThat(ruleGroup.getNumEntities()).isEqualTo(1);
    }

    private boolean removeRule(String ruleId, final RuleGroup ruleGroup) {
        Rule rule = mock(Rule.class);
        return ruleGroup.removeEntity(rule);
    }

    @Test
    public void removeExistingRule() {
        String ruleId = "myrule";
        RuleGroup ruleGroup = new RuleGroup();
        createRule(ruleId, ruleGroup);
        boolean ruleRemoved = removeRule(ruleId, ruleGroup);

        assertThat(ruleRemoved).isTrue();
        assertThat(ruleGroup.getNumEntities()).isEqualTo(0);
    }

    @Test
    public void removeAbsentRule() {
        String ruleId = "myrule";
        RuleGroup ruleGroup = new RuleGroup();
        boolean ruleRemoved = removeRule(ruleId, ruleGroup);

        assertThat(ruleRemoved).isFalse();
        assertThat(ruleGroup.getNumEntities()).isEqualTo(0);
    }
}
