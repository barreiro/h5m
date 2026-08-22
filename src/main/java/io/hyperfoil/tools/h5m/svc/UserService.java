package io.hyperfoil.tools.h5m.svc;

import io.hyperfoil.tools.h5m.api.Role;
import io.hyperfoil.tools.h5m.api.User;
import io.hyperfoil.tools.h5m.api.svc.UserServiceInterface;
import io.hyperfoil.tools.h5m.entity.UserEntity;
import io.hyperfoil.tools.h5m.entity.mapper.ApiMapper;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@ApplicationScoped
public class UserService implements UserServiceInterface {

    @Inject
    ApiMapper apiMapper;

    @Inject
    SecurityIdentity identity;

    @Override
    @Transactional
    public User resolveUser() {
        return apiMapper.toUser(resolveUserEntity());
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private UserEntity resolveUserEntity() {
        return switch (identity.getPrincipal()) {
            case JsonWebToken jwt -> UserEntity.find("sub = ?1 and iss = ?2", jwt.getSubject(), jwt.getIssuer()).firstResult();
            default -> UserEntity.find("username", identity.getPrincipal().getName()).firstResult();
        };
    }

    @Override
    @Transactional
    public long create(String username, Role role) {
        UserEntity user = new UserEntity(username, role);
        user.persist();
        return user.id;
    }

    @Override
    @Transactional
    public long create(String sub, String iss, String username, Role role) {
        UserEntity user = new UserEntity(sub, iss, username, role);
        user.persist();
        return user.id;
    }

    @Override
    @Transactional
    public User byUsername(String username) {
        UserEntity entity = UserEntity.find("username", username).firstResult();
        return apiMapper.toUser(entity);
    }

    @Transactional
    public User bySub(String sub, String iss) {
        UserEntity entity = UserEntity.find("sub = ?1 and iss = ?2", sub, iss).firstResult();
        return apiMapper.toUser(entity);
    }

    @Override
    @Transactional
    public List<User> list() {
        List<UserEntity> entities = UserEntity.listAll();
        return entities.stream().map(apiMapper::toUser).toList();
    }

    @Override
    @Transactional
    public boolean isMemberOf(long teamId) {
        UserEntity entity = resolveUserEntity();
        return entity != null && entity.teams.stream().anyMatch(team -> team.id.equals(teamId));
    }

    @Override
    @Transactional
    public void setRole(long userId, Role role) {
        UserEntity user = UserEntity.findById(userId);
        if (user != null) {
            user.role = role;
        }
    }

    @Override
    @Transactional
    public long count() {
        return UserEntity.count();
    }
}
