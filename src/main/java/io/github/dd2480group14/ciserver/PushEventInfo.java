package io.github.dd2480group14.ciserver;

/**
 * Class representing the extracted data from the push webhook event.
 * 
 * The PushEventInfo object contains information required to clone
 * the repository, identify commit and run tests.
*/

public class PushEventInfo {
    
    private final String author;
    private final String repoURL;
    private final String SHA;
    private final String branch;
    private final String commitMessage;

    /**
     * Creates a PushEventInfo object containing data extracted
     * from a Github push webhook payload.
     * 
     * @param repoURL
     * @param SHA
     * @param branch
     * @param commitMessage
     */
    public PushEventInfo(
        String author,
        String repoURL,
        String SHA,
        String branch,
        String commitMessage) {
            this.author = author;
            this.repoURL = repoURL;
            this.SHA = SHA;
            this.branch = branch;
            this.commitMessage = commitMessage;
        }

    /**
     * @return URL for the repository.
     */
    public String getRepoURL(){
        return repoURL;
    }

    /**
     * @return SHA to identify commit.
     */
    public String getSHA(){
        return SHA;
    }

    /**
     * @return Branch name where the push occured.
     */
    public String getBranch(){
        return branch;
    }

    /**
     * @return Author of the push event.
     */
    public String getAuthor(){
        return author;
    }


    /**
     * @return Commit message of the push event.
     */
    public String getCommitMessage(){
        return commitMessage;
    }
}
