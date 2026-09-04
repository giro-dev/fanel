import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

export type Household = { id: string; name: string; locale: string; timezone: string }

type HouseholdContextValue = {
  households: Household[]
  current?: Household
  setCurrent: (id: string) => void
}

const HouseholdContext = createContext<HouseholdContextValue | undefined>(undefined)
const storageKey = 'fanel.household'

export function HouseholdProvider({ children }: { children: ReactNode }) {
  const [selectedId, setSelectedId] = useState(() => localStorage.getItem(storageKey) ?? '')
  const query = useQuery({
    queryKey: ['households'],
    queryFn: () => api<Household[]>('/households'),
  })
  const households = useMemo(() => query.data ?? [], [query.data])
  const current = households.find((household) => household.id === selectedId)

  useEffect(() => {
    if (!selectedId && households.length > 0) {
      setSelectedId(households[0].id)
    }
  }, [households, selectedId])

  useEffect(() => {
    if (selectedId) localStorage.setItem(storageKey, selectedId)
  }, [selectedId])

  const value = useMemo(() => ({
    households,
    current,
    setCurrent: (id: string) => setSelectedId(id),
  }), [current, households])

  return <HouseholdContext.Provider value={value}>{children}</HouseholdContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useHousehold() {
  const context = useContext(HouseholdContext)
  if (!context) throw new Error('useHousehold must be used within HouseholdProvider')
  return context
}
