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

public record PushEventInfo (
    String author,
    String repoURL,
    String SHA,
    String branch,
    String commitMessage ) {

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
                String repoURL = repo.getString("clone_url");

                String SHA = jsonObject.getString("after");

                String ref = jsonObject.getString("ref");
                String branch = ref.replace("refs/heads/", "");

                JSONObject pusher = jsonObject.getJSONObject("pusher");
                String author = pusher.getString("name");

                String commitMessage;
                
                try {
                    JSONArray commits= jsonObject.getJSONArray("commits");
                    JSONObject latestCommit = commits.getJSONObject(0);
                    commitMessage = latestCommit.getString("message");
                } catch (JSONException e) {
                    commitMessage = "N/A";
                }

                return new PushEventInfo(author, repoURL, SHA, branch, commitMessage);
            } catch (JSONException e) {
                throw new IllegalArgumentException("Invalid Github push payload", e);
            }
        }
    }
