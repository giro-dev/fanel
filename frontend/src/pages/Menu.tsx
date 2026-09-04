import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useHousehold } from '../household/HouseholdContext'
import { useMenuWeek, useSetMeal, type MealSlot, type MealType } from '../api/menu'
import { addDays, dateKey, isoWeek, isoWeekKey, startOfIsoWeek } from '../lib/isoWeek'
import { Link } from 'react-router'

const meals: MealType[] = ['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER']
const mealKeys: Record<MealType, string> = {
  BREAKFAST: 'breakfast', LUNCH: 'lunch', SNACK: 'snack', DINNER: 'dinner',
}
const dayKeys = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun']

function MealCell({ slot, date, meal, save }: {
  slot?: MealSlot
  date: string
  meal: MealType
  save: (date: string, meal: MealType, text: string) => void
}) {
  const [text, setText] = useState(slot?.text ?? '')
  const value = slot?.text ?? ''
  useEffect(() => setText(value), [value])
  return <input
    aria-label={`${date} ${meal}`}
    value={text}
    onChange={(event) => setText(event.target.value)}
    onBlur={() => save(date, meal, text)}
    onKeyDown={(event) => { if (event.key === 'Enter') event.currentTarget.blur() }}
    maxLength={500}
  />
}

export function Menu() {
  const { t } = useTranslation()
  const { current } = useHousehold()
  const [anchor, setAnchor] = useState(new Date())
  const currentWeek = isoWeek(anchor)
  const weekKey = isoWeekKey(anchor)
  const menu = useMenuWeek(current?.id, weekKey)
  const save = useSetMeal(current?.id ?? '', weekKey)
  const slots = useMemo(() => new Map(menu.data?.slots.map((slot) => [`${slot.date}-${slot.meal}`, slot])), [menu.data])
  const today = dateKey(new Date())
  const days = Array.from({ length: 7 }, (_, index) => addDays(currentWeek.from, index))

  if (!current) return <section className="panel empty-state"><p>{t('households.select')}</p><Link to="/households">{t('households.title')}</Link></section>

  return <section className="panel menu-panel">
    <div className="page-heading">
      <h2>{t('nav.menu')}</h2>
      <div className="week-controls">
        <button type="button" onClick={() => setAnchor(startOfIsoWeek(addDays(anchor, -7)))}>{t('menu.previous')}</button>
        <button type="button" onClick={() => setAnchor(new Date())}>{t('menu.today')}</button>
        <button type="button" onClick={() => setAnchor(startOfIsoWeek(addDays(anchor, 7)))}>{t('menu.next')}</button>
      </div>
    </div>
    <p className="week-label">{t('menu.week', { week: currentWeek.week, from: currentWeek.from.getDate(), to: currentWeek.to.getDate(), month: t(`months.${currentWeek.to.getMonth() + 1}`) })}</p>
    {menu.isPending && <p>{t('loading')}</p>}
    {menu.isError && <p role="alert">{t('error')}</p>}
    <div className="menu-grid">
      <div className="meal-label" />
      {days.map((day, index) => <div key={dateKey(day)} className={`day-heading ${dateKey(day) === today ? 'today' : ''}`}><span>{t(`days.${dayKeys[index]}`)}</span><strong>{day.getDate()}</strong></div>)}
      {meals.map((meal) => <div className="menu-row" key={meal}>
        <div className="meal-label">{t(`meals.${mealKeys[meal]}`)}</div>
        {days.map((day) => {
          const date = dateKey(day)
          return <div key={date} className={`meal-cell ${date === today ? 'today' : ''}`}>
            <MealCell slot={slots.get(`${date}-${meal}`)} date={date} meal={meal} save={(savedDate, savedMeal, text) => save.mutate({ date: savedDate, meal: savedMeal, text })} />
          </div>
        })}
      </div>)}
    </div>
    <div className="mobile-menu">
      {days.map((day, index) => {
        const date = dateKey(day)
        return <div className={`mobile-day ${date === today ? 'today' : ''}`} key={date}>
          <div className="day-heading"><span>{t(`days.${dayKeys[index]}`)}</span><strong>{day.getDate()}</strong></div>
          {meals.map((meal) => <div className="mobile-meal" key={meal}>
            <span className="meal-label">{t(`meals.${mealKeys[meal]}`)}</span>
            <MealCell slot={slots.get(`${date}-${meal}`)} date={date} meal={meal} save={(savedDate, savedMeal, text) => save.mutate({ date: savedDate, meal: savedMeal, text })} />
          </div>)}
        </div>
      })}
    </div>
  </section>
}
