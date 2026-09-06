import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useHousehold, type Member } from '../context/HouseholdContext'

export function WhoAmI() {
  const { t } = useTranslation()
  const { member, members, selectMember, clearMember } = useHousehold()
  const [pending, setPending] = useState<Member | null>(null)
  const [pin, setPin] = useState('')
  const [error, setError] = useState(false)

  const choose = async (candidate: Member, candidatePin?: string) => {
    const ok = await selectMember(candidate, candidatePin)
    if (ok) {
      setPending(null)
      setPin('')
      setError(false)
    } else {
      setPending(candidate)
      setError(candidatePin !== undefined)
    }
  }

  if (member) {
    return (
      <div className="whoami">
        <span className="member-chip" style={{ background: member.color ?? 'var(--tile)' }}>{member.name}</span>
        <button type="button" className="link" onClick={clearMember}>{t('whoami.switch')}</button>
      </div>
    )
  }

  return (
    <div className="whoami">
      <span>{t('whoami.prompt')}</span>
      <div className="member-picker">
        {members.map((m) => (
          <button key={m.id} type="button" className="member-chip"
                  style={{ background: m.color ?? 'var(--tile)' }}
                  onClick={() => void choose(m)}>
            {m.name}
          </button>
        ))}
      </div>
      {pending && (
        <form className="pin-form" onSubmit={(e) => { e.preventDefault(); void choose(pending, pin) }}>
          <label>
            {t('whoami.pin', { name: pending.name })}
            <input type="password" inputMode="numeric" value={pin}
                   onChange={(e) => setPin(e.target.value)} autoFocus />
          </label>
          <button type="submit">{t('whoami.confirm')}</button>
          {error && <p role="alert" className="error">{t('whoami.wrongPin')}</p>}
        </form>
      )}
    </div>
  )
}
