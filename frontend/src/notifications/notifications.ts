import { api } from '../api/client'

export interface PushKeys { p256dh: string; auth: string }

export function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  return Uint8Array.from(rawData, (c) => c.charCodeAt(0))
}

export async function subscribeToNotifications(householdId: string, publicKey: string): Promise<void> {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    throw new Error('Not supported')
  }
  const registration = await navigator.serviceWorker.ready
  let subscription = await registration.pushManager.getSubscription()
  if (subscription) {
    await unsubscribeFromNotifications(householdId, subscription.endpoint)
    await subscription.unsubscribe()
  }
  subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(publicKey),
  })
  const json = subscription.toJSON()
  const keys = json.keys as { p256dh: string; auth: string }
  await api(`/households/${householdId}/notifications/subscriptions`, {
    method: 'POST',
    body: JSON.stringify({ endpoint: subscription.endpoint, keys }),
  })
}

export async function unsubscribeFromNotifications(householdId: string, endpoint: string): Promise<void> {
  await api(`/households/${householdId}/notifications/subscriptions`, {
    method: 'DELETE',
    body: JSON.stringify({ endpoint }),
  })
}
