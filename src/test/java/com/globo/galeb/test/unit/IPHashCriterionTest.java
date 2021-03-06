/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static com.globo.galeb.consistenthash.HashAlgorithm.HashType;
import static org.mockito.Mockito.*;

import java.util.EnumSet;
import java.util.Set;

import com.globo.galeb.consistenthash.HashAlgorithm;
import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.criteria.impl.IPHashCriterion;
import com.globo.galeb.criteria.impl.LoadBalanceCriterion;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.backend.Backend;
import com.globo.galeb.entity.impl.backend.BackendPool;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.request.RequestData;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;

/**
 * Class IPHashCriterionTest.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 22, 2014.
 */
public class IPHashCriterionTest {

    /** The backend pool. */
    BackendPool backendPool;

    /** The num backends. */
    int numBackends = 10;

    @Before
    public void setUp() throws Exception {
        Vertx vertx = mock(DefaultVertx.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(vertx.createHttpClient()).thenReturn(httpClient);

        JsonObject backendPoolProperties = new JsonObject()
            .putString(LoadBalanceCriterion.LOADBALANCE_POLICY_FIELDNAME,
                    IPHashCriterion.class.getSimpleName().replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, ""));
        JsonObject backendPoolJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, "test.localdomain")
            .putObject(IJsonable.PROPERTIES_FIELDNAME, backendPoolProperties);
        backendPool = (BackendPool) new BackendPool(backendPoolJson).setPlataform(vertx);

        for (int x=0; x<numBackends; x++) {
            backendPool.addEntity(new Backend(new JsonObject().putString(IJsonable.ID_FIELDNAME, String.format("0:%s", x))));
        }
    }

    @Test
    public void checkPersistentChoice() {
        long numTests = 256L*256L;

        for (Integer counter=0; counter<numTests; counter++) {

            RequestData requestData1 = new RequestData().setRemoteAddress(counter.toString());
            IBackend backend1 = backendPool.getChoice(requestData1);
            RequestData requestData2 = new RequestData().setRemoteAddress(counter.toString());
            IBackend backend2 = backendPool.getChoice(requestData2);
            RequestData requestData3 = new RequestData().setRemoteAddress(counter.toString());
            IBackend backend3 = backendPool.getChoice(requestData3);

            assertThat(backend1).as("backend1 persistent choice with backend2").isEqualTo(backend2);
            assertThat(backend1).as("backend1 persistent choice with backend3").isEqualTo(backend3);
        }
    }

    @Test
    public void checkUniformDistribution() {
        long samples = 10000L;
        int rounds = 5;
        double percentMarginOfError = 0.5;
        Set<HashType> hashs = EnumSet.allOf(HashAlgorithm.HashType.class);

        for (int round=0; round < rounds; round++) {
            System.out.println(String.format("TestHashPolicy.checkUniformDistribution - round %s: %d samples", round+1, samples));

            for (HashType hash: hashs) {

                backendPool.getProperties().mergeIn(new JsonObject().putString(IPHashCriterion.HASH_ALGORITHM_FIELDNAME, hash.toString()));
                backendPool.resetLoadBalance();

                long sum = 0L;
                long initialTime = System.currentTimeMillis();
                for (Integer counter=0; counter<samples; counter++) {
                    RequestData requestData = new RequestData().setRemoteAddress(counter.toString());
                    sum += backendPool.getChoice(requestData).getPort();
                }
                long finishTime = System.currentTimeMillis();

                double result = (numBackends*(numBackends-1)/2.0) * (samples/numBackends);

                System.out.println(String.format("-> TestHashPolicy.checkUniformDistribution (%s): Time spent (ms): %d. NonUniformDistRatio (smaller is better): %.4f%%",
                        hash, finishTime-initialTime, Math.abs(100.0*(result-sum)/result)));

                double topLimit = sum*(1.0+percentMarginOfError);
                double bottomLimit = sum*(1.0-percentMarginOfError);

                assertThat(result).isGreaterThanOrEqualTo(bottomLimit)
                                  .isLessThanOrEqualTo(topLimit);
            }
        }
    }
}
