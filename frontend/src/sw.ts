/// <reference lib="es2023" />
/// <reference types="@types/serviceworker" />

import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching'
import { NavigationRoute, registerRoute } from 'workbox-routing'
import { NetworkFirst, NetworkOnly } from 'workbox-strategies'
import { BackgroundSyncPlugin } from 'workbox-background-sync'
import { clientsClaim } from 'workbox-core'

declare const self: ServiceWorkerGlobalScope

cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

const isShoppingMutation = (input: { url: URL; request: Request }): boolean =>
  input.url.pathname.startsWith('/api/v1/households/') &&
  input.url.pathname.includes('/shopping/') &&
  ['POST', 'PATCH', 'PUT', 'DELETE'].includes(input.request.method)

registerRoute(
  isShoppingMutation,
  new NetworkOnly({
    plugins: [new BackgroundSyncPlugin('fanel-shopping-mutations', { maxRetentionTime: 24 * 60 })],
  })
)

registerRoute(
  (input: { url: URL; request: Request }) =>
    input.url.pathname.startsWith('/api/v1/households/') &&
    input.url.pathname.includes('/shopping/') &&
    input.request.method === 'GET',
  new NetworkFirst({ cacheName: 'fanel-shopping-get', plugins: [] })
)

registerRoute(new NavigationRoute(new NetworkFirst({ cacheName: 'fanel-pages' })))

self.addEventListener('push', (event) => {
  const data = (event.data?.json() ?? { title: 'Fanel', body: '' }) as { title: string; body: string }
  event.waitUntil(self.registration.showNotification(data.title, { body: data.body, icon: '/icon-192.png' }))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil(self.clients.openWindow('/'))
})

self.skipWaiting()
clientsClaim()
