package com.mycompany.cleancheck.database.service;

/** A previous/current issue pair that moved or changed. */
final class IssueChange {

    IssueData previousIssue;
    IssueData currentIssue;

    IssueChange(
            IssueData previousIssue,
            IssueData currentIssue
    ) {

        this.previousIssue =
                previousIssue;

        this.currentIssue =
                currentIssue;
    }
}
