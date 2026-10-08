package com.zombiedetector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end checks of the multi-tenant rules against the real running app (real security
 * filter chain, real H2). Uses only the JDK HTTP client so there is no dependency on which
 * MockMvc/WebTestClient test helpers a given Spring Boot version ships.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.email=test-admin@example.com",
                "app.admin.password=TestAdminPass123!",
                "app.auth.rate-limit.max-attempts=100000"
        })
class SecurityIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private record Resp(int status, String body) {}
    private record Account(String token, String email) {}

    private Resp send(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (json != null) {
            b.header("Content-Type", "application/json");
            b.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body());
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        assertTrue(m.find(), "no field '" + name + "' in: " + json);
        return m.group(1);
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private Account register() throws Exception {
        String email = "user" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Resp r = send("POST", "/api/auth/register", null, credentials(email, "password123"));
        assertEquals(200, r.status(), r.body());
        return new Account(field(r.body(), "token"), email);
    }

    private String adminToken() throws Exception {
        Resp r = send("POST", "/api/auth/login", null, credentials("test-admin@example.com", "TestAdminPass123!"));
        assertEquals(200, r.status(), r.body());
        assertEquals("ADMIN", field(r.body(), "role"));
        return field(r.body(), "token");
    }

    private String addNode(String token) throws Exception {
        Resp r = send("POST", "/api/nodes", token, "{\"instanceType\":\"t2.micro\",\"hourlyRate\":0.0116,\"environment\":\"DEV\"}");
        assertEquals(200, r.status(), r.body());
        return field(r.body(), "nodeId");
    }

    private static final String VALID_POLICY =
            "{\"cpuThreshold\":15.0,\"idleWindowMinutes\":2,\"minSamplesRequired\":5,"
                    + "\"recoveryStrikesRequired\":3,\"gracePeriodSeconds\":20}";

    // ---- authentication ----------------------------------------------------------------------

    @Test
    void healthIsPublicButEverythingElseNeedsAToken() throws Exception {
        assertEquals(200, send("GET", "/api/health", null, null).status());
        assertEquals(401, send("GET", "/api/nodes", null, null).status());
        assertEquals(401, send("GET", "/api/policy", null, null).status());
        assertEquals(401, send("GET", "/api/audit", null, null).status());
        assertEquals(401, send("GET", "/api/some-route-nobody-defined", null, null).status());
        assertEquals(401, send("GET", "/api/nodes", "not-a-real-token", null).status());
    }

    @Test
    void registerLoginAndMeWorkAndEmailsAreCaseInsensitive() throws Exception {
        String local = "MiXed" + UUID.randomUUID().toString().substring(0, 6);
        String mixedCase = local + "@Example.com";

        Resp registered = send("POST", "/api/auth/register", null, credentials(mixedCase, "password123"));
        assertEquals(200, registered.status(), registered.body());
        assertEquals("USER", field(registered.body(), "role"));
        assertEquals(mixedCase.toLowerCase(), field(registered.body(), "email"));

        assertEquals(409, send("POST", "/api/auth/register", null,
                credentials(mixedCase.toLowerCase(), "password123")).status(), "same email, different case");

        Resp login = send("POST", "/api/auth/login", null, credentials(mixedCase.toUpperCase(), "password123"));
        assertEquals(200, login.status(), login.body());

        Resp me = send("GET", "/api/auth/me", field(login.body(), "token"), null);
        assertEquals(200, me.status());
        assertEquals(mixedCase.toLowerCase(), field(me.body(), "email"));
    }

    @Test
    void badCredentialsAndBadInputAreRejected() throws Exception {
        Account a = register();
        assertEquals(401, send("POST", "/api/auth/login", null, credentials(a.email(), "wrong-password")).status());
        assertEquals(401, send("POST", "/api/auth/login", null, credentials("nobody@example.com", "password123")).status());
        assertEquals(400, send("POST", "/api/auth/register", null, credentials("not-an-email", "password123")).status());
        assertEquals(400, send("POST", "/api/auth/register", null, credentials("short@example.com", "short")).status());
    }

    // ---- roles ----------------------------------------------------------------------------------

    @Test
    void onlyAdminsCanChangePolicyAndInvalidValuesAreRejected() throws Exception {
        Account user = register();
        String admin = adminToken();

        assertEquals(403, send("PUT", "/api/policy", user.token(), VALID_POLICY).status());
        assertEquals(200, send("GET", "/api/policy", user.token(), null).status(), "anyone signed in can read it");

        Resp ok = send("PUT", "/api/policy", admin, VALID_POLICY);
        assertEquals(200, ok.status(), ok.body());
        assertEquals("test-admin@example.com", field(ok.body(), "updatedBy"));

        String cpuTooHigh = VALID_POLICY.replace("15.0", "150.0");
        assertEquals(400, send("PUT", "/api/policy", admin, cpuTooHigh).status());

        String unreachableSamples = VALID_POLICY.replace("\"idleWindowMinutes\":2", "\"idleWindowMinutes\":1")
                .replace("\"minSamplesRequired\":5", "\"minSamplesRequired\":20");
        assertEquals(400, send("PUT", "/api/policy", admin, unreachableSamples).status());
    }

    @Test
    void onlyAdminsCanUseTheSchedulerKillSwitch() throws Exception {
        Account user = register();
        assertEquals(403, send("POST", "/api/scheduler/pause", user.token(), null).status());
        assertEquals(403, send("POST", "/api/scheduler/resume", user.token(), null).status());
        assertEquals(200, send("GET", "/api/scheduler/status", user.token(), null).status());
    }

    // ---- tenant isolation -----------------------------------------------------------------------

    @Test
    void usersOnlySeeAndManageTheirOwnNodes() throws Exception {
        Account alice = register();
        Account bob = register();
        String admin = adminToken();

        String aliceNode = addNode(alice.token());

        assertTrue(send("GET", "/api/nodes", alice.token(), null).body().contains(aliceNode));
        assertFalse(send("GET", "/api/nodes", bob.token(), null).body().contains(aliceNode),
                "Bob must not see Alice's node");
        assertTrue(send("GET", "/api/nodes", admin, null).body().contains(aliceNode), "admin sees everything");

        assertEquals(403, send("POST", "/api/nodes/" + aliceNode + "/override", bob.token(), null).status());
        assertEquals(403, send("DELETE", "/api/nodes/" + aliceNode, bob.token(), null).status());
        assertEquals(404, send("POST", "/api/nodes/does-not-exist/override", alice.token(), null).status());

        assertEquals(200, send("POST", "/api/nodes/" + aliceNode + "/override", alice.token(), null).status());

        assertEquals(200, send("DELETE", "/api/nodes/" + aliceNode, alice.token(), null).status());
        assertFalse(send("GET", "/api/nodes", alice.token(), null).body().contains(aliceNode));
        assertEquals(404, send("DELETE", "/api/nodes/" + aliceNode, alice.token(), null).status());
    }

    @Test
    void adminCanRemoveAnyUsersNode() throws Exception {
        Account alice = register();
        String node = addNode(alice.token());
        assertEquals(200, send("DELETE", "/api/nodes/" + node, adminToken(), null).status());
    }

    @Test
    void savingsAndAuditAreScopedToo() throws Exception {
        Account alice = register();
        Account bob = register();
        String aliceNode = addNode(alice.token());

        assertTrue(send("GET", "/api/audit", alice.token(), null).body().contains(aliceNode));
        assertFalse(send("GET", "/api/audit", bob.token(), null).body().contains(aliceNode));

        Resp history = send("GET", "/api/savings/history", bob.token(), null);
        assertEquals(200, history.status());
        assertEquals(7, history.body().split("\"date\"", -1).length - 1, "one row per day for 7 days");
    }

    // ---- input validation -------------------------------------------------------------------------

    @Test
    void nodeCreationValidatesItsInput() throws Exception {
        Account user = register();
        assertEquals(400, send("POST", "/api/nodes", user.token(),
                "{\"instanceType\":\"\",\"hourlyRate\":0.1,\"environment\":\"DEV\"}").status());
        assertEquals(400, send("POST", "/api/nodes", user.token(),
                "{\"instanceType\":\"t2.micro\",\"hourlyRate\":-1,\"environment\":\"DEV\"}").status());
        assertEquals(400, send("POST", "/api/nodes", user.token(),
                "{\"instanceType\":\"t2.micro\",\"hourlyRate\":1000,\"environment\":\"DEV\"}").status());
        assertEquals(400, send("POST", "/api/nodes", user.token(),
                "{\"instanceType\":\"t2.micro\",\"hourlyRate\":0.1,\"environment\":\"MOON\"}").status());
    }
}
