package io.miragon.training.adapter.outbound.engine;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The deployment adapter POSTs the owned model to {@code /deployment/create} as a multipart upload.
 * We stub the engine with {@link MockRestServiceServer} and assert the request: the deployment endpoint
 * and a multipart body. This is the model deployment that makes the engine a generic host of our process.
 */
class EngineDeploymentAdapterTest {

    @Test
    void deploysTheOwnedModelToTheEngine() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://engine/engine-rest");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EngineDeploymentAdapter adapter = new EngineDeploymentAdapter(builder.build());

        server.expect(requestTo("http://engine/engine-rest/deployment/create"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andRespond(withSuccess("{\"id\":\"dep-1\"}", MediaType.APPLICATION_JSON));

        adapter.deployProcessModel();

        server.verify();
    }
}
