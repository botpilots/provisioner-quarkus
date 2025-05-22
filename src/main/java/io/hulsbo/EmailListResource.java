package io.hulsbo;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.hulsbo.entities.EmailEntity;
import jakarta.transaction.Transactional; // For database transactions

// Import Logger
import org.jboss.logging.Logger;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Map; // For creating JSON error objects

@Path("/opt")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmailListResource {

    private static final Logger LOG = Logger.getLogger(EmailListResource.class);

    // Helper record for JSON error responses
    public record ErrorResponse(String message) {}

    @POST
    @Path("/in")
    @Transactional
    public Response optInEmail(@QueryParam("email") @jakarta.validation.constraints.Email String emailString) {
        LOG.infof("Attempting to opt-in email: %s", emailString);
        if (emailString == null || emailString.trim().isEmpty()) {
            LOG.warn("Opt-in attempt with empty email string.");
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(new ErrorResponse("Email parameter is required."))
                           .build();
        }

        Optional<EmailEntity> existingEmailOpt = EmailEntity.find("email", emailString).firstResultOptional();

        EmailEntity emailEntity;
        if (existingEmailOpt.isPresent()) {
            emailEntity = existingEmailOpt.get();
            if (emailEntity.optOut) {
                LOG.infof("Re-opting in email: %s", emailString);
                emailEntity.optOut = false;
                emailEntity.optOutDate = null; // Clear opt-out date
                emailEntity.optOutReason = null; // Clear opt-out reason
                // Panache automatically persists changes
            } else {
                LOG.infof("Email %s is already opted-in.", emailString);
            }
        } else {
            LOG.infof("Opting in new email: %s", emailString);
            emailEntity = new EmailEntity(emailString);
            emailEntity.persist(); // Necessary for new entities
        }

        return Response.ok(emailEntity).build();
    }

    @POST
    @Path("/out")
    @Transactional
    public Response optOutEmail(@QueryParam("email") @jakarta.validation.constraints.Email String emailString,
                                @QueryParam("reason") String reason) {
        LOG.infof("Attempting to opt-out email: %s with reason: %s", emailString, reason);
        if (emailString == null || emailString.trim().isEmpty()) {
            LOG.warn("Opt-out attempt with empty email string.");
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(new ErrorResponse("Email parameter is required."))
                           .build();
        }

        Optional<EmailEntity> existingEmailOpt = EmailEntity.find("email", emailString).firstResultOptional();

        if (existingEmailOpt.isEmpty()) {
            LOG.warnf("Email %s not found for opt-out.", emailString);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Email not found, cannot opt out."))
                           .build();
        }

        EmailEntity emailEntity = existingEmailOpt.get();

        if (emailEntity.optOut) {
            LOG.infof("Email %s is already opted-out.", emailString);
            if (reason != null && !reason.equalsIgnoreCase(emailEntity.optOutReason)) {
                LOG.infof("Updating opt-out reason for %s to: %s", emailString, reason);
                emailEntity.optOutReason = reason;
            }
        } else {
            LOG.infof("Opting out email: %s", emailString);
            emailEntity.optOut = true;
            emailEntity.optOutDate = LocalDateTime.now();
            emailEntity.optOutReason = reason;
        }
        // Panache automatically persists changes for managed entities.

        return Response.ok(emailEntity).build();
    }
} 