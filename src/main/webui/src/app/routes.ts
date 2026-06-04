import { AppHeader } from '@app/layout/AppHeader';
import { DashboardPage } from '@app/pages/DashboardPage';
import { FolderPage } from '@app/pages/FolderPage';
import { ValuePage } from '@app/pages/ValuePage';
import { createBrowserRouter } from 'react-router-dom';

const router = createBrowserRouter([
  {
    Component: AppHeader,
    path: '/',
    children: [
      {
        Component: DashboardPage,
        index: true,
      },
      {
        Component: FolderPage,
        path: 'folder/:folderId',
      },
      {
        Component: ValuePage,
        path: 'folder/:folderId/value/:valueId',
      },
    ],
  },
]);

export default router;
