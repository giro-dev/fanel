const dayMilliseconds = 24 * 60 * 60 * 1000

function dateOnly(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

export function addDays(date: Date, days: number) {
  return new Date(date.getTime() + days * dayMilliseconds)
}

export function startOfIsoWeek(date: Date) {
  const value = dateOnly(date)
  const day = value.getDay() || 7
  return addDays(value, 1 - day)
}

export function isoWeek(date: Date) {
  const value = dateOnly(date)
  const thursday = addDays(value, 4 - (value.getDay() || 7))
  const year = thursday.getFullYear()
  const firstThursday = new Date(year, 0, 4)
  const week = Math.floor((thursday.getTime() - startOfIsoWeek(firstThursday).getTime()) / (7 * dayMilliseconds)) + 1
  const from = startOfIsoWeek(value)
  return { year, week, from, to: addDays(from, 6) }
}

export function isoWeekKey(date: Date) {
  const value = isoWeek(date)
  return `${value.year}-W${String(value.week).padStart(2, '0')}`
}

export function dateKey(date: Date) {
  return date.toISOString().slice(0, 10)
}
