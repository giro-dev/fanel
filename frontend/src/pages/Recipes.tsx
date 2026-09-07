import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { useHousehold } from '../context/HouseholdContext'

type Ingredient = { id?: string; name: string; quantity?: number; unit?: string; category?: string }
type Recipe = { id: string; name: string; servings: number; notes?: string; tags: string[]; ingredients: Ingredient[] }

const emptyIngredient: Ingredient = { name: '', quantity: undefined, unit: '' }

export function Recipes() {
  const { t } = useTranslation()
  const { household } = useHousehold()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [servings, setServings] = useState(4)
  const [ingredients, setIngredients] = useState<Ingredient[]>([{ ...emptyIngredient }])

  const recipes = useQuery({
    queryKey: ['recipes', household?.id],
    queryFn: () => api<Recipe[]>(`/households/${household!.id}/recipes`),
    enabled: !!household,
  })

  const create = useMutation({
    mutationFn: () => api<Recipe>(`/households/${household!.id}/recipes`, {
      method: 'POST',
      body: JSON.stringify({
        name, servings, tags: [],
        ingredients: ingredients.filter((i) => i.name.trim()),
      }),
    }),
    onSuccess: async () => {
      setName(''); setServings(4); setIngredients([{ ...emptyIngredient }])
      await queryClient.invalidateQueries({ queryKey: ['recipes'] })
    },
  })

  const remove = useMutation({
    mutationFn: (recipeId: string) => api<void>(`/households/${household!.id}/recipes/${recipeId}`, { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recipes'] }),
  })

  const updateIngredient = (index: number, patch: Partial<Ingredient>) => {
    setIngredients(ingredients.map((ing, i) => (i === index ? { ...ing, ...patch } : ing)))
  }

  return (
    <section className="panel">
      <h2>{t('recipes.title')}</h2>
      <form className="create-form recipe-form" onSubmit={(e) => { e.preventDefault(); if (name.trim()) create.mutate() }}>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder={t('recipes.namePlaceholder')} required />
        <label className="inline">
          {t('recipes.servings')}
          <input type="number" min={1} value={servings} onChange={(e) => setServings(Number(e.target.value))} />
        </label>
        <fieldset>
          <legend>{t('recipes.ingredients')}</legend>
          {ingredients.map((ingredient, i) => (
            <div key={i} className="ingredient-row">
              <input value={ingredient.name} placeholder={t('recipes.ingredientName')}
                     onChange={(e) => updateIngredient(i, { name: e.target.value })} />
              <input type="number" value={ingredient.quantity ?? ''} placeholder={t('recipes.quantity')}
                     onChange={(e) => updateIngredient(i, { quantity: e.target.value ? Number(e.target.value) : undefined })} />
              <input value={ingredient.unit ?? ''} placeholder={t('recipes.unit')}
                     onChange={(e) => updateIngredient(i, { unit: e.target.value })} />
            </div>
          ))}
          <button type="button" className="link" onClick={() => setIngredients([...ingredients, { ...emptyIngredient }])}>
            {t('recipes.addIngredient')}
          </button>
        </fieldset>
        <button type="submit" disabled={create.isPending}>{t('recipes.create')}</button>
      </form>
      {recipes.isPending && <p>{t('loading')}</p>}
      {recipes.isError && <p role="alert">{t('error')}</p>}
      <ul className="item-list">
        {recipes.data?.map((recipe) => (
          <li key={recipe.id}>
            <div>
              <strong>{recipe.name}</strong>
              <span className="meta">{t('recipes.servingsCount', { count: recipe.servings })}</span>
              <p className="ingredient-list">
                {recipe.ingredients.map((i) => i.name).join(', ')}
              </p>
            </div>
            <button type="button" className="link" onClick={() => remove.mutate(recipe.id)}>✕</button>
          </li>
        ))}
      </ul>
    </section>
  )
}
