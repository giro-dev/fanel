import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { client, unwrap } from '../api/typed'
import type { WireGuardPairingToken, WireGuardPeer, WireGuardStatus } from '../api/types'

function relativeTime(value: string | null | undefined, now: number, locale: string, fallback: string): string {
  if (!value) return fallback
  const seconds = Math.round((Date.parse(value) - now) / 1000)
  const units: [Intl.RelativeTimeFormatUnit, number][] = [
    ['year', 60 * 60 * 24 * 365],
    ['month', 60 * 60 * 24 * 30],
    ['day', 60 * 60 * 24],
    ['hour', 60 * 60],
    ['minute', 60],
    ['second', 1],
  ]
  const defaultUnit: [Intl.RelativeTimeFormatUnit, number] = ['second', 1]
  const [unit, duration] = units.find(([, size]) => Math.abs(seconds) >= size) ?? defaultUnit
  return new Intl.RelativeTimeFormat(locale, { numeric: 'auto' }).format(Math.round(seconds / duration), unit)
}

export function WireGuard() {
  const { t, i18n } = useTranslation()
  const queryClient = useQueryClient()
  const [pairing, setPairing] = useState<WireGuardPairingToken | null>(null)
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    if (!pairing) return
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [pairing])

  const status = useQuery({
    queryKey: ['wireguard', 'status'],
    queryFn: () => unwrap(client.GET('/api/v1/admin/wg/status')).then((result) => result as WireGuardStatus),
  })
  const peers = useQuery({
    queryKey: ['wireguard', 'peers'],
    queryFn: () => unwrap(client.GET('/api/v1/admin/wg/peers')).then((result) => result as WireGuardPeer[]),
    enabled: status.data?.enabled === true,
  })
  const createToken = useMutation({
    mutationFn: () => unwrap(client.POST('/api/v1/admin/wg/pairing-tokens'))
      .then((result) => result as WireGuardPairingToken),
    onSuccess: (response) => setPairing(response),
  })
  const revoke = useMutation({
    mutationFn: (id: string) => unwrap(client.DELETE('/api/v1/admin/wg/peers/{id}', {
      params: { path: { id } },
    })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['wireguard', 'peers'] }),
  })

  const expiresIn = pairing
    ? Math.max(0, Math.ceil((Date.parse(pairing.expiresAt) - now) / 1000))
    : 0
  const countdown = expiresIn > 0
    ? `${Math.floor(expiresIn / 60)}:${String(expiresIn % 60).padStart(2, '0')}`
    : t('wireguard.expired')

  return (
    <section className="panel wireguard-panel">
      <h1>{t('wireguard.title')}</h1>
      {status.isLoading && <p>{t('loading')}</p>}
      {status.isError && <p role="alert">{t('error')}</p>}
      {status.data && !status.data.enabled && (
        <div className="wireguard-disabled">
          <p>{t('wireguard.disabled')}</p>
          <a href="https://github.com/giro-dev/fanel/blob/main/docs/wireguard.md" target="_blank" rel="noreferrer">
            {t('wireguard.docs')}
          </a>
        </div>
      )}
      {status.data?.enabled && (
        <>
          <dl className="wireguard-status">
            <div><dt>{t('wireguard.interface')}</dt><dd>{status.data.interfaceName}</dd></div>
            <div><dt>{t('wireguard.network')}</dt><dd>{status.data.network}</dd></div>
            <div><dt>{t('wireguard.endpoint')}</dt><dd>{status.data.endpoint || '—'}</dd></div>
            <div>
              <dt>{t('wireguard.reachability')}</dt>
              <dd>{status.data.interfaceReachable ? t('wireguard.reachable') : t('wireguard.unreachable')}</dd>
            </div>
          </dl>

          <section className="wireguard-pairing">
            <h2>{t('wireguard.pairingTitle')}</h2>
            <button type="button" disabled={createToken.isPending} onClick={() => createToken.mutate()}>
              {createToken.isPending ? t('loading') : t('wireguard.createToken')}
            </button>
            {createToken.isError && <p role="alert">{t('error')}</p>}
            {pairing && (
              <div className="wireguard-qr">
                <img src={`data:image/png;base64,${pairing.qrPngBase64}`} alt={t('wireguard.qrAlt')} />
                <div>
                  <p>{t('wireguard.expiresIn', { countdown })}</p>
                  <p><a href={pairing.payload}>{t('wireguard.openPairingLink')}</a></p>
                  <code>{pairing.payload}</code>
                </div>
              </div>
            )}
          </section>

          <section className="wireguard-peers">
            <h2>{t('wireguard.peersTitle')}</h2>
            {peers.isLoading && <p>{t('loading')}</p>}
            {peers.isError && <p role="alert">{t('error')}</p>}
            {peers.data && peers.data.length === 0 && <p>{t('wireguard.noPeers')}</p>}
            {peers.data && peers.data.length > 0 && (
              <table className="wireguard-table">
                <thead>
                  <tr>
                    <th>{t('wireguard.device')}</th>
                    <th>{t('wireguard.address')}</th>
                    <th>{t('wireguard.status')}</th>
                    <th>{t('wireguard.handshake')}</th>
                    <th>{t('wireguard.created')}</th>
                    <th>{t('wireguard.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {peers.data.map((peer) => (
                    <tr key={peer.id}>
                      <td>{peer.deviceName}</td>
                      <td>{peer.assignedIp}</td>
                      <td>{t(`wireguard.peerStatus.${peer.status}`)}</td>
                      <td>{relativeTime(peer.lastHandshakeAt, now, i18n.language, t('wireguard.never'))}</td>
                      <td>{peer.createdAt ? new Date(peer.createdAt).toLocaleDateString(i18n.language) : '—'}</td>
                      <td>
                        {peer.status === 'ACTIVE' && (
                          <button
                            type="button"
                            className="ghost danger"
                            disabled={revoke.isPending}
                            onClick={() => {
                              if (window.confirm(t('wireguard.confirmRevoke', { name: peer.deviceName }))) {
                                revoke.mutate(peer.id)
                              }
                            }}
                          >
                            {t('wireguard.revoke')}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}
    </section>
  )
}
