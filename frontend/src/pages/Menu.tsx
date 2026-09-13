import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'

type MealType = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER'
type MealSlot = { id: string; dayOfWeek: number; mealType: MealType; text?: string; recipeId?: string }
type MealPlan = { id?: string; householdId: string; isoYear: number; isoWeek: number; slots: MealSlot[] }
type Recipe = { id: string; name: string }

const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER']

function isoWeek(date: Date): { year: number; week: number } {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const day = d.getUTCDay() || 7
  d.setUTCDate(d.getUTCDate() + 4 - day)
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
  return { year: d.getUTCFullYear(), week: Math.ceil(((d.getTime() - yearStart.getTime()) / 86400000 + 1) / 7) }
}

export function Menu() {
  const { t, i18n } = useTranslation()
  const { household } = useHousehold()
  const queryClient = useQueryClient()
  const [offset, setOffset] = useState(0)
  const [editing, setEditing] = useState<{ day: number; meal: MealType } | null>(null)
  const [value, setValue] = useState('')

  const base = new Date()
  base.setDate(base.getDate() + offset * 7)
  const { year, week } = isoWeek(base)

  const plan = useQuery({
    queryKey: ['menu', household?.id, year, week],
    queryFn: () => api<MealPlan>(`/households/${household!.id}/menu?year=${year}&week=${week}`),
    enabled: !!household,
  })

  const recipes = useQuery({
    queryKey: ['recipes', household?.id],
    queryFn: () => api<Recipe[]>(`/households/${household!.id}/recipes`),
    enabled: !!household,
  })

  const closeIfSame = (vars: { dayOfWeek: number; mealType: MealType }) =>
    setEditing((cur) => (cur?.day === vars.dayOfWeek && cur?.meal === vars.mealType ? null : cur))

  const setSlot = useMutation({
    mutationFn: (slot: { dayOfWeek: number; mealType: MealType; text: string | null; recipeId: string | null }) =>
      api<MealSlot>(`/households/${household!.id}/menu/slots?year=${year}&week=${week}`, {
        method: 'PUT', body: JSON.stringify(slot),
      }),
    onSuccess: async (_d, vars) => {
      closeIfSame(vars)
      await queryClient.invalidateQueries({ queryKey: ['menu'] })
    },
  })

  const clearSlot = useMutation({
    mutationFn: (slot: { dayOfWeek: number; mealType: MealType }) =>
      api<void>(`/households/${household!.id}/menu/slots?year=${year}&week=${week}&dayOfWeek=${slot.dayOfWeek}&mealType=${slot.mealType}`, {
        method: 'DELETE',
      }),
    onSuccess: async (_d, vars) => {
      closeIfSame(vars)
      await queryClient.invalidateQueries({ queryKey: ['menu'] })
    },
  })

  const dayNames = Array.from({ length: 7 }, (_, i) =>
    new Intl.DateTimeFormat(i18n.language, { weekday: 'long' }).format(new Date(2024, 0, 1 + i)))

  const slotFor = (day: number, meal: MealType) =>
    plan.data?.slots.find((s) => s.dayOfWeek === day && s.mealType === meal)

  const recipeName = (id?: string) => recipes.data?.find((r) => r.id === id)?.name

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>{t('menu.title')}</h2>
        <div className="week-nav">
          <button type="button" onClick={() => setOffset(offset - 1)}>←</button>
          <span>{t('menu.week', { week, year })}</span>
          <button type="button" onClick={() => setOffset(offset + 1)}>→</button>
        </div>
      </div>
      {plan.isPending && <p>{t('loading')}</p>}
      {plan.isError && <p role="alert">{t('error')}</p>}
      <datalist id="menu-recipes">
        {recipes.data?.map((r) => <option key={r.id} value={r.name} />)}
      </datalist>
      {plan.data && (
        <table className="menu-grid">
          <thead>
            <tr><th></th>{dayNames.map((d) => <th key={d}>{d}</th>)}</tr>
          </thead>
          <tbody>
            {MEAL_TYPES.map((meal) => (
              <tr key={meal}>
                <th>{t(`menu.meals.${meal}`)}</th>
                {dayNames.map((_, i) => {
                  const day = i + 1
                  const slot = slotFor(day, meal)
                  const isEditing = editing?.day === day && editing.meal === meal
                  const save = () => {
                    const trimmed = value.trim()
                    if (!trimmed) {
                      if (slot) clearSlot.mutate({ dayOfWeek: day, mealType: meal })
                      else setEditing(null)
                      return
                    }
                    const match = recipes.data?.find((r) => r.name.toLowerCase() === trimmed.toLowerCase())
                    setSlot.mutate({
                      dayOfWeek: day, mealType: meal,
                      text: match ? null : trimmed, recipeId: match?.id ?? null,
                    })
                  }
                  return (
                    <td key={day} onClick={() => {
                      if (isEditing) return
                      setEditing({ day, meal })
                      setValue(slot?.recipeId ? (recipeName(slot.recipeId) ?? '') : (slot?.text ?? ''))
                    }}>
                      {isEditing ? (
                        <form onSubmit={(e) => { e.preventDefault(); save() }}>
                          <input list="menu-recipes" value={value} onChange={(e) => setValue(e.target.value)}
                                 onKeyDown={(e) => { if (e.key === 'Escape') setEditing(null) }}
                                 autoFocus placeholder={t('menu.slotPlaceholder')} onBlur={save} />
                        </form>
                      ) : (recipeName(slot?.recipeId) || slot?.text) ? (
                        <>
                          {recipeName(slot?.recipeId) && <strong>{recipeName(slot?.recipeId)}</strong>}
                          {slot?.text && <span className="slot-note">{slot.text}</span>}
                        </>
                      ) : <span className="empty-slot">·</span>}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
