package io.github.dd2480group14.ciserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Record representing the extracted data from the push webhook event.
 * 
 * The PushEventInfo record contains information required to clone
 * the repository, identify commit and run tests.
 * 
 * @param author        Author of the push event
 * @param repoURL       URL of the repository
 * @param SHA           SHA identifying the commit
 * @param branch        Branch name where push occured
 * @param commitMessage Commit message of the push event
*/

record PushEventInfo (
    String author,
    String repoURL,
    String SHA,
    String branch,
    String commitMessage,
    String owner,
    String repoName ) {

        /**
         * Creates PushEventInfo from Github push payload.
         * 
         * @param jsonObject the JSON payload recieved from Github push event.
         * @return a PushEventInfo record containing extracted data.
         * @throws IllegalArgumentException if payload is not valid.
         */
        static PushEventInfo fromJSON(JSONObject jsonObject) throws IllegalArgumentException {
            try {
                JSONObject repo = jsonObject.getJSONObject("repository");

                // Repository URL
                String repoURL = repo.getString("clone_url");

                // Commit SHA
                String SHA = jsonObject.getString("after");

                // Branch name
                String ref = jsonObject.getString("ref");
                String branch = ref.replace("refs/heads/", "");

                JSONObject pusher = jsonObject.getJSONObject("pusher");
                String author = pusher.getString("name");

                // Repository owner
                JSONObject ownerObject = repo.optJSONObject("owner");
                String owner = ownerObject != null ?
                        ownerObject.optString("login", "Unknown") :
                        "Unknown";


                String repoName = repo.optString("name", "Unknown");

                String commitMessage = "No commit message";

                // Safe commit parsing
                JSONArray commits = jsonObject.optJSONArray("commits");

                if (commits != null && commits.length() > 0) {
                    JSONObject latestCommit = commits.getJSONObject(0);

                    commitMessage = latestCommit.optString("message", "No commit message");
                }
                
                return new PushEventInfo(
                        author,
                        repoURL,
                        SHA,
                        branch,
                        commitMessage,
                        owner,
                        repoName
                );
                
            } catch (JSONException e) {
                    throw new IllegalArgumentException("Invalid Github push payload", e);
            }
        }
    }

