package io.hyperfoil.tools.h5m.rest;

import io.hyperfoil.tools.h5m.api.Team;
import io.hyperfoil.tools.h5m.api.User;
import io.hyperfoil.tools.h5m.api.svc.TeamServiceInterface;
import io.hyperfoil.tools.h5m.api.svc.UserServiceInterface;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "User", description = "User and role information")
public class UserResource {

    @Inject
    UserServiceInterface userService;

    @Inject
    TeamServiceInterface teamService;

    @Inject
    SecurityIdentity identity;

    @GET
    @Operation(description = "List all users")
    public List<User> listUsers() {
        return identity.isAnonymous() ? List.of() : userService.list();
    }

    @GET
    @Path("me")
    @Authenticated
    @Operation(description = "Get the authenticated user")
    public User currentUser() {
        User user = userService.resolveUser();
        if (user == null) {
            throw new InternalServerErrorException("The authenticated user is not present in the database");
        }
        return user;
    }

    @GET
    @Path("teams")
    @Authenticated
    @Operation(description = "Get the teams of the authenticated user")
    public List<Team> userTeams() {
        User user = userService.resolveUser();
        if (user == null) {
            throw new InternalServerErrorException("The authenticated user is not present in the database");
        }
        return teamService.listByUsername(user.username());
    }

}
