import type { Team } from '@client/types.gen.ts';

import { extractErrorMessage } from '@app/context/NotificationProvider.tsx';
import { fieldError } from '@app/validation';
import { Button, ComposedModal, Form, InlineNotification, ModalBody, ModalFooter, ModalHeader, TextInput } from '@carbon/react';
import { createTeamMutation, listTeamsOptions, renameTeamMutation } from '@client/@tanstack/react-query.gen.ts';
import { zCreateTeamBody } from '@client/zod.gen.ts';
import { useForm } from '@tanstack/react-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

export const TeamNameModal = ({ team, open, onClose }: { team: Team | null; open: boolean; onClose: () => void }) => {
  const isRename = team !== null;
  const [submitError, setSubmitError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const onSuccess = () => {
    void queryClient.invalidateQueries({ queryKey: listTeamsOptions().queryKey });
    form.reset();
    setSubmitError(null);
    onClose();
  };

  const createTeam = useMutation({
    ...createTeamMutation(),
    onSuccess,
    onError: (e) => {
      setSubmitError(extractErrorMessage(e) ?? 'Failed to create team');
    },
  });

  const renameTeam = useMutation({
    ...renameTeamMutation(),
    onSuccess,
    onError: (e) => {
      setSubmitError(extractErrorMessage(e) ?? 'Failed to rename team');
    },
  });

  const mutation = isRename ? renameTeam : createTeam;

  const form = useForm({
    defaultValues: { name: team?.name ?? '' },
    validators: {
      onSubmit: zCreateTeamBody,
    },
    onSubmit: ({ value }) => {
      setSubmitError(null);
      if (isRename) {
        renameTeam.mutate({ path: { id: team.id ?? 0 }, body: { name: value.name } });
      } else {
        createTeam.mutate({ body: { name: value.name } });
      }
    },
  });

  const handleClose = () => {
    form.reset();
    setSubmitError(null);
    onClose();
  };

  return (
    <ComposedModal open={open} onClose={handleClose}>
      <ModalHeader label={isRename ? `Rename Team` : undefined} title={isRename ? team.name : 'Create Team'} />
      <ModalBody>
        <Form
          onSubmit={(e) => {
            e.preventDefault();
          }}
        >
          <form.Field name="name">
            {(field) => (
              <TextInput
                id="team-name"
                labelText="Team name"
                placeholder="e.g. performance"
                value={field.state.value}
                onChange={(e) => {
                  field.handleChange(e.target.value);
                }}
                onBlur={field.handleBlur}
                invalid={field.state.meta.errors.length > 0}
                invalidText={fieldError(field.state.meta.errors)}
              />
            )}
          </form.Field>
          {submitError && <InlineNotification kind="error" title={submitError} lowContrast hideCloseButton />}
        </Form>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose}>
          Cancel
        </Button>
        <Button
          kind="primary"
          onClick={() => {
            void form.handleSubmit();
          }}
          disabled={mutation.isPending}
        >
          {mutation.isPending ? 'Saving...' : 'Save'}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};
