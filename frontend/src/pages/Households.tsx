import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'

type Household = { id: string; name: string; locale: string; timezone: string }

export function Households() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const households = useQuery({ queryKey: ['households'], queryFn: () => api<Household[]>('/households') })
  const create = useMutation({
    mutationFn: () => api<Household>('/households', {
      method: 'POST',
      body: JSON.stringify({ name, locale: 'ca', timezone: 'Europe/Madrid' }),
    }),
    onSuccess: async () => { setName(''); await queryClient.invalidateQueries({ queryKey: ['households'] }) },
  })

  return (
    <section className="panel">
      <h2>{t('households.title')}</h2>
      <form onSubmit={(event) => { event.preventDefault(); create.mutate() }} className="create-form">
        <label>
          {t('households.name')}
          <input value={name} onChange={(event) => setName(event.target.value)} required />
        </label>
        <button type="submit" disabled={create.isPending}>{t('households.create')}</button>
      </form>
      {households.isPending && <p>{t('loading')}</p>}
      {households.isError && <p role="alert">{t('error')}</p>}
      {households.data?.length === 0 && <p>{t('households.empty')}</p>}
      <ul className="household-list">
        {households.data?.map((household) => <li key={household.id}><strong>{household.name}</strong><span>{household.timezone}</span></li>)}
      </ul>
    </section>
  )
}
