import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

export type ShoppingItem = {
  id: string
  householdId: string
  text: string
  done: boolean
  createdAt: string
}

const key = (householdId: string) => ['shopping', householdId] as const

export function useShoppingList(householdId: string | undefined) {
  return useQuery({
    queryKey: householdId ? key(householdId) : ['shopping', undefined],
    queryFn: () => api<ShoppingItem[]>(`/households/${householdId}/shopping`),
    enabled: Boolean(householdId),
  })
}

export function useAddShoppingItem(householdId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (text: string) => api<ShoppingItem>(`/households/${householdId}/shopping`, {
      method: 'POST',
      body: JSON.stringify({ text }),
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key(householdId) }),
  })
}

export function useUpdateShoppingItem(householdId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, text, done }: { id: string; text?: string; done?: boolean }) =>
      api<ShoppingItem>(`/households/${householdId}/shopping/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ text, done }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key(householdId) }),
  })
}

export function useDeleteShoppingItem(householdId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/households/${householdId}/shopping/${id}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key(householdId) }),
  })
}

export function useClearDoneShopping(householdId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api<{ deleted: number }>(`/households/${householdId}/shopping/done`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key(householdId) }),
  })
}
