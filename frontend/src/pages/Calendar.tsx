import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import listPlugin from '@fullcalendar/list'
import interactionPlugin from '@fullcalendar/interaction'
import type { DateClickArg } from '@fullcalendar/interaction'
import type { EventClickArg } from '@fullcalendar/core'
import caLocale from '@fullcalendar/core/locales/ca'
import esLocale from '@fullcalendar/core/locales/es'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'

type CalEvent = { id: string; title: string; date: string; time?: string; addedBy?: string }

const LOCALES = { ca: caLocale, es: esLocale }

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

  const calendarEvents = (events.data ?? []).map((e) => ({
    id: e.id,
    title: e.addedBy ? `${e.title} (${memberName(e.addedBy)})` : e.title,
    start: e.time ? `${e.date}T${e.time}` : e.date,
    allDay: !e.time,
  }))

  const onDateClick = (arg: DateClickArg) => setDate(arg.dateStr)
  const onEventClick = (arg: EventClickArg) => {
    if (window.confirm(t('calendar.confirmDelete', { title: arg.event.title }))) {
      remove.mutate(arg.event.id)
    }
  }

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
      <div className="calendar-view">
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
          initialView="dayGridMonth"
          headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,listWeek' }}
          locale={LOCALES[i18n.language as keyof typeof LOCALES] ?? LOCALES.ca}
          height="auto"
          events={calendarEvents}
          dateClick={onDateClick}
          eventClick={onEventClick}
        />
      </div>
    </section>
  )
}
