import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useTheme, type Theme } from '../hooks/useTheme'

const stroke = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' } as const

const icons: Record<Theme, ReactNode> = {
  light: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <circle cx="12" cy="12" r="4.5" />
      <path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5 5l1.8 1.8M17.2 17.2 19 19M19 5l-1.8 1.8M6.8 17.2 5 19" />
    </svg>
  ),
  dark: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z" />
    </svg>
  ),
  system: (
    <svg viewBox="0 0 24 24" width="1.15rem" height="1.15rem" {...stroke}>
      <rect x="3" y="4" width="18" height="12.5" rx="2" />
      <path d="M9 20.5h6M12 16.5v4" />
    </svg>
  ),
}

export function ThemeToggle() {
  const { t } = useTranslation()
  const { theme, cycle } = useTheme()
  return (
    <button type="button" className="theme-toggle" onClick={cycle}
            title={t(`theme.${theme}`)} aria-label={t('theme.label')}>
      {icons[theme]}
    </button>
  )
}
