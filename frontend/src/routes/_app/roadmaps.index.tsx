import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_app/roadmaps/')({
  component: RoadmapsPage,
})

// Placeholder until Task 12 (Roadmaps UI).
function RoadmapsPage() {
  return (
    <main className="mx-auto max-w-[1200px] px-4 pb-30 sm:px-8">
      <div className="mt-12 mb-6">
        <h1 className="font-display text-[28px] leading-tight font-semibold tracking-[-0.5px]">Roadmaps</h1>
      </div>
    </main>
  )
}
