import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_app/categories')({
  component: CategoriesPage,
})

// Placeholder until Task 11 (Categories UI).
function CategoriesPage() {
  return (
    <main className="mx-auto max-w-[860px] px-4 pb-30 sm:px-8">
      <div className="mt-12 mb-6">
        <h1 className="font-display text-[28px] leading-tight font-semibold tracking-[-0.5px]">Categories</h1>
      </div>
    </main>
  )
}
