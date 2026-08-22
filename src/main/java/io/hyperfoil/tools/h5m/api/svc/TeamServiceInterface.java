package io.hyperfoil.tools.h5m.api.svc;

import io.hyperfoil.tools.h5m.api.Team;
import io.hyperfoil.tools.h5m.api.User;

import java.util.List;

public interface TeamServiceInterface {

    Team create(String name);

    void delete(long teamId);

    Team renameTeam(long teamId, String name);

    Team find(String name);

    List<Team> list();

    List<Team> listByUsername(String username);

    List<User> listMembers(long teamId);

    List<User> addMember(long teamId, long userId);

    List<User> removeMember(long teamId, long userId);
}
