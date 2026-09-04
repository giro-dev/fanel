import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { NavLink, Route, Routes } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Households } from './pages/Households'
import { Placeholder } from './pages/Placeholder'
import { HouseholdProvider, useHousehold } from './household/HouseholdContext'
import { Menu } from './pages/Menu'
import { Shopping } from './pages/Shopping'

const queryClient = new QueryClient()

function Shell() {
  const { t } = useTranslation()
  const { households, current, setCurrent } = useHousehold()
  const nav = [
    ['/menu', t('nav.menu')],
    ['/calendari', t('nav.calendar')],
    ['/compra', t('nav.shopping')],
    ['/tasques', t('nav.chores')],
  ]
  return (
    <div className="app-shell">
      <header><h1>Fanel</h1><div className="header-actions">
        {households.length > 1 && <label className="household-picker"><span className="sr-only">{t('households.title')}</span><select value={current?.id ?? ''} onChange={(event) => setCurrent(event.target.value)}>{households.map((household) => <option key={household.id} value={household.id}>{household.name}</option>)}</select></label>}
        <NavLink to="/households">{t('households.title')}</NavLink>
      </div></header>
      <nav>{nav.map(([path, label]) => <NavLink key={path} to={path}>{label}</NavLink>)}</nav>
      <main>
        <Routes>
          <Route path="/households" element={<Households />} />
          <Route path="/menu" element={<Menu />} />
          <Route path="/calendari" element={<Placeholder title={t('nav.calendar')} />} />
          <Route path="/compra" element={<Shopping />} />
          <Route path="/tasques" element={<Placeholder title={t('nav.chores')} />} />
          <Route path="*" element={<Placeholder title="Fanel" />} />
        </Routes>
      </main>
    </div>
  )
}

export function App() {
  return <QueryClientProvider client={queryClient}><HouseholdProvider><Shell /></HouseholdProvider></QueryClientProvider>
}
