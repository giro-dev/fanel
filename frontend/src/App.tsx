import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { NavLink, Route, Routes } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Households } from './pages/Households'
import { Menu } from './pages/Menu'
import { Shopping } from './pages/Shopping'
import { Calendar } from './pages/Calendar'
import { Chores } from './pages/Chores'
import { Placeholder } from './pages/Placeholder'
import { HouseholdProvider, useHousehold } from './context/HouseholdContext'
import { WhoAmI } from './components/WhoAmI'
import { useHouseholdEvents } from './hooks/useHouseholdEvents'

const queryClient = new QueryClient()

function Shell() {
  const { t } = useTranslation()
  const { household } = useHousehold()
  useHouseholdEvents(household?.id)
  const nav = [
    ['/menu', t('nav.menu')],
    ['/calendari', t('nav.calendar')],
    ['/compra', t('nav.shopping')],
    ['/tasques', t('nav.chores')],
  ]
  return (
    <div className="app-shell">
      <header>
        <h1>Fanel</h1>
        <NavLink to="/households">{t('households.title')}</NavLink>
        <WhoAmI />
      </header>
      <nav>{nav.map(([path, label]) => <NavLink key={path} to={path}>{label}</NavLink>)}</nav>
      <main>
        <Routes>
          <Route path="/households" element={<Households />} />
          <Route path="/menu" element={<Menu />} />
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
