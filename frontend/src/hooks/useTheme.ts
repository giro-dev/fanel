import { useEffect, useState } from 'react'

export type Theme = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'fanel-theme'
const ORDER: Theme[] = ['system', 'light', 'dark']

function storedTheme(): Theme {
  const value = localStorage.getItem(STORAGE_KEY)
  return value === 'light' || value === 'dark' ? value : 'system'
}

function systemDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(storedTheme)
  const [dark, setDark] = useState(() => {
    const t = storedTheme()
    return t === 'dark' || (t === 'system' && systemDark())
  })

  useEffect(() => {
    const root = document.documentElement
    if (theme === 'system') {
      delete root.dataset.theme
      localStorage.removeItem(STORAGE_KEY)
    } else {
      root.dataset.theme = theme
      localStorage.setItem(STORAGE_KEY, theme)
    }
    setDark(theme === 'dark' || (theme === 'system' && systemDark()))
  }, [theme])

  useEffect(() => {
    if (theme !== 'system') return
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => setDark(media.matches)
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [theme])

  useEffect(() => {
    const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
    if (meta) meta.content = dark ? '#10161a' : '#f4f2ec'
  }, [dark])

  const cycle = () => setTheme(ORDER[(ORDER.indexOf(theme) + 1) % ORDER.length])

  return { theme, cycle }
}
