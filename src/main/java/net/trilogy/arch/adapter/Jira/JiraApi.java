package net.trilogy.arch.adapter.Jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.trilogy.arch.domain.architectureUpdate.Jira;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JiraApi {
    private final HttpClient client;
    private final String baseUri;
    private final String getStoryEndpoint;
    private final String bulkCreateEndpoint;

    public JiraApi(HttpClient client, String baseUri, String getStoryEndpoint, String bulkCreateEndpoint) {
        this.client = client;
        this.baseUri = baseUri.replaceAll("/$", "") + "/";
        this.bulkCreateEndpoint = bulkCreateEndpoint.replaceAll("(^/|/$)", "") + "/";
        this.getStoryEndpoint = getStoryEndpoint.replaceAll("(^/|/$)", "") + "/";
    }

    public JiraCreateStoriesResult createStories(List<JiraStory> jiraStories, String epicKey, String projectId, String projectKey, String username, char[] password) throws JiraApiException {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(generateBodyForCreateStories(epicKey, jiraStories, projectId)))
                .header("Authorization", "Basic " + getEncodeAuth(username, password))
                .header("Content-Type", "application/json")
                .uri(URI.create(baseUri + bulkCreateEndpoint))
                .build();
        try {
            final HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw JiraApiException.builder()
                        .message("Failed to log into Jira. Please check your credentials.")
                        .response(response)
                        .build();
            }

        } catch (JiraApiException e) {
            throw e;
        } catch (Throwable e) {
            throw JiraApiException.builder()
                    .message("Unknown exception occurred.")
                    .build();
        }
        return new JiraCreateStoriesResult();
    }

    public JiraQueryResult getStory(Jira jira, String username, char[] password) throws JiraApiException {
        String encodedAuth = getEncodeAuth(username, password);
        final String ticket = jira.getTicket();
        final HttpRequest request = createGetStoryRequest(encodedAuth, ticket);

        HttpResponse<String> response = null;
        try {
            response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 401) {
                throw JiraApiException.builder().message("Failed to log into Jira. Please check your credentials.").response(response).build();
            }
            return new JiraQueryResult(response);
        } catch(JiraApiException e) {
            throw e;
        } catch (Throwable e) {
            throw JiraApiException.builder()
                    .cause(e)
                    .response(response)
                    .message("Unknown error occurred")
                    .build();
        }
    }

    private String generateBodyForCreateStories(String epicKey, List<JiraStory> jiraStories, String projectId) {
        return new JSONObject(
                Map.of("issueUpdates", new JSONArray(
                        jiraStories.stream().map(story -> new JSONObject(Map.of(
                                "fields", Map.of(
                                        "customfield_10002", epicKey,
                                        "project", Map.of("id", projectId),
                                        "summary", story.getTitle(),
                                        "issuetype", Map.of("name", "Feature Story"),
                                        "description", makeDescription(story)
                                )))).collect(Collectors.toList())
                ))
        ).toString();
    }

    private String makeDescription(JiraStory story) {
        return "" +
                makeFunctionalRequirementTable(story) +
                makeTddTablesByComponent(story);
    }

    private String makeTddTablesByComponent(JiraStory story) {
        Map<String, List<JiraStory.JiraTdd>> compMap = story.getTdds()
                .stream()
                .collect(
                        Collectors.groupingBy(JiraStory.JiraTdd::getComponent)
                );

        return "h3. Technical Design:\n" +
                compMap.entrySet().stream().map(
                        entry -> "h4. Component: " + entry.getKey() + "\n||TDD||Description||\n" +
                                entry.getValue().stream().map(
                                        tdd -> "| " + tdd.getId() + " | {noformat}" + tdd.getText() + "{noformat} |\n"
                                ).collect(Collectors.joining())
                ).collect(Collectors.joining()) +
                "";
    }

    private String makeFunctionalRequirementTable(JiraStory story) {
        return "h3. Implements functionality:\n" +
                "||Id||Source||Description||\n" +
                story.getFunctionalRequirements()
                        .stream()
                        .map(this::makeFunctionalRequiremntRow)
                        .collect(Collectors.joining());
    }

    private String makeFunctionalRequiremntRow(JiraStory.JiraFunctionalRequirement funcReq) {
        return ""
                + "| " + funcReq.getId() + " | "
                + funcReq.getSource()
                + " | {noformat}" + funcReq.getText() + "{noformat} |\n"
                + "";

    }

    private String getEncodeAuth(String username, char[] password) {
        final String s = username + ":" + String.valueOf(password);
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    private HttpRequest createGetStoryRequest(String encodedAuth, String ticket) {
        return HttpRequest
                .newBuilder()
                .GET()
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + encodedAuth)
                .uri(URI.create(baseUri + getStoryEndpoint + ticket))
                .build();
    }

    @VisibleForTesting
    HttpClient getHttpClient() {
        return client;
    }

    @VisibleForTesting
    String getBaseUri() {
        return baseUri;
    }

    @VisibleForTesting
    String getGetStoryEndpoint() {
        return getStoryEndpoint;
    }

    @VisibleForTesting
    String getBulkCreateEndpoint() {
        return bulkCreateEndpoint;
    }

    @Getter
    public static class JiraApiException extends Exception {
        @NonNull private final String message;
        private final Throwable cause;
        private final HttpResponse<?> response;

        @Builder
        private JiraApiException(@NonNull String message, Throwable cause, HttpResponse<?> response) {
            this.message = message;
            this.cause = cause;
            this.response = response;
        }
    }
}
