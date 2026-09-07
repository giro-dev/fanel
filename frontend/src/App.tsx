import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { NavLink, Route, Routes } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Households } from './pages/Households'
import { Menu } from './pages/Menu'
import { Recipes } from './pages/Recipes'
import { Shopping } from './pages/Shopping'
import { Calendar } from './pages/Calendar'
import { Chores } from './pages/Chores'
import { Placeholder } from './pages/Placeholder'
import { HouseholdProvider, useHousehold } from './context/HouseholdContext'
import { WhoAmI } from './components/WhoAmI'
import { ThemeToggle } from './components/ThemeToggle'
import { useHouseholdEvents } from './hooks/useHouseholdEvents'

const queryClient = new QueryClient()

const stroke = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' } as const

const icons = {
  menu: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <path d="M4 6h16M4 12h16M4 18h10" />
    </svg>
  ),
  recipes: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v17.5H6.5A2.5 2.5 0 0 0 4 22z" />
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
    </svg>
  ),
  calendar: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <rect x="3" y="4.5" width="18" height="17" rx="2.5" />
      <path d="M8 2.5v4M16 2.5v4M3 10h18" />
    </svg>
  ),
  shopping: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <path d="M4 7h16l-1.5 12.5a2 2 0 0 1-2 1.5h-9a2 2 0 0 1-2-1.5z" />
      <path d="M8 10V6a4 4 0 0 1 8 0v4" />
    </svg>
  ),
  chores: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <path d="M9 6h12M9 12h12M9 18h12" />
      <path d="M3.5 6l1.2 1.2L6.8 5M3.5 12l1.2 1.2 2.1-2.2M3.5 18l1.2 1.2 2.1-2.2" />
    </svg>
  ),
  households: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <path d="M3.5 10.5 12 3l8.5 7.5" />
      <path d="M5.5 9v11h13V9" />
      <path d="M10 20v-6h4v6" />
    </svg>
  ),
}

function Shell() {
  const { t } = useTranslation()
  const { household } = useHousehold()
  useHouseholdEvents(household?.id)
  const nav = [
    ['/menu', t('nav.menu'), icons.menu],
    ['/receptes', t('nav.recipes'), icons.recipes],
    ['/calendari', t('nav.calendar'), icons.calendar],
    ['/compra', t('nav.shopping'), icons.shopping],
    ['/tasques', t('nav.chores'), icons.chores],
  ] as const
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">Fanel</div>
        <nav className="main-nav">
          {nav.map(([path, label, icon]) => (
            <NavLink key={path} to={path}>{icon}<span>{label}</span></NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="footer-actions">
            <NavLink to="/households">{icons.households}<span>{t('households.title')}</span></NavLink>
            <ThemeToggle />
          </div>
          <WhoAmI />
        </div>
      </aside>
      <div className="topbar">
        <div className="brand">Fanel</div>
        <div className="topbar-actions">
          <NavLink to="/households" aria-label={t('households.title')} className="icon-link">{icons.households}</NavLink>
          <ThemeToggle />
          <WhoAmI />
        </div>
      </div>
      <main className="content">
        <Routes>
          <Route path="/households" element={<Households />} />
          <Route path="/menu" element={<Menu />} />
          <Route path="/receptes" element={<Recipes />} />
          <Route path="/calendari" element={<Calendar />} />
          <Route path="/compra" element={<Shopping />} />
          <Route path="/tasques" element={<Chores />} />
          <Route path="*" element={<Placeholder title="Fanel" />} />
        </Routes>
      </main>
    </div>
  )
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <HouseholdProvider>
        <Shell />
      </HouseholdProvider>
    </QueryClientProvider>
  )
}
