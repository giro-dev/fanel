import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

export type Member = { id: string; householdId: string; name: string; role: 'ADULT' | 'CHILD'; color?: string }
export type Household = { id: string; name: string; locale: string; timezone: string }

type HouseholdContextValue = {
  household?: Household
  member?: Member
  members: Member[]
  selectHousehold: (id: string) => void
  selectMember: (member: Member, pin?: string) => Promise<boolean>
  clearMember: () => void
}

const HouseholdContext = createContext<HouseholdContextValue | null>(null)

const HOUSEHOLD_KEY = 'fanel.householdId'
const MEMBER_KEY = 'fanel.memberId'

export function HouseholdProvider({ children }: { children: ReactNode }) {
  const [householdId, setHouseholdId] = useState(() => localStorage.getItem(HOUSEHOLD_KEY) ?? undefined)
  const [memberId, setMemberId] = useState(() => localStorage.getItem(MEMBER_KEY) ?? undefined)

  const households = useQuery({ queryKey: ['households'], queryFn: () => api<Household[]>('/households') })
  const household = useMemo(
    () => households.data?.find((h) => h.id === householdId) ?? households.data?.[0],
    [households.data, householdId],
  )

  const membersQuery = useQuery({
    queryKey: ['members', household?.id],
    queryFn: () => api<Member[]>(`/households/${household!.id}/members`),
    enabled: !!household,
  })
  const members = membersQuery.data ?? []
  const member = members.find((m) => m.id === memberId)

  useEffect(() => {
    if (household && household.id !== householdId) {
      setHouseholdId(household.id)
      localStorage.setItem(HOUSEHOLD_KEY, household.id)
    }
  }, [household, householdId])

  const value: HouseholdContextValue = {
    household,
    member,
    members,
    selectHousehold: (id) => {
      setHouseholdId(id)
      localStorage.setItem(HOUSEHOLD_KEY, id)
      setMemberId(undefined)
      localStorage.removeItem(MEMBER_KEY)
    },
    selectMember: async (candidate, pin) => {
      if (!household) return false
      const result = await api<{ valid: boolean }>(
        `/households/${household.id}/members/${candidate.id}/verify-pin`,
        { method: 'POST', body: JSON.stringify({ pin: pin ?? '' }) },
      )
      if (!result.valid) return false
      setMemberId(candidate.id)
      localStorage.setItem(MEMBER_KEY, candidate.id)
      return true
    },
    clearMember: () => {
      setMemberId(undefined)
      localStorage.removeItem(MEMBER_KEY)
    },
  }

  return <HouseholdContext.Provider value={value}>{children}</HouseholdContext.Provider>
}

export function useHousehold() {
  const ctx = useContext(HouseholdContext)
  if (!ctx) throw new Error('useHousehold must be used within HouseholdProvider')
  return ctx
}
