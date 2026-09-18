import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'
import { Modal } from '../components/Modal'

type Ingredient = { id?: string; name: string; quantity?: number; unit?: string; category?: string }
type Recipe = {
  id: string
  name: string
  servings: number
  notes?: string
  description?: string
  steps?: string[]
  imageMimeType?: string
  imageData?: string
  tags: string[]
  ingredients: Ingredient[]
}

const emptyIngredient: Ingredient = { name: '', quantity: undefined, unit: '' }

type Image = { mimeType: string; data: string }

function fileToBase64(file: File): Promise<Image> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      const [prefix, data] = result.split(',')
      const mimeType = prefix.split(':')[1]?.split(';')[0] ?? file.type
      resolve({ mimeType, data: data ?? '' })
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function normalize(text: string): string {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
}

export function Recipes() {
  const { t } = useTranslation()
  const { household } = useHousehold()
  const queryClient = useQueryClient()
  const [query, setQuery] = useState('')
  const [view, setView] = useState<'list' | 'grid'>('list')
  const [isCreating, setIsCreating] = useState(false)

  const [name, setName] = useState('')
  const [servings, setServings] = useState(4)
  const [notes, setNotes] = useState('')
  const [description, setDescription] = useState('')
  const [steps, setSteps] = useState<string[]>([''])
  const [image, setImage] = useState<Image | null>(null)
  const [ingredients, setIngredients] = useState<Ingredient[]>([{ ...emptyIngredient }])

  const recipes = useQuery({
    queryKey: ['recipes', household?.id],
    queryFn: () => api<Recipe[]>(`/households/${household!.id}/recipes`),
    enabled: !!household,
  })

  const filtered = useMemo(() => {
    const q = normalize(query.trim())
    if (!q) return recipes.data ?? []
    return (recipes.data ?? []).filter((r) => {
      const hay = [r.name, r.description, r.notes, r.ingredients.map((i) => i.name).join(' ')]
        .filter(Boolean)
        .join(' ')
      return normalize(hay).includes(q)
    })
  }, [query, recipes.data])

  const openCreate = () => {
    setName('')
    setServings(4)
    setNotes('')
    setDescription('')
    setSteps([''])
    setImage(null)
    setIngredients([{ ...emptyIngredient }])
    setIsCreating(true)
  }

  const create = useMutation({
    mutationFn: () =>
      api<Recipe>(`/households/${household!.id}/recipes`, {
        method: 'POST',
        body: JSON.stringify({
          name,
          servings,
          notes: notes || undefined,
          description: description || undefined,
          steps: steps.filter((s) => s.trim()),
          tags: [],
          ingredients: ingredients.filter((i) => i.name.trim()),
          imageMimeType: image?.mimeType,
          imageData: image?.data,
        }),
      }),
    onSuccess: async () => {
      setIsCreating(false)
      openCreate()
      await queryClient.invalidateQueries({ queryKey: ['recipes'] })
    },
  })

  const remove = useMutation({
    mutationFn: (recipeId: string) =>
      api<void>(`/households/${household!.id}/recipes/${recipeId}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  })

  const updateIngredient = (index: number, patch: Partial<Ingredient>) => {
    setIngredients(ingredients.map((ing, i) => (i === index ? { ...ing, ...patch } : ing)))
  }

  const updateStep = (index: number, value: string) => {
    setSteps(steps.map((s, i) => (i === index ? value : s)))
  }

  const handleImage = async (file: File | undefined) => {
    if (!file) {
      setImage(null)
      return
    }
    setImage(await fileToBase64(file))
  }

  const RecipeImage = ({ recipe, className }: { recipe: Recipe; className?: string }) => {
    if (!recipe.imageMimeType || !recipe.imageData) return null
    return <img src={`data:${recipe.imageMimeType};base64,${recipe.imageData}`} alt={recipe.name} className={className} />
  }

  const RecipeInfo = ({ recipe }: { recipe: Recipe }) => (
    <>
      <h4>{recipe.name}</h4>
      <span className="meta">{t('recipes.servingsCount', { count: recipe.servings })}</span>
      {recipe.description && <p className="recipe-description">{recipe.description}</p>}
      {recipe.steps && recipe.steps.length > 0 && (
        <ol className="steps-list">
          {recipe.steps.map((s, i) => <li key={i}>{s}</li>)}
        </ol>
      )}
      <p className="ingredient-list">{recipe.ingredients.map((i) => i.name).join(', ')}</p>
    </>
  )

  return (
    <section className="panel">
      <div className="recipes-toolbar">
        <h2>{t('recipes.title')}</h2>
        <div className="recipes-search-bar">
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t('recipes.searchPlaceholder')}
            aria-label={t('recipes.searchPlaceholder')}
          />
          <div className="view-toggle" role="group" aria-label={t('recipes.view')}>
            <button
              type="button"
              className={view === 'list' ? 'active' : ''}
              onClick={() => setView('list')}
            >
              {t('recipes.list')}
            </button>
            <button
              type="button"
              className={view === 'grid' ? 'active' : ''}
              onClick={() => setView('grid')}
            >
              {t('recipes.grid')}
            </button>
          </div>
        </div>
      </div>

      {recipes.isPending && <p>{t('loading')}</p>}
      {recipes.isError && <p role="alert">{t('error')}</p>}

      {view === 'grid' ? (
        <div className="recipe-grid">
          {filtered.map((recipe) => (
            <div key={recipe.id} className="recipe-card">
              <RecipeImage recipe={recipe} className="recipe-card-image" />
              <div className="recipe-card-body">
                <RecipeInfo recipe={recipe} />
              </div>
              <button type="button" className="link" onClick={() => remove.mutate(recipe.id)}>✕</button>
            </div>
          ))}
        </div>
      ) : (
        <ul className="item-list recipe-list">
          {filtered.map((recipe) => (
            <li key={recipe.id} className="recipe-list-item">
              <RecipeImage recipe={recipe} className="recipe-list-thumb" />
              <div className="recipe-list-body">
                <RecipeInfo recipe={recipe} />
              </div>
              <button type="button" className="link" onClick={() => remove.mutate(recipe.id)}>✕</button>
            </li>
          ))}
        </ul>
      )}

      {filtered.length === 0 && !recipes.isPending && !recipes.isError && (
        <p className="meta">{t('recipes.noResults')}</p>
      )}

      <button type="button" className="fab" aria-label={t('recipes.newRecipe')} onClick={openCreate}>
        +
      </button>

      {isCreating && (
        <Modal title={t('recipes.newRecipe')} onClose={() => setIsCreating(false)}>
          <form
            className="create-form modal-form"
            onSubmit={(e) => {
              e.preventDefault()
              if (name.trim()) create.mutate()
            }}
          >
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t('recipes.namePlaceholder')}
              required
              autoFocus
            />
            <label className="inline">
              {t('recipes.servings')}
              <input
                type="number"
                min={1}
                value={servings}
                onChange={(e) => setServings(Number(e.target.value))}
              />
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('recipes.descriptionPlaceholder')}
              rows={3}
            />
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder={t('recipes.notesPlaceholder')}
              rows={2}
            />
            <fieldset>
              <legend>{t('recipes.steps')}</legend>
              <ol className="steps-list">
                {steps.map((step, i) => (
                  <li key={i}>
                    <input
                      value={step}
                      placeholder={t('recipes.stepPlaceholder', { number: i + 1 })}
                      onChange={(e) => updateStep(i, e.target.value)}
                    />
                  </li>
                ))}
              </ol>
              <button type="button" className="link" onClick={() => setSteps([...steps, ''])}>
                {t('recipes.addStep')}
              </button>
            </fieldset>
            <fieldset>
              <legend>{t('recipes.ingredients')}</legend>
              {ingredients.map((ingredient, i) => (
                <div key={i} className="ingredient-row">
                  <input
                    value={ingredient.name}
                    placeholder={t('recipes.ingredientName')}
                    onChange={(e) => updateIngredient(i, { name: e.target.value })}
                  />
                  <input
                    type="number"
                    value={ingredient.quantity ?? ''}
                    placeholder={t('recipes.quantity')}
                    onChange={(e) =>
                      updateIngredient(i, { quantity: e.target.value ? Number(e.target.value) : undefined })
                    }
                  />
                  <input
                    value={ingredient.unit ?? ''}
                    placeholder={t('recipes.unit')}
                    onChange={(e) => updateIngredient(i, { unit: e.target.value })}
                  />
                </div>
              ))}
              <button
                type="button"
                className="link"
                onClick={() => setIngredients([...ingredients, { ...emptyIngredient }])}
              >
                {t('recipes.addIngredient')}
              </button>
            </fieldset>
            <label className="inline file-label">
              {t('recipes.image')}
              <input type="file" accept="image/*" onChange={(e) => handleImage(e.target.files?.[0])} />
            </label>
            {image && (
              <img
                src={`data:${image.mimeType};base64,${image.data}`}
                alt={t('recipes.preview')}
                className="recipe-thumb"
              />
            )}
            <div className="footer-actions">
              <button type="submit" disabled={create.isPending}>
                {t('recipes.create')}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </section>
  )
}
