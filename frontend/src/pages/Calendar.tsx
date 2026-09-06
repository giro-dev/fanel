import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'

type CalEvent = { id: string; title: string; date: string; time?: string; addedBy?: string }

export function Calendar() {
  const { t, i18n } = useTranslation()
  const { household, member, members } = useHousehold()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [date, setDate] = useState('')
  const [time, setTime] = useState('')

  const events = useQuery({
    queryKey: ['calendar', household?.id],
    queryFn: () => api<CalEvent[]>(`/households/${household!.id}/calendar`),
    enabled: !!household,
  })

  const create = useMutation({
    mutationFn: () => api<CalEvent>(`/households/${household!.id}/calendar`, {
      method: 'POST',
      body: JSON.stringify({ title, date, time: time || null, addedBy: member?.id ?? null }),
    }),
    onSuccess: async () => {
      setTitle(''); setDate(''); setTime('')
      await queryClient.invalidateQueries({ queryKey: ['calendar'] })
    },
  })
  const remove = useMutation({
    mutationFn: (id: string) => api<void>(`/households/${household!.id}/calendar/${id}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['calendar'] }),
  })

  const memberName = (id?: string) => members.find((m) => m.id === id)?.name
  const grouped = new Map<string, CalEvent[]>()
  for (const e of events.data ?? []) {
    const list = grouped.get(e.date) ?? []
    list.push(e)
    grouped.set(e.date, list)
  }
  const formatDay = (iso: string) =>
    new Intl.DateTimeFormat(i18n.language, { weekday: 'long', day: 'numeric', month: 'long' })
      .format(new Date(`${iso}T00:00:00`))

  return (
    <section className="panel">
      <h2>{t('calendar.title')}</h2>
      <form className="create-form" onSubmit={(e) => { e.preventDefault(); if (title && date) create.mutate() }}>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder={t('calendar.titlePlaceholder')} required />
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        <input type="time" value={time} onChange={(e) => setTime(e.target.value)} />
        <button type="submit" disabled={create.isPending}>{t('calendar.add')}</button>
      </form>
      {events.isPending && <p>{t('loading')}</p>}
      {events.isError && <p role="alert">{t('error')}</p>}
      {[...grouped.entries()].map(([day, dayEvents]) => (
        <div key={day} className="day-group">
          <h3>{formatDay(day)}</h3>
          <ul className="item-list">
            {dayEvents.map((e) => (
              <li key={e.id}>
                <span>{e.time?.slice(0, 5)} {e.title}</span>
                <span className="meta">
                  {e.addedBy && <em>{memberName(e.addedBy)}</em>}
                  <button type="button" className="link" onClick={() => remove.mutate(e.id)}>✕</button>
                </span>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  )
}
