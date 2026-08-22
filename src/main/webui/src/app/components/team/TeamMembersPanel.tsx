import type { Team } from '@client/types.gen.ts';

import { useNotification } from '@app/context/useNotification.tsx';
import { useRoles } from '@app/context/useRoles.tsx';
import { useTeams } from '@app/context/useTeams.tsx';
import { Add, Subtract } from '@carbon/icons-react';
import { ContainedList, ContainedListItem, ExpandableSearch, IconButton, InlineNotification, Modal, Stack } from '@carbon/react';
import {
  addMemberMutation,
  listMembersOptions,
  listTeamsOptions,
  listUsersOptions,
  removeMemberMutation,
  userTeamsOptions,
} from '@client/@tanstack/react-query.gen.ts';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useState } from 'react';

export const TeamMembersPanel = ({ team }: { team: Team }) => {
  const { isAdmin, userId } = useRoles();
  const userTeams = useTeams();
  const canEdit = isAdmin || userTeams.some((t) => t.id === team.id);
  const queryClient = useQueryClient();
  const notifications = useNotification();
  const teamId = team.id ?? 0;
  const [filter, setFilter] = useState('');
  const [confirmSelfRemove, setConfirmSelfRemove] = useState(false);

  const { data: members } = useSuspenseQuery(listMembersOptions({ path: { id: teamId } }));
  const { data: allUsers } = useSuspenseQuery(listUsersOptions());

  const membersQueryKey = listMembersOptions({ path: { id: teamId } }).queryKey;
  const teamsQueryKey = listTeamsOptions().queryKey;
  const userTeamsQueryKey = userTeamsOptions().queryKey;

  const updateTeamMemberCount = (count: number) => {
    queryClient.setQueryData(teamsQueryKey, (teams: Team[] | undefined) => teams?.map((t) => (t.id === teamId ? { ...t, memberCount: count } : t)));
  };

  const addMember = useMutation({
    ...addMemberMutation(),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(membersQueryKey, data);
      updateTeamMemberCount(data.length);
      if (variables.path.userId === userId) {
        queryClient.setQueryData(userTeamsQueryKey, (teams: Team[] | undefined) => [...(teams ?? []), { ...team, memberCount: data.length }]);
      }
    },
    onError: (e) => {
      notifications.handleError('Failed to add Member', e);
    },
  });

  const removeMember = useMutation({
    ...removeMemberMutation(),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(membersQueryKey, data);
      updateTeamMemberCount(data.length);
      if (variables.path.userId === userId) {
        queryClient.setQueryData(userTeamsQueryKey, (teams: Team[] | undefined) => teams?.filter((t) => t.id !== teamId));
      }
    },
    onError: (e) => {
      notifications.handleError('Failed to remove Member', e);
    },
  });

  const handleRemove = (memberId: number) => {
    if (memberId === userId && !isAdmin) {
      setConfirmSelfRemove(true);
    } else {
      removeMember.mutate({ path: { id: teamId, userId: memberId } });
    }
  };

  const memberIds = new Set(members.map((m) => m.id));
  const nonMembers = filter ? allUsers.filter((u) => !memberIds.has(u.id) && (u.username ?? '').toLowerCase().includes(filter.toLowerCase())) : [];

  return (
    <Stack gap={2}>
      <ContainedList label={`Members of ${team.name}`} kind="on-page">
        {members.map((user) => (
          <ContainedListItem
            key={user.id}
            action={
              canEdit && (
                <IconButton
                  label="Remove from Team"
                  kind={'danger--ghost' as 'ghost'}
                  onClick={() => {
                    handleRemove(user.id ?? 0);
                  }}
                >
                  <Subtract />
                </IconButton>
              )
            }
          >
            {user.username ?? ''}
          </ContainedListItem>
        ))}
        {members.length === 0 && <InlineNotification kind="info" title="No members in this team" lowContrast hideCloseButton />}
      </ContainedList>
      {canEdit && (
        <ContainedList
          label="Add Users"
          kind="on-page"
          action={
            <ExpandableSearch
              id="member-search"
              size="lg"
              labelText="Search Users"
              placeholder="Search Users"
              value={filter}
              onChange={(e) => {
                setFilter(e.target.value);
              }}
              onClear={() => {
                setFilter('');
              }}
            />
          }
        >
          {nonMembers.map((user) => (
            <ContainedListItem
              key={user.id}
              action={
                <IconButton
                  label="Add to Team"
                  kind="ghost"
                  onClick={() => {
                    addMember.mutate({ path: { id: teamId, userId: user.id ?? 0 } });
                  }}
                >
                  <Add />
                </IconButton>
              }
            >
              {user.username ?? ''}
            </ContainedListItem>
          ))}
        </ContainedList>
      )}

      <Modal
        open={confirmSelfRemove}
        danger
        modalLabel={'Leave Team'}
        modalHeading={team.name}
        primaryButtonText="Leave"
        secondaryButtonText="Cancel"
        onRequestClose={() => {
          setConfirmSelfRemove(false);
        }}
        onSecondarySubmit={() => {
          setConfirmSelfRemove(false);
        }}
        onRequestSubmit={() => {
          removeMember.mutate({ path: { id: teamId, userId: userId } });
          setConfirmSelfRemove(false);
        }}
      >
        <p>You will no longer be able to manage this Team and its Folders. Are you sure?</p>
      </Modal>
    </Stack>
  );
};
