import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'

type Chore = { id: string; title: string; assigneeId?: string; done: boolean }

export function Chores() {
  const { t } = useTranslation()
  const { household, members } = useHousehold()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [assignee, setAssignee] = useState('')

  const chores = useQuery({
    queryKey: ['chores', household?.id],
    queryFn: () => api<Chore[]>(`/households/${household!.id}/chores`),
    enabled: !!household,
  })
  const invalidate = { onSuccess: () => queryClient.invalidateQueries({ queryKey: ['chores'] }) }

  const create = useMutation({
    mutationFn: () => api<Chore>(`/households/${household!.id}/chores`, {
      method: 'POST', body: JSON.stringify({ title, assigneeId: assignee || null }),
    }),
    onSuccess: async () => { setTitle(''); await queryClient.invalidateQueries({ queryKey: ['chores'] }) },
  })
  const update = useMutation({
    mutationFn: (chore: Partial<Chore> & { id: string }) =>
      api<Chore>(`/households/${household!.id}/chores/${chore.id}`, {
        method: 'PATCH', body: JSON.stringify(chore),
      }),
    ...invalidate,
  })
  const remove = useMutation({
    mutationFn: (id: string) => api<void>(`/households/${household!.id}/chores/${id}`, { method: 'DELETE' }),
    ...invalidate,
  })

  const memberOf = (id?: string) => members.find((m) => m.id === id)

  return (
    <section className="panel">
      <h2>{t('chores.title')}</h2>
      <form className="create-form" onSubmit={(e) => { e.preventDefault(); if (title.trim()) create.mutate() }}>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder={t('chores.titlePlaceholder')} required />
        <select value={assignee} onChange={(e) => setAssignee(e.target.value)}>
          <option value="">{t('chores.unassigned')}</option>
          {members.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
        </select>
        <button type="submit" disabled={create.isPending}>{t('chores.add')}</button>
      </form>
      {chores.isPending && <p>{t('loading')}</p>}
      {chores.isError && <p role="alert">{t('error')}</p>}
      <ul className="item-list">
        {chores.data?.map((chore) => {
          const assigneeMember = memberOf(chore.assigneeId)
          return (
            <li key={chore.id} className={chore.done ? 'done' : ''}>
              <label className="inline">
                <input type="checkbox" checked={chore.done}
                       onChange={() => update.mutate({ id: chore.id, done: !chore.done })} />
                <span>{chore.title}</span>
              </label>
              <span className="meta">
                {assigneeMember && (
                  <em className="member-chip small" style={{ background: assigneeMember.color ?? 'var(--tile)' }}>
                    {assigneeMember.name}
                  </em>
                )}
                <button type="button" className="link" onClick={() => remove.mutate(chore.id)}>✕</button>
              </span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
