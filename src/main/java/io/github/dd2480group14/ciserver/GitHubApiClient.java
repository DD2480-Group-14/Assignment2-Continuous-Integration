package io.github.dd2480group14.ciserver;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class GitHubApiClient {
    private final OkHttpClient client;
    private final String token;
    private static final String GITHUB_API_BASE = "https://api.github.com";

    /**
     * Creates a new GitHub API client.
     */
    GitHubApiClient(String token) {
        this.client = new OkHttpClient();
        this.token = token;
    }

    /**
     * Updates the commit status on GitHub.
     */
    boolean updateCommitStatus(String repoURL, String sha, String state, 
                                     String description, String targetUrl) {
        try {
            // Extract owner or repo from clone URL
            String ownerRepo = extractOwnerRepo(repoURL);
            
            // Build API URL
            String url = String.format("%s/repos/%s/statuses/%s", 
                                      GITHUB_API_BASE, ownerRepo, sha);
            
            // Build payload
            JSONObject payload = new JSONObject();
            payload.put("state", state);
            payload.put("description", description);
            payload.put("context", "ci/dd2480-group14"); // Identifier for your CI
            
            if (targetUrl != null && !targetUrl.isEmpty()) {
                payload.put("target_url", targetUrl);
            }
            
            // Create request
            RequestBody body = RequestBody.create(
                payload.toString(), 
                MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .post(body)
                .build();
            
            // Execute request
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println(" Successfully updated commit status: " + state);
                    return true;
                } else {
                    System.err.println(" Failed to update commit status: " + response.code());
                    System.err.println("Response: " + response.body().string());
                    return false;
                }
            }
            
        } catch (Exception e) {
            System.err.println(" Error updating commit status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts owner/repo from GitHub clone URL.
     */
    private String extractOwnerRepo(String cloneURL) {
        // Remove .git suffix
        String url = cloneURL.replace(".git", "");
        
        // Extract from https://github.com/owner/repo
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        
        throw new IllegalArgumentException("Invalid GitHub URL: " + cloneURL);
    }
}