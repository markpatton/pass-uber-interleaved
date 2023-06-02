package org.eclipse.pass.notification.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
public class SubmissionEventMessage {

    /**
     * The submission event ID.
     */
    @JsonProperty("submission-event")
    private String submissionEventId;

    /**
     * The link for user approval.  Will only be present if event type is APPROVAL_REQUESTED_NEWUSER.
     */
    @JsonProperty("approval-link")
    private URI userApprovalLink;
}
