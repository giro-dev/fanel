import { useState } from 'react'
import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useHousehold } from '../household/HouseholdContext'
import { useAddShoppingItem, useClearDoneShopping, useDeleteShoppingItem, useShoppingList, useUpdateShoppingItem } from '../api/shopping'

export function Shopping() {
  const { t } = useTranslation()
  const { current } = useHousehold()
  const [text, setText] = useState('')
  const list = useShoppingList(current?.id)
  const add = useAddShoppingItem(current?.id ?? '')
  const update = useUpdateShoppingItem(current?.id ?? '')
  const remove = useDeleteShoppingItem(current?.id ?? '')
  const clear = useClearDoneShopping(current?.id ?? '')

  if (!current) return <section className="panel empty-state"><p>{t('households.select')}</p><Link to="/households">{t('households.title')}</Link></section>
  const items = list.data ?? []
  const doneCount = items.filter((item) => item.done).length
  return <section className="panel shopping-panel">
    <div className="page-heading"><h2>{t('nav.shopping')}</h2><button type="button" disabled={!doneCount || clear.isPending} onClick={() => clear.mutate()}>{t('shopping.clearDone')}</button></div>
    <form className="shopping-form" onSubmit={(event) => { event.preventDefault(); if (text.trim()) { add.mutate(text.trim()); setText('') } }}>
      <input value={text} onChange={(event) => setText(event.target.value)} placeholder={t('shopping.placeholder')} aria-label={t('shopping.item')} />
      <button type="submit" disabled={!text.trim() || add.isPending}>{t('shopping.add')}</button>
    </form>
    {list.isPending && <p>{t('loading')}</p>}
    {list.isError && <p role="alert">{t('error')}</p>}
    <ul className="shopping-list">
      {[...items].sort((a, b) => Number(a.done) - Number(b.done)).map((item) => <li className={item.done ? 'done' : ''} key={item.id}>
        <label><input type="checkbox" checked={item.done} onChange={() => update.mutate({ id: item.id, done: !item.done })} /><span>{item.text}</span></label>
        <button className="delete-button" type="button" aria-label={t('shopping.delete')} onClick={() => remove.mutate(item.id)}>×</button>
      </li>)}
    </ul>
    {!list.isPending && items.length === 0 && <p>{t('shopping.empty')}</p>}
  </section>
}
