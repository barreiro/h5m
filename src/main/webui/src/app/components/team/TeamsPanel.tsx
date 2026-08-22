import type { Team } from '@client/types.gen.ts';

import { TeamNameModal } from '@app/components/team/TeamNameModal.tsx';
import { useNotification } from '@app/context/useNotification.tsx';
import { useRoles } from '@app/context/useRoles.tsx';
import { Add, Edit, TrashCan } from '@carbon/icons-react';
import { ContainedList, ContainedListItem, ExpandableSearch, InlineNotification, Modal, Tag, IconButton } from '@carbon/react';
import { deleteTeamMutation, listTeamsOptions } from '@client/@tanstack/react-query.gen.ts';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useState } from 'react';

export const TeamsPanel = ({ onSelect }: { onSelect: (team: Team | null) => void }) => {
  const { isAdmin } = useRoles();
  const queryClient = useQueryClient();
  const notifications = useNotification();
  const { data: teams } = useSuspenseQuery(listTeamsOptions());

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [editTeam, setEditTeam] = useState<Team | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Team | null>(null);
  const [teamFilter, setTeamFilter] = useState('');

  const deleteTeam = useMutation({
    ...deleteTeamMutation(),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: listTeamsOptions().queryKey });
      const deleted = teams.find((t) => t.id === variables.path.id);
      notifications.success(`Team '${deleted?.name ?? ''}' deleted`);
      if (selectedId === variables.path.id) {
        setSelectedId(null);
        onSelect(null);
      }
      setConfirmDelete(null);
    },
    onError: (_e, variables) => {
      const deleted = teams.find((t) => t.id === variables.path.id);
      notifications.handleError(`Failed to delete Team '${deleted?.name ?? ''}'`, _e);
      setConfirmDelete(null);
    },
  });

  const filteredTeams = teams.filter((t) => !teamFilter || t.name.toLowerCase().includes(teamFilter.toLowerCase()));

  return (
    <>
      <ContainedList
        label="Teams"
        kind="on-page"
        action={
          <>
            <ExpandableSearch
              id="team-filter"
              size="lg"
              labelText="Filter Team"
              placeholder="Filter Team"
              value={teamFilter}
              onChange={(e) => {
                setTeamFilter(e.target.value);
              }}
              onClear={() => {
                setTeamFilter('');
              }}
            />
            {isAdmin && (
              <IconButton
                label="Create Team"
                kind="ghost"
                onClick={() => {
                  setCreateOpen(true);
                }}
              >
                <Add />
              </IconButton>
            )}
          </>
        }
      >
        {filteredTeams.map((team) => (
          <ContainedListItem
            key={team.id}
            onClick={() => {
              setSelectedId(team.id ?? null);
              onSelect(team);
            }}
            action={
              <>
                <IconButton
                  label={`${String(team.memberCount ?? 0)} members`}
                  kind="ghost"
                  onMouseDown={(e) => {
                    e.preventDefault();
                  }}
                  style={{ cursor: 'default' }}
                >
                  <Tag size="lg">{team.memberCount ?? 0}</Tag>
                </IconButton>
                {isAdmin && (
                  <>
                    <IconButton
                      label="Rename Team"
                      kind="ghost"
                      onClick={(e) => {
                        e.stopPropagation();
                        setEditTeam(team);
                      }}
                    >
                      <Edit />
                    </IconButton>
                    <IconButton
                      label="Delete Team"
                      kind={'danger--ghost' as 'ghost'}
                      onClick={(e) => {
                        e.stopPropagation();
                        setConfirmDelete(team);
                      }}
                    >
                      <TrashCan />
                    </IconButton>
                  </>
                )}
              </>
            }
          >
            {team.name}
          </ContainedListItem>
        ))}
        {teams.length === 0 && <InlineNotification kind="info" title="Create a team to get started." lowContrast hideCloseButton />}
        {teams.length !== 0 && filteredTeams.length === 0 && <InlineNotification kind="info" title="No teams found." lowContrast hideCloseButton />}
      </ContainedList>

      <TeamNameModal
        team={null}
        open={createOpen}
        onClose={() => {
          setCreateOpen(false);
        }}
      />

      <TeamNameModal
        team={editTeam}
        open={editTeam !== null}
        onClose={() => {
          setEditTeam(null);
        }}
      />

      <Modal
        open={confirmDelete !== null}
        danger
        modalLabel={'Delete Team'}
        modalHeading={confirmDelete?.name}
        primaryButtonText="Delete"
        secondaryButtonText="Cancel"
        onRequestClose={() => {
          setConfirmDelete(null);
        }}
        onSecondarySubmit={() => {
          setConfirmDelete(null);
        }}
        onRequestSubmit={() => {
          deleteTeam.mutate({ path: { id: confirmDelete?.id ?? 0 } });
        }}
      >
        <p>Are you sure you want to delete this team? This action cannot be undone.</p>
      </Modal>
    </>
  );
};
