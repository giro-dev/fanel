import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { NavLink, Route, Routes } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Households } from './pages/Households'
import { Placeholder } from './pages/Placeholder'

const queryClient = new QueryClient()

function Shell() {
  const { t } = useTranslation()
  const nav = [
    ['/menu', t('nav.menu')],
    ['/calendari', t('nav.calendar')],
    ['/compra', t('nav.shopping')],
    ['/tasques', t('nav.chores')],
  ]
  return (
    <div className="app-shell">
      <header><h1>Fanel</h1><NavLink to="/households">{t('households.title')}</NavLink></header>
      <nav>{nav.map(([path, label]) => <NavLink key={path} to={path}>{label}</NavLink>)}</nav>
      <main>
        <Routes>
          <Route path="/households" element={<Households />} />
          <Route path="/menu" element={<Placeholder title={t('nav.menu')} />} />
          <Route path="/calendari" element={<Placeholder title={t('nav.calendar')} />} />
          <Route path="/compra" element={<Placeholder title={t('nav.shopping')} />} />
          <Route path="/tasques" element={<Placeholder title={t('nav.chores')} />} />
          <Route path="*" element={<Placeholder title="Fanel" />} />
        </Routes>
      </main>
    </div>
  )
}

export function App() {
  return <QueryClientProvider client={queryClient}><Shell /></QueryClientProvider>
}
