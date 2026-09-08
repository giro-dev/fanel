import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useHousehold } from '../context/HouseholdContext'

export function Account() {
  const { t } = useTranslation()
  const { username, logout } = useAuth()
  const { member } = useHousehold()
  const queryClient = useQueryClient()
  const [newUsername, setNewUsername] = useState('')
  const [password, setPassword] = useState('')
  const [saved, setSaved] = useState(false)

  const mutation = useMutation({
    mutationFn: () => api(`/households/${member!.householdId}/members/${member!.id}/credentials`, {
      method: 'PUT',
      body: JSON.stringify({ username: newUsername, password }),
    }),
    onSuccess: async () => {
      setSaved(true)
      setPassword('')
      await queryClient.invalidateQueries({ queryKey: ['members'] })
    },
  })

  return (
    <section className="panel">
      <h2>{t('account.title')}</h2>
      <p>{t('account.loggedInAs', { username })}</p>
      <button type="button" className="link" onClick={logout}>{t('account.logout')}</button>

      {member?.role === 'ADULT' ? (
        <form className="create-form" onSubmit={(event) => { event.preventDefault(); setSaved(false); mutation.mutate() }}>
          <p>{t('account.setCredentialsFor', { name: member.name })}</p>
          <label>
            {t('account.username')}
            <input value={newUsername} onChange={(event) => setNewUsername(event.target.value)} required />
          </label>
          <label>
            {t('account.password')}
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)}
                   required minLength={8} />
          </label>
          <button type="submit" disabled={mutation.isPending}>{t('account.save')}</button>
          {mutation.isError && <p role="alert">{t('error')}</p>}
          {saved && <p>{t('account.saved')}</p>}
        </form>
      ) : (
        <p>{t('account.selectAdult')}</p>
      )}
    </section>
  )
}
