package io.hyperfoil.tools.h5m.rest;

import io.hyperfoil.tools.h5m.api.Team;
import io.hyperfoil.tools.h5m.api.User;
import io.hyperfoil.tools.h5m.api.svc.TeamServiceInterface;
import io.hyperfoil.tools.h5m.api.svc.UserServiceInterface;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

import static io.hyperfoil.tools.h5m.api.Role.ADMIN_ROLE;

@Path("/team")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Team", description = "Manage teams and team membership")
public class TeamResource {

    @Inject
    TeamServiceInterface teamService;

    @Inject
    SecurityIdentity identity;

    @Inject
    UserServiceInterface userService;

    @GET
    @PermitAll
    @Operation(description = "Retrieve the list of all teams")
    public List<Team> listTeams() {
        return teamService.list();
    }

    @POST
    @RolesAllowed(ADMIN_ROLE)
    @Operation(description = "Create a new team")
    public Team createTeam(@Valid Team team) {
        return teamService.create(team.name());
    }

    @PUT
    @Path("{id}")
    @RolesAllowed(ADMIN_ROLE)
    @Operation(description = "Rename a team")
    public Team renameTeam(@PathParam("id") long id, @Valid Team team) {
        return teamService.renameTeam(id, team.name());
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed(ADMIN_ROLE)
    @Operation(description = "Delete a team by its ID")
    public void deleteTeam(@PathParam("id") long id) {
        teamService.delete(id);
    }

    @GET
    @Path("{id}/members")
    @Authenticated
    @Operation(description = "List the members of a team")
    public List<User> listMembers(@PathParam("id") long id) {
        if (!identity.getRoles().contains(ADMIN_ROLE) && !userService.isMemberOf(id)) {
            throw new ForbiddenException("You are not allowed to perform this action");
        }
        return teamService.listMembers(id);
    }

    @PUT
    @Path("{id}/members/{userId}")
    @Authenticated
    @Operation(description = "Add a user to a team")
    public List<User> addMember(@PathParam("id") long id, @PathParam("userId") long userId) {
        if (!identity.getRoles().contains(ADMIN_ROLE) && !userService.isMemberOf(id)) {
            throw new ForbiddenException("You are not allowed to perform this action");
        }
        return teamService.addMember(id, userId);
    }

    @DELETE
    @Path("{id}/members/{userId}")
    @Authenticated
    @Operation(description = "Remove a user from a team")
    public List<User> removeMember(@PathParam("id") long id, @PathParam("userId") long userId) {
        if (!identity.getRoles().contains(ADMIN_ROLE) && !userService.isMemberOf(id)) {
            throw new ForbiddenException("You are not allowed to perform this action");
        }
        return teamService.removeMember(id, userId);
    }
}
