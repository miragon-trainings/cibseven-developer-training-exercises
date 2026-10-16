package io.miragon.training.adapter.outbound.engine;

import org.cibseven.rest.client.api.ProcessDefinitionApi;
import org.cibseven.rest.client.invoker.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The adapter drives the engine through the generated typed client. We point that client's RestClient
 * at a {@link MockRestServiceServer} and assert the request it sends: the start-by-key path plus the
 * typed variable payload ({@code {value, type}}). No live engine — just an HTTP stub.
 */
class RemoteWelcomeKitProcessAdapterTest {

    @Test
    void startsTheProcessByKeyWithTheTypedNameVariable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ApiClient apiClient = new ApiClient(builder.build());
        apiClient.setBasePath("http://engine/engine-rest");
        RemoteWelcomeKitProcessAdapter adapter =
                new RemoteWelcomeKitProcessAdapter(new ProcessDefinitionApi(apiClient));

        server.expect(requestTo("http://engine/engine-rest/process-definition/key/sendWelcomeKit/start"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.variables.name.value").value("Jane"))
                .andExpect(jsonPath("$.variables.name.type").value("String"))
                .andRespond(withSuccess("{\"id\":\"pi-1\"}", MediaType.APPLICATION_JSON));

        adapter.startWelcomeKit("Jane");

        server.verify();
    }
}
