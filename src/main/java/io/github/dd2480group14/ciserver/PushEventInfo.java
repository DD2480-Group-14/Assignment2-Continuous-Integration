package io.github.dd2480group14.ciserver;

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
    String commitMessage ) {}
