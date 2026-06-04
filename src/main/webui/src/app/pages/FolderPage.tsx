import type { Node as ApiNode, Value } from '@client/types.gen.ts';

import { DataTab } from '@app/components/DataTab';
import { NodeGraphVisualizer } from '@app/components/NodeGraphVisualizer';
import { useState } from 'react';
import {
  Button,
  DataTable,
  ErrorBoundary,
  InlineLoading,
  InlineNotification,
  MenuButton,
  MenuItem,
  Pagination,
  SkeletonText,
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  Tag,
} from '@carbon/react';
import { byIdOptions, getRecalculationStatusOptions, listFoldersOptions } from '@client/@tanstack/react-query.gen.ts';
import { queryOptions, useQuery, useSuspenseQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Suspense, useCallback, useEffect } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import '@app/pages/DashboardPage.css';
import { CreateNodeModal } from '@app/components/CreateNodeModal';
import { DeleteNodeModal } from '@app/components/DeleteNodeModal';
import { EditNodeModal } from '@app/components/EditNodeModal';

const NodesTab = ({ groupId }: { groupId: number }) => {
  const { data: nodeGroup } = useSuspenseQuery(byIdOptions({ path: { id: groupId } }));
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [nodeToDelete, setNodeToDelete] = useState<ApiNode | null>(null);
  const [nodeToEdit, setNodeToEdit] = useState<ApiNode | null>(null);
  const [recalculationId, setRecalculationId] = useState<string | null>(null);

  const { data: recalcStatus } = useQuery({
    ...getRecalculationStatusOptions({ path: { id: recalculationId ?? '' } }),
    enabled: recalculationId !== null,
    refetchInterval: (query) => query.state.data?.state === 'RUNNING' ? 2000 : false,
  });

  useEffect(() => {
    if (recalcStatus?.state === 'COMPLETED') {
      const timer = setTimeout(() => setRecalculationId(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [recalcStatus?.state]);

  return (
    <>
      <Button
          kind="primary"
          size="md"
          onClick={() => setIsCreateOpen(true)}
          className="create-folder-btn"
          style={{ margin: 'var(--cds-spacing-05)' }}>
          Create Node
      </Button>
      {recalcStatus?.state === 'RUNNING' && (
        <div style={{ margin: '0 var(--cds-spacing-05)' }}>
          <InlineLoading
            description={`Recalculating… ${recalcStatus.completedRoots ?? 0}/${recalcStatus.totalRoots ?? 0} values`}
            status="active"
          />
        </div>
      )}
      {recalcStatus?.state === 'COMPLETED' && (
        <InlineNotification
          kind="success"
          title="Recalculation complete"
          subtitle={`Finished in ${recalcStatus.durationMs ?? 0}ms`}
          onClose={() => setRecalculationId(null)}
          style={{ margin: '0 var(--cds-spacing-05)', maxWidth: 'none' }}
        />
      )}
      {recalcStatus?.state === 'FAILED' && (
        <InlineNotification
          kind="error"
          title="Recalculation failed"
          subtitle={recalcStatus.error ?? 'Unknown error'}
          onClose={() => setRecalculationId(null)}
          style={{ margin: '0 var(--cds-spacing-05)', maxWidth: 'none' }}
        />
      )}
      <CreateNodeModal open={isCreateOpen} onClose={() => setIsCreateOpen(false)} groupId={groupId} />
      <DeleteNodeModal node={nodeToDelete} onClose={() => setNodeToDelete(null)} />
      <EditNodeModal node={nodeToEdit} onClose={() => setNodeToEdit(null)} onRecalculation={setRecalculationId} />
      {nodeGroup.sources?.length === 0 ? (
        <p style={{ margin: 'var(--cds-spacing-05)' }}>No nodes defined yet. Use the Create Node button above to get started.</p>
      ) : (
        <StructuredListWrapper>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>Name</StructuredListCell>
              <StructuredListCell head>Type</StructuredListCell>
              <StructuredListCell head>FQDN</StructuredListCell>
              <StructuredListCell head>Operation</StructuredListCell>
              <StructuredListCell head />
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {nodeGroup.sources?.map((node: ApiNode) => (
              <StructuredListRow key={node.id}>
                <StructuredListCell>{node.name}</StructuredListCell>
                <StructuredListCell>
                  <Tag size="sm">{node.type}</Tag>
                </StructuredListCell>
                <StructuredListCell>{node.fqdn}</StructuredListCell>
                <StructuredListCell>{node.operation}</StructuredListCell>
                <StructuredListCell>
                  <MenuButton label="Action" kind="ghost" size="sm" menuAlignment="bottom-end">
                    <MenuItem label="Delete" kind="danger" onClick={() => setNodeToDelete(node)} />
                    <MenuItem label="Edit" onClick={() => setNodeToEdit(node)} />
                  </MenuButton>
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
    </>
  );
};

const GraphVisualizer = ({ groupId }: { groupId: number }) => {
  const { data: nodeGroup } = useSuspenseQuery(byIdOptions({ path: { id: groupId } }));
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  return (
    <>
      <Button
        kind="primary"
        size="md"
        onClick={() => setIsCreateOpen(true)}
        className="create-folder-btn"
        style={{ margin: 'var(--cds-spacing-05)' }}
      >
        Create Node
      </Button>
      <CreateNodeModal open={isCreateOpen} onClose={() => setIsCreateOpen(false)} groupId={groupId} />
      <NodeGraphVisualizer nodeGroup={nodeGroup} />
    </>
  );
};

interface FolderStatus {
  id: number;
  name: string;
  uploadCount: number;
  nodeCount: number;
  changeCount: number;
  lastUpload: string | null;
  lastChange: string | null;
}

const folderStatusOptions = (folderId: number) =>
  queryOptions<FolderStatus | null>({
    queryKey: ['folderStatus', folderId],
    queryFn: async () => {
      const { data } = await axios.get<FolderStatus[]>('/api/folder/summary', { params: { id: folderId } });
      return data[0] ?? null;
    },
  });

const StatusTab = ({ folderId }: { folderId: number }) => {
  const { data: status } = useSuspenseQuery(folderStatusOptions(folderId));
  if (!status) {
    return <p>No status available</p>;
  }
  return (
    <StructuredListWrapper>
      <StructuredListBody>
        <StructuredListRow>
          <StructuredListCell>Uploads</StructuredListCell>
          <StructuredListCell>{status.uploadCount}</StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>Nodes</StructuredListCell>
          <StructuredListCell>{status.nodeCount}</StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>Changes</StructuredListCell>
          <StructuredListCell>{status.changeCount}</StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>Last upload</StructuredListCell>
          <StructuredListCell>{status.lastUpload ?? '—'}</StructuredListCell>
        </StructuredListRow>
        <StructuredListRow>
          <StructuredListCell>Last change</StructuredListCell>
          <StructuredListCell>{status.lastChange ?? '—'}</StructuredListCell>
        </StructuredListRow>
      </StructuredListBody>
    </StructuredListWrapper>
  );
};

const nodeValuesOptions = (nodeId: number, page: number, size: number) =>
  queryOptions<Value[]>({
    queryKey: ['nodeValues', nodeId, page, size],
    queryFn: async () => {
      const { data } = await axios.get<Value[]>(`/api/value/node/${String(nodeId)}`, { params: { page, size } });
      return data;
    },
  });

const UPLOAD_HEADERS = [
  { key: 'id', header: 'ID' },
  { key: 'node', header: 'Node' },
  { key: 'folder', header: 'Folder' },
  { key: 'createdAt', header: 'Created' },
];

const UploadsTab = ({ folderId, nodeId }: { folderId: number; nodeId: number }) => {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const { data: values = [] } = useQuery(nodeValuesOptions(nodeId, page - 1, pageSize));
  const { data: status } = useQuery(folderStatusOptions(folderId));
  const totalItems = status?.uploadCount ?? 0;

  const rows = values.map((v) => ({
    id: String(v.id),
    node: v.node?.name ?? '—',
    folder: v.folder?.name ?? '—',
    createdAt: (v as { createdAt?: string }).createdAt ?? '—',
  }));
  return (
    <>
      <DataTable rows={rows} headers={UPLOAD_HEADERS}>
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader {...getHeaderProps({ header })} key={header.key}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow {...getRowProps({ row })} key={row.id}>
                  {row.cells.map((cell) => (
                    <TableCell key={cell.id}>
                      {cell.info.header === 'id' ? (
                        <Link to={`/folder/${String(folderId)}/value/${cell.value as string}`}>{cell.value}</Link>
                      ) : (
                        cell.value ?? '—'
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DataTable>
      <Pagination
        totalItems={totalItems}
        page={page}
        pageSize={pageSize}
        pageSizes={[10, 20, 50]}
        onChange={({ page: p, pageSize: s }: { page: number; pageSize: number }) => {
          setPage(p);
          setPageSize(s);
        }}
      />
    </>
  );
};

const UploadsTabLoader = ({ folderId, groupId }: { folderId: number; groupId: number }) => {
  const { data: nodeGroup } = useSuspenseQuery(byIdOptions({ path: { id: groupId } }));
  const rootId = nodeGroup.root?.id;
  if (rootId == null) {
    return <p>No root node found</p>;
  }
  return <UploadsTab folderId={folderId} nodeId={rootId} />;
};

const TAB_ANCHORS = ['data', 'status', 'nodes', 'graph', 'uploads'];

const FolderContent = ({ folderId }: { folderId: number }) => {
  const { data: folders } = useSuspenseQuery(listFoldersOptions());
  const folder = folders.find((f) => f.id === folderId);
  const navigate = useNavigate();
  const location = useLocation();
  const selectedIndex = Math.max(0, TAB_ANCHORS.indexOf(location.hash.slice(1)));
  const onTabChange = useCallback(({ selectedIndex: i }: { selectedIndex: number }) => {
    void navigate({ hash: TAB_ANCHORS[i] }, { replace: true });
  }, [navigate]);
  if (!folder) {
    return <InlineLoading status="error" description="Folder not found" />;
  }
  return (
    <Tabs selectedIndex={selectedIndex} onChange={onTabChange}>
      <TabList aria-label="Folder tabs">
        <Tab>Data</Tab>
        <Tab>Status</Tab>
        <Tab>Nodes</Tab>
        <Tab>Graph</Tab>
        <Tab>Uploads</Tab>
      </TabList>
      <TabPanels>
        <TabPanel>
          {folder.name && folder.groupId != null ? (
            <DataTab folderName={folder.name} groupId={folder.groupId} />
          ) : (
            <p>Folder name not available</p>
          )}
        </TabPanel>
        <TabPanel>
          <ErrorBoundary fallback={<InlineLoading status="error" description="Failed to load status" />}>
            <Suspense fallback={<SkeletonText paragraph={true} lineCount={5} />}>
              <StatusTab folderId={folderId} />
            </Suspense>
          </ErrorBoundary>
        </TabPanel>
        <TabPanel>
          {folder.groupId != null ? (
            <ErrorBoundary fallback={<InlineLoading status="error" description="Failed to load nodes" />}>
              <Suspense fallback={<SkeletonText paragraph={true} lineCount={5} />}>
                <NodesTab groupId={folder.groupId} />
              </Suspense>
            </ErrorBoundary>
          ) : (
            <p>No node group associated with this folder</p>
          )}
        </TabPanel>
        <TabPanel>
          {folder.groupId != null ? (
            <ErrorBoundary fallback={<InlineLoading status="error" description="Failed to load nodes" />}>
              <Suspense fallback={<SkeletonText paragraph={true} lineCount={5} />}>
                <GraphVisualizer groupId={folder.groupId} />
              </Suspense>
            </ErrorBoundary>
          ) : (
            <p>No node group associated with this folder</p>
          )}
        </TabPanel>
        <TabPanel>
          {folder.groupId != null ? (
            <ErrorBoundary fallback={<InlineLoading status="error" description="Failed to load uploads" />}>
              <Suspense fallback={<SkeletonText paragraph={true} lineCount={5} />}>
                <UploadsTabLoader folderId={folderId} groupId={folder.groupId} />
              </Suspense>
            </ErrorBoundary>
          ) : (
            <p>No node group associated with this folder</p>
          )}
        </TabPanel>
      </TabPanels>
    </Tabs>
  );
};

export const FolderPage = () => {
  const { folderId } = useParams<{ folderId: string }>();
  const id = Number(folderId);
  if (!folderId || isNaN(id)) {
    return null;
  }
  return (
    <div style={{ padding: 'var(--cds-spacing-05)', marginTop: 'var(--cds-spacing-09)' }}>
      <ErrorBoundary fallback={<InlineLoading status="error" description="Failed to load folder" />}>
        <Suspense fallback={<SkeletonText paragraph={true} lineCount={5} />}>
          <FolderContent folderId={id} />
        </Suspense>
      </ErrorBoundary>
    </div>
  );
};
