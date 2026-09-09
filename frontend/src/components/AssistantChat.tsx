import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useHousehold } from '../context/HouseholdContext'
import { api } from '../api/client'

type Agent = {
  id: string
  nameKey: string
  descriptionKey: string
  supportsMedia: boolean
  toolNames: string[]
}

type Attachment = { mimeType: string; data: string }

type Message = {
  role: 'user' | 'assistant'
  text: string
  attachments?: Attachment[]
  recipe?: RecipeSuggestion
  error?: boolean
}

type RecipeSuggestion = {
  name: string
  servings: number
  notes?: string
  tags: string[]
  ingredients: { name: string; quantity?: number; unit?: string; category?: string }[]
}

type AgentResponse = {
  agentId: string
  conversationId: string
  text: string
  toolCalls: string[]
}

type CreatedRecipe = {
  id: string
  name: string
  servings: number
  notes?: string
  tags: string[]
  ingredients: { name: string; quantity?: number; unit?: string; category?: string }[]
}

const stroke = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' } as const

function uuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      resolve(result.split(',')[1] ?? '')
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function tryParseRecipe(text: string): RecipeSuggestion | undefined {
  try {
    const parsed = JSON.parse(text) as RecipeSuggestion & { error?: boolean }
    if (parsed.error) return undefined
    if (parsed.name && Array.isArray(parsed.ingredients)) return parsed
  } catch {
    // not a JSON response
  }
  return undefined
}

export function AssistantChat() {
  const { t } = useTranslation()
  const { household } = useHousehold()
  const [open, setOpen] = useState(false)
  const [agents, setAgents] = useState<Agent[]>([])
  const [selectedAgent, setSelectedAgent] = useState<string>('')
  const [loadError, setLoadError] = useState<string | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [pending, setPending] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [conversationIds, setConversationIds] = useState<Record<string, string>>({})
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!household || !open) return
    setLoadError(null)
    api<Agent[]>(`/households/${household.id}/assistant/agents`)
      .then((list) => {
        setAgents(list)
        setSelectedAgent((prev) => prev || (list[0]?.id ?? ''))
      })
      .catch((err) => {
        setAgents([])
        setLoadError(err instanceof Error ? err.message : t('error'))
      })
  }, [household, open, t])

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, open])

  if (!household) return null

  const selected = agents.find((a) => a.id === selectedAgent)

  const send = async () => {
    if (!selectedAgent || (!input.trim() && !file)) return
    setPending(true)

    const attachments: Attachment[] = []
    if (file) {
      const data = await fileToBase64(file)
      attachments.push({ mimeType: file.type, data })
    }

    const userText = input.trim() || (file ? t('assistant.imageAttached') : '')
    setMessages((prev) => [...prev, { role: 'user', text: userText, attachments }])
    setInput('')
    setFile(null)

    const conversationId = conversationIds[selectedAgent] ?? uuid()
    try {
      const response = await api<AgentResponse>(`/households/${household.id}/assistant/chat`, {
        method: 'POST',
        body: JSON.stringify({ agentId: selectedAgent, conversationId, message: input, attachments }),
      })
      setConversationIds((prev) => ({ ...prev, [selectedAgent]: response.conversationId }))
      const recipe = tryParseRecipe(response.text)
      setMessages((prev) => [...prev, { role: 'assistant', text: response.text, recipe }])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', text: err instanceof Error ? err.message : t('error'), error: true },
      ])
    } finally {
      setPending(false)
    }
  }

  const createRecipe = async (recipe: RecipeSuggestion) => {
    try {
      await api<CreatedRecipe>(`/households/${household.id}/recipes`, {
        method: 'POST',
        body: JSON.stringify({
          name: recipe.name,
          servings: recipe.servings,
          notes: recipe.notes,
          tags: recipe.tags,
          ingredients: recipe.ingredients.map((i) => ({
            name: i.name,
            quantity: i.quantity,
            unit: i.unit,
            category: i.category,
          })),
        }),
      })
      setMessages((prev) => [...prev, { role: 'assistant', text: t('assistant.recipeCreated') }])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', text: err instanceof Error ? err.message : t('error'), error: true },
      ])
    }
  }

  const handleKey = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      void send()
    }
  }

  return (
    <>
      <button
        type="button"
        className="assistant-fab"
        aria-label={t('assistant.open')}
        onClick={() => setOpen(true)}
      >
        <svg viewBox="0 0 24 24" width="1.5rem" height="1.5rem" {...stroke}>
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
      </button>

      {open && (
        <>
          <div className="assistant-backdrop" onClick={() => setOpen(false)} />
          <div className="assistant-panel" role="dialog" aria-label={t('assistant.title')}>
            <div className="assistant-header">
              <h3>{t('assistant.title')}</h3>
              <select
                value={selectedAgent}
                onChange={(e) => setSelectedAgent(e.target.value)}
                aria-label={t('assistant.selectAgent')}
              >
                {agents.length === 0 && <option value="">{t('assistant.noAgents')}</option>}
                {agents.map((a) => (
                  <option key={a.id} value={a.id}>
                    {t(a.nameKey)}
                  </option>
                ))}
              </select>
              <button type="button" className="icon-btn" onClick={() => setOpen(false)} aria-label={t('assistant.close')}>
                ✕
              </button>
            </div>

            {loadError && <p className="assistant-error">{loadError}</p>}

            <div className="assistant-messages">
              {messages.length === 0 && !loadError && (
                <p className="assistant-empty">{t('assistant.intro')}</p>
              )}
              {messages.map((m, i) => (
                <div
                  key={i}
                  className={`assistant-message ${m.role}${m.error ? ' error' : ''}`}
                >
                  {m.attachments?.map((a, j) => (
                    a.mimeType.startsWith('image/') ? (
                      <img
                        key={j}
                        src={`data:${a.mimeType};base64,${a.data}`}
                        alt={t('assistant.imageAttached')}
                      />
                    ) : (
                      <span key={j}>{t('assistant.imageAttached')}</span>
                    )
                  ))}
                  {m.text && <p>{m.text}</p>}
                  {m.recipe && (
                    <div className="assistant-recipe">
                      <h4>{m.recipe.name}</h4>
                      <p>{t('recipes.servings')}: {m.recipe.servings}</p>
                      {m.recipe.ingredients.length > 0 && (
                        <ul>
                          {m.recipe.ingredients.map((ing, k) => (
                            <li key={k}>
                              {ing.name}
                              {ing.quantity !== undefined && ` — ${ing.quantity}`}
                              {ing.unit && ` ${ing.unit}`}
                              {ing.category && ` (${ing.category})`}
                            </li>
                          ))}
                        </ul>
                      )}
                      <button type="button" onClick={() => void createRecipe(m.recipe!)}>
                        {t('assistant.createRecipe')}
                      </button>
                    </div>
                  )}
                </div>
              ))}
              <div ref={endRef} />
            </div>

            <div className="assistant-input">
              {selected?.supportsMedia && (
                <label className="assistant-file">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                  />
                  {file ? file.name : t('assistant.attachImage')}
                </label>
              )}
              <div className="assistant-input-row">
                <textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKey}
                  placeholder={t('assistant.placeholder')}
                  rows={2}
                  disabled={pending}
                />
                <button type="button" onClick={() => void send()} disabled={pending || (!input.trim() && !file)}>
                  {t('assistant.send')}
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  )
}
