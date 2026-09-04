import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import ca from './ca.json'
import es from './es.json'

void i18n.use(initReactI18next).init({
  resources: { ca: { translation: ca }, es: { translation: es } },
  lng: navigator.language.toLowerCase().startsWith('es') ? 'es' : 'ca',
  fallbackLng: 'ca',
  interpolation: { escapeValue: false },
})

export default i18n
