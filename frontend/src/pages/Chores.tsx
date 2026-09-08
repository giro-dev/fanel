import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'
import { Modal } from '../components/Modal'

type RecurrenceFreq = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

type Chore = {
  id: string
  title: string
  assigneeId?: string
  done: boolean
  dueDate?: string | null
  recurrenceFreq?: RecurrenceFreq | null
  recurrenceInterval?: number | null
  rotationMemberIds: string[]
}

type RecurrenceForm = {
  choreId: string
  dueDate: string
  recurrenceFreq: RecurrenceFreq | ''
  recurrenceInterval: number
  rotationMemberIds: string[]
}

const REPEAT_OPTIONS: RecurrenceFreq[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']

export function Chores() {
  const { t } = useTranslation()
  const { household, members } = useHousehold()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [assignee, setAssignee] = useState('')
  const [recurrenceForm, setRecurrenceForm] = useState<RecurrenceForm | null>(null)

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
  const updateRecurrence = useMutation({
    mutationFn: (data: RecurrenceForm) =>
      api<Chore>(`/households/${household!.id}/chores/${data.choreId}/recurrence`, {
        method: 'PUT',
        body: JSON.stringify({
          dueDate: data.dueDate || null,
          recurrenceFreq: data.recurrenceFreq || null,
          recurrenceInterval: data.recurrenceFreq ? data.recurrenceInterval : null,
          rotationMemberIds: data.recurrenceFreq ? data.rotationMemberIds : [],
        }),
      }),
    onSuccess: async () => { setRecurrenceForm(null); await queryClient.invalidateQueries({ queryKey: ['chores'] }) },
  })
  const remove = useMutation({
    mutationFn: (id: string) => api<void>(`/households/${household!.id}/chores/${id}`, { method: 'DELETE' }),
    ...invalidate,
  })

  const memberOf = (id?: string) => members.find((m) => m.id === id)

  const openRecurrence = (chore: Chore) => setRecurrenceForm({
    choreId: chore.id,
    dueDate: chore.dueDate ?? '',
    recurrenceFreq: chore.recurrenceFreq ?? '',
    recurrenceInterval: chore.recurrenceInterval ?? 1,
    rotationMemberIds: chore.rotationMemberIds,
  })

  const toggleRotationMember = (id: string) => {
    setRecurrenceForm((current) => current && ({
      ...current,
      rotationMemberIds: current.rotationMemberIds.includes(id)
        ? current.rotationMemberIds.filter((m) => m !== id)
        : [...current.rotationMemberIds, id],
    }))
  }

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
                {chore.recurrenceFreq && (
                  <em className="meta" title={t(`chores.repeatOptions.${chore.recurrenceFreq}`)}>↻</em>
                )}
                {assigneeMember && (
                  <em className="member-chip small" style={{ background: assigneeMember.color ?? 'var(--tile)' }}>
                    {assigneeMember.name}
                  </em>
                )}
                <button type="button" className={`icon-btn ${chore.recurrenceFreq ? 'active' : ''}`}
                        title={t('chores.recurrence')} onClick={() => openRecurrence(chore)}>
                  🔁
                </button>
                <button type="button" className="link" onClick={() => remove.mutate(chore.id)}>✕</button>
              </span>
            </li>
          )
        })}
      </ul>

      {recurrenceForm && (
        <Modal title={t('chores.recurrence')} onClose={() => setRecurrenceForm(null)}>
          <form className="create-form modal-form" onSubmit={(e) => { e.preventDefault(); updateRecurrence.mutate(recurrenceForm) }}>
            <label>
              {t('chores.dueDate')}
              <input type="date" value={recurrenceForm.dueDate}
                     onChange={(e) => setRecurrenceForm({ ...recurrenceForm, dueDate: e.target.value })} />
            </label>
            <label>
              {t('chores.repeat')}
              <select value={recurrenceForm.recurrenceFreq}
                      onChange={(e) => setRecurrenceForm({ ...recurrenceForm, recurrenceFreq: e.target.value as RecurrenceFreq | '' })}>
                <option value="">{t('chores.repeatOptions.none')}</option>
                {REPEAT_OPTIONS.map((freq) => (
                  <option key={freq} value={freq}>{t(`chores.repeatOptions.${freq}`)}</option>
                ))}
              </select>
            </label>
            {recurrenceForm.recurrenceFreq && (
              <>
                <label className="inline">
                  {t('chores.every')}
                  <input type="number" min={1} value={recurrenceForm.recurrenceInterval}
                         onChange={(e) => setRecurrenceForm({
                           ...recurrenceForm, recurrenceInterval: Math.max(1, Number(e.target.value)),
                         })} />
                  {t(`chores.repeatUnit.${recurrenceForm.recurrenceFreq}`)}
                </label>
                <fieldset>
                  <legend>{t('chores.rotation')}</legend>
                  <p className="meta">{t('chores.rotationHint')}</p>
                  {members.map((m) => {
                    const position = recurrenceForm.rotationMemberIds.indexOf(m.id)
                    return (
                      <label className="inline" key={m.id}>
                        <input type="checkbox" checked={position !== -1} onChange={() => toggleRotationMember(m.id)} />
                        <em className="member-chip small" style={{ background: m.color ?? 'var(--tile)' }}>
                          {position !== -1 ? `${position + 1}. ` : ''}{m.name}
                        </em>
                      </label>
                    )
                  })}
                </fieldset>
              </>
            )}
            <div className="footer-actions">
              <button type="submit" disabled={updateRecurrence.isPending}>{t('chores.save')}</button>
            </div>
          </form>
        </Modal>
      )}
    </section>
  )
}
