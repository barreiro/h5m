import { AuthActions } from '@app/layout/AuthActions.tsx';
import { DOCS_IFRAME_STYLE, docsIframeSrc } from '@app/pages/SitePage';
import { Help } from '@carbon/icons-react';
import {
  Content,
  ErrorBoundary,
  Header,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderMenuButton,
  HeaderName,
  InlineLoading,
  SideNav,
  SideNavItems,
  SideNavLink,
  SkeletonText,
  SkipToContent,
  Theme,
} from '@carbon/react';
import { listFoldersOptions } from '@client/@tanstack/react-query.gen.ts';
import { useSuspenseQuery } from '@tanstack/react-query';
import { ReactNode, Suspense, useCallback, useState } from 'react';
import { Link, Outlet, useParams } from 'react-router-dom';

const NavFolders = () => {
  const { data: folders } = useSuspenseQuery(listFoldersOptions());
  const { folderId } = useParams<{ folderId: string }>();
  return (
    <SideNavItems>
      {folders.map((folder) => (
        <SideNavLink key={folder.id} as={Link} to={`/folder/${String(folder.id)}`} isActive={folder.id === Number(folderId)}>
          {folder.name}
        </SideNavLink>
      ))}
    </SideNavItems>
  );
};

export const AppHeader = ({ children }: { children?: ReactNode }) => {
  const [sideNavOpen, setSideNavOpen] = useState(false);
  const [docsOpen, setDocsOpen] = useState(false);
  const toggleSideNav = useCallback(() => {
    setSideNavOpen((prev) => !prev);
  }, []);
  const toggleDocs = useCallback(() => {
    setDocsOpen((prev) => !prev);
  }, []);
  return (
    <>
      <Theme theme="g100">
        <Header aria-label="Carbon App">
          <SkipToContent />
          <HeaderMenuButton aria-label="Hamburger menu" onClick={toggleSideNav} isActive={sideNavOpen} isCollapsible={true} />
          <HeaderName as={Link} to="/" prefix="h5m">
            Horreum
          </HeaderName>
          <HeaderGlobalBar>
            <HeaderGlobalAction aria-label="Documentation" onClick={toggleDocs} isActive={docsOpen} tooltipAlignment="end">
              <Help size={24} />
            </HeaderGlobalAction>
            <AuthActions />
          </HeaderGlobalBar>
        </Header>
        <SideNav aria-label="Side navigation" expanded={sideNavOpen} isPersistent={false} isFixedNav={false}>
          <ErrorBoundary
            fallback={
              <div style={{ padding: 'var(--cds-spacing-05)' }}>
                <InlineLoading status="error" description="Folder load failed" />
              </div>
            }
          >
            <Suspense
              fallback={
                <div style={{ padding: 'var(--cds-spacing-05)' }}>
                  <SkeletonText paragraph={true} lineCount={50} />
                </div>
              }
            >
              <NavFolders />
            </Suspense>
          </ErrorBoundary>
        </SideNav>
      </Theme>
      {children ?? (
        <>
          <Content>
            <Outlet />
          </Content>
          <iframe
            src={docsIframeSrc()}
            title="Documentation"
            style={{
              ...DOCS_IFRAME_STYLE,
              display: docsOpen ? 'block' : 'none',
              position: 'fixed',
              top: '3rem',
              zIndex: 8000,
            }}
          />
        </>
      )}
    </>
  );
};
