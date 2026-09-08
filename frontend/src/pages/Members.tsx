import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold, type Member } from '../context/HouseholdContext'

const ROLES = ['ADMIN', 'ADULT', 'CHILD'] as const
const DEFAULT_COLOR = '#2c6e8e'

export function Members() {
  const { t } = useTranslation()
  const { household, members, member } = useHousehold()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [role, setRole] = useState<Member['role']>('CHILD')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editName, setEditName] = useState('')
  const [editColor, setEditColor] = useState(DEFAULT_COLOR)

  // The global bootstrap admin has no linked member; once someone picks "who am I" we defer to their role.
  const isAdmin = !member || member.role === 'ADMIN'
  const canManage = (target: Member) =>
    isAdmin || target.id === member?.id || (member?.role === 'ADULT' && (target.guardianIds ?? []).includes(member.id))
  const invalidate = { onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members'] }) }

  const create = useMutation({
    mutationFn: () => api<Member>(`/households/${household!.id}/members`, {
      method: 'POST',
      body: JSON.stringify({ name, role }),
    }),
    onSuccess: async () => { setName(''); await queryClient.invalidateQueries({ queryKey: ['members'] }) },
  })

  const changeRole = useMutation({
    mutationFn: ({ memberId, role }: { memberId: string; role: string }) =>
      api(`/households/${household!.id}/members/${memberId}/role`, { method: 'PUT', body: JSON.stringify({ role }) }),
    ...invalidate,
  })

  const setGuardians = useMutation({
    mutationFn: ({ childId, guardianIds }: { childId: string; guardianIds: string[] }) =>
      api(`/households/${household!.id}/members/${childId}/guardians`, {
        method: 'PUT',
        body: JSON.stringify({ guardianIds }),
      }),
    ...invalidate,
  })

  const update = useMutation({
    mutationFn: ({ memberId, name, color }: { memberId: string; name: string; color: string }) =>
      api(`/households/${household!.id}/members/${memberId}`, {
        method: 'PATCH',
        body: JSON.stringify({ name, color }),
      }),
    onSuccess: async () => { setEditingId(null); await queryClient.invalidateQueries({ queryKey: ['members'] }) },
  })

  const remove = useMutation({
    mutationFn: (memberId: string) => api<void>(`/households/${household!.id}/members/${memberId}`, { method: 'DELETE' }),
    ...invalidate,
  })

  const startEdit = (m: Member) => {
    setEditingId(m.id)
    setEditName(m.name)
    setEditColor(m.color && /^#[0-9a-fA-F]{6}$/.test(m.color) ? m.color : DEFAULT_COLOR)
  }

  const potentialGuardians = members.filter((m) => m.role !== 'CHILD')

  return (
    <section className="panel">
      <h2>{t('members.title')}</h2>

      {isAdmin && (
        <form className="create-form" onSubmit={(event) => { event.preventDefault(); if (name.trim()) create.mutate() }}>
          <label>
            {t('members.name')}
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label>
            {t('members.role')}
            <select value={role} onChange={(event) => setRole(event.target.value as Member['role'])}>
              {ROLES.map((r) => <option key={r} value={r}>{t(`members.roles.${r}`)}</option>)}
            </select>
          </label>
          <button type="submit" disabled={create.isPending}>{t('members.add')}</button>
        </form>
      )}
      {create.isError && <p role="alert">{t('error')}</p>}

      <ul className="member-list">
        {members.map((m) => (
          <li key={m.id}>
            {editingId === m.id ? (
              <form className="create-form" onSubmit={(event) => {
                event.preventDefault()
                update.mutate({ memberId: m.id, name: editName, color: editColor })
              }}>
                <label>
                  {t('members.name')}
                  <input value={editName} onChange={(event) => setEditName(event.target.value)} required />
                </label>
                <label>
                  {t('members.color')}
                  <input type="color" value={editColor} onChange={(event) => setEditColor(event.target.value)} />
                </label>
                <button type="submit" disabled={update.isPending}>{t('members.save')}</button>
                <button type="button" className="link" onClick={() => setEditingId(null)}>{t('members.cancel')}</button>
              </form>
            ) : (
              <div className="footer-actions">
                <span className="member-chip" style={{ background: m.color ?? 'var(--tile)' }}>{m.name}</span>
                {isAdmin ? (
                  <select value={m.role} onChange={(event) => changeRole.mutate({ memberId: m.id, role: event.target.value })}>
                    {ROLES.map((r) => <option key={r} value={r}>{t(`members.roles.${r}`)}</option>)}
                  </select>
                ) : (
                  <span>{t(`members.roles.${m.role}`)}</span>
                )}
                {canManage(m) && (
                  <>
                    <button type="button" className="link" onClick={() => startEdit(m)}>{t('members.edit')}</button>
                    <button type="button" className="link" onClick={() => {
                      if (window.confirm(t('members.confirmDelete', { name: m.name }))) remove.mutate(m.id)
                    }}>{t('members.delete')}</button>
                  </>
                )}
              </div>
            )}
            {isAdmin && m.role === 'CHILD' && (
              <fieldset>
                <legend>{t('members.guardians')}</legend>
                {potentialGuardians.length === 0 && <p>{t('members.noGuardianCandidates')}</p>}
                {potentialGuardians.map((guardian) => {
                  const checked = m.guardianIds?.includes(guardian.id) ?? false
                  return (
                    <label className="inline" key={guardian.id}>
                      <input type="checkbox" checked={checked} onChange={(event) => {
                        const next = event.target.checked
                          ? [...(m.guardianIds ?? []), guardian.id]
                          : (m.guardianIds ?? []).filter((id) => id !== guardian.id)
                        setGuardians.mutate({ childId: m.id, guardianIds: next })
                      }} />
                      {guardian.name}
                    </label>
                  )
                })}
              </fieldset>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
