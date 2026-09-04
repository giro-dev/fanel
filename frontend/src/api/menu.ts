import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

export type MealType = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER'
export type MealSlot = {
  id: string
  householdId: string
  date: string
  meal: MealType
  text: string
}
export type WeekMenu = {
  householdId: string
  isoYear: number
  isoWeek: number
  from: string
  to: string
  slots: MealSlot[]
}

export function useMenuWeek(householdId: string | undefined, week: string) {
  return useQuery({
    queryKey: ['menu', householdId, week],
    queryFn: () => api<WeekMenu>(`/households/${householdId}/menu?week=${week}`),
    enabled: Boolean(householdId),
  })
}

export function useSetMeal(householdId: string, week: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ date, meal, text }: { date: string; meal: MealType; text: string }) =>
      api<MealSlot>(`/households/${householdId}/menu/${date}/${meal}`, {
        method: 'PUT',
        body: JSON.stringify({ text }),
      }),
    onMutate: async ({ date, meal, text }) => {
      const key = ['menu', householdId, week] as const
      await queryClient.cancelQueries({ queryKey: key })
      const previous = queryClient.getQueryData<WeekMenu>(key)
      if (previous) {
        const slots = previous.slots.filter((slot) => !(slot.date === date && slot.meal === meal))
        if (text.trim()) {
          slots.push({ id: `optimistic-${date}-${meal}`, householdId, date, meal, text })
        }
        queryClient.setQueryData(key, { ...previous, slots })
      }
      return { previous }
    },
    onError: (_error, _variables, context) => {
      if (context?.previous) queryClient.setQueryData(['menu', householdId, week], context.previous)
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['menu', householdId, week] }),
  })
}
