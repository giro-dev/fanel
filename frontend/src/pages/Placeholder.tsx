import { useTranslation } from 'react-i18next'

export function Placeholder({ title }: { title: string }) {
  const { t } = useTranslation()
  return <section className="panel"><h2>{title}</h2><p>{t('placeholder.soon')}</p></section>
}
