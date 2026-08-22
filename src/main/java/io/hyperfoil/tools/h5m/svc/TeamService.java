package io.hyperfoil.tools.h5m.svc;

import io.hyperfoil.tools.h5m.api.Team;
import io.hyperfoil.tools.h5m.api.User;
import io.hyperfoil.tools.h5m.api.svc.TeamServiceInterface;
import io.hyperfoil.tools.h5m.entity.TeamEntity;
import io.hyperfoil.tools.h5m.entity.UserEntity;
import io.hyperfoil.tools.h5m.entity.mapper.ApiMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@ApplicationScoped
public class TeamService implements TeamServiceInterface {

    @Inject
    ApiMapper apiMapper;

    @Override
    @Transactional
    public Team create(String name) {
        TeamEntity team = new TeamEntity(name);
        team.persist();
        return apiMapper.toTeam(team);
    }

    @Override
    @Transactional
    public void delete(long teamId) {
        TeamEntity.deleteById(teamId);
    }

    @Override
    @Transactional
    public Team renameTeam(long teamId, String name) {
        TeamEntity team = TeamEntity.findById(teamId);
        if (team == null) {
            throw new NotFoundException("Team not found" + teamId);
        }
        team.name = name;
        return apiMapper.toTeam(team);
    }

    @Override
    @Transactional
    public Team find(String name) {
        TeamEntity entity = TeamEntity.find("name", name).firstResult();
        return entity != null ? apiMapper.toTeam(entity) : null;
    }

    @Override
    @Transactional
    public List<Team> list() {
        List<TeamEntity> entities = TeamEntity.listAll();
        return entities.stream().map(apiMapper::toTeam).toList();
    }

    @Override
    @Transactional
    public List<Team> listByUsername(String username) {
        UserEntity user = UserEntity.find("username", username).firstResult();
        return user == null ? List.of() : user.teams.stream().map(apiMapper::toTeam).toList();
    }

    @Override
    @Transactional
    public List<User> listMembers(long teamId) {
        TeamEntity team = TeamEntity.findById(teamId);
        return team == null ? List.of() : team.members.stream().map(apiMapper::toUser).toList();
    }

    @Override
    @Transactional
    public List<User> addMember(long teamId, long userId) {
        return modifyMembers(teamId, userId, Set::add);
    }

    @Override
    @Transactional
    public List<User> removeMember(long teamId, long userId) {
        return modifyMembers(teamId, userId, Set::remove);
    }

    private List<User> modifyMembers(long teamId, long userId, BiConsumer<Set<UserEntity>, UserEntity> action) {
        TeamEntity team = TeamEntity.findById(teamId);
        if (team == null) {
            throw new NotFoundException("Team not found" + teamId);
        }
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new NotFoundException("User not found" + userId);
        }
        action.accept(team.members, user);
        return team.members.stream().map(apiMapper::toUser).toList();
    }
}
