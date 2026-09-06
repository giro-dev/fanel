import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'

type Item = { id: string; name: string; done: boolean }
type ShoppingList = { id: string; name: string; items: Item[] }

export function Shopping() {
  const { t } = useTranslation()
  const { household } = useHousehold()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')

  const list = useQuery({
    queryKey: ['shopping', household?.id],
    queryFn: () => api<ShoppingList>(`/households/${household!.id}/shopping/lists/default`),
    enabled: !!household,
  })

  const invalidate = { onSuccess: () => queryClient.invalidateQueries({ queryKey: ['shopping'] }) }

  const add = useMutation({
    mutationFn: () => api<Item>(`/households/${household!.id}/shopping/lists/${list.data!.id}/items`, {
      method: 'POST', body: JSON.stringify({ name }),
    }),
    onSuccess: async () => { setName(''); await queryClient.invalidateQueries({ queryKey: ['shopping'] }) },
  })
  const toggle = useMutation({
    mutationFn: (item: Item) => api<Item>(`/households/${household!.id}/shopping/items/${item.id}`, {
      method: 'PATCH', body: JSON.stringify({ done: !item.done }),
    }),
    ...invalidate,
  })
  const remove = useMutation({
    mutationFn: (item: Item) => api<void>(`/households/${household!.id}/shopping/items/${item.id}`, { method: 'DELETE' }),
    ...invalidate,
  })
  const clearPurchased = useMutation({
    mutationFn: () => api<{ removed: number }>(`/households/${household!.id}/shopping/lists/${list.data!.id}/clear-purchased`, { method: 'POST' }),
    ...invalidate,
  })

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>{t('shopping.title')}</h2>
        <button type="button" onClick={() => clearPurchased.mutate()} disabled={!list.data?.items.some((i) => i.done)}>
          {t('shopping.clearPurchased')}
        </button>
      </div>
      <form className="create-form" onSubmit={(e) => { e.preventDefault(); if (name.trim()) add.mutate() }}>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder={t('shopping.addPlaceholder')} />
        <button type="submit" disabled={add.isPending}>{t('shopping.add')}</button>
      </form>
      {list.isPending && <p>{t('loading')}</p>}
      {list.isError && <p role="alert">{t('error')}</p>}
      <ul className="item-list">
        {list.data?.items.map((item) => (
          <li key={item.id} className={item.done ? 'done' : ''}>
            <label className="inline">
              <input type="checkbox" checked={item.done} onChange={() => toggle.mutate(item)} />
              <span>{item.name}</span>
            </label>
            <button type="button" className="link" onClick={() => remove.mutate(item)}>✕</button>
          </li>
        ))}
      </ul>
    </section>
  )
}
