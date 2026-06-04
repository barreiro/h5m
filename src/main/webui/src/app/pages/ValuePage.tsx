import { ArrowLeft } from '@carbon/icons-react';
import {
  Button,
  ErrorBoundary,
  InlineLoading,
  SkeletonText,
} from '@carbon/react';
import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Suspense } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

const valueDataOptions = (id: number) =>
  queryOptions({
    queryKey: ['valueData', id],
    queryFn: async () => {
      const { data } = await axios.get<unknown>(`/api/value/${String(id)}/data`);
      return data;
    },
  });

const ValueContent = ({ valueId }: { valueId: number }) => {
  const navigate = useNavigate();
  const { data } = useSuspenseQuery(valueDataOptions(valueId));
  return (
    <>
      <Button
        kind="ghost"
        size="sm"
        renderIcon={ArrowLeft}
        onClick={() => { void navigate(-1); }}
      >
        Back
      </Button>
      <pre style={{ marginTop: 'var(--cds-spacing-05)', overflow: 'auto', maxHeight: '80vh' }}>
        {JSON.stringify(data, null, 2)}
      </pre>
    </>
  );
};

export const ValuePage = () => {
  const { valueId } = useParams<{ valueId: string }>();
  const vId = Number(valueId);
  if (!valueId || isNaN(vId)) {
    return null;
  }
  return (
    <div style={{ padding: 'var(--cds-spacing-05)', marginTop: 'var(--cds-spacing-09)' }}>
      <ErrorBoundary fallback={<InlineLoading status="error" description="Failed to load value" />}>
        <Suspense fallback={<SkeletonText paragraph={true} lineCount={5} />}>
          <ValueContent valueId={vId} />
        </Suspense>
      </ErrorBoundary>
    </div>
  );
};
