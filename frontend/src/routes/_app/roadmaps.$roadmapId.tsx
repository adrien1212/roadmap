import { useState } from 'react'
import { Link, createFileRoute, notFound, useNavigate } from '@tanstack/react-router'
import { useSuspenseQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'

import { NotFound } from '#/components/not-found'
import { RoadmapTitle } from '#/components/roadmap-title'
import { Button } from '#/components/ui/button'
import { ApiError } from '#/lib/api'
import { roadmapQuery } from '#/lib/roadmaps'

export const Route = createFileRoute('/_app/roadmaps/$roadmapId')({
  params: {
    parse: (params) => ({ roadmapId: Number(params.roadmapId) }),
    stringify: (params) => ({ roadmapId: String(params.roadmapId) }),
  },
  /** `?rename=true` opens the name in inline-rename mode (right after "New Roadmap"). */
  validateSearch: (search: Record<string, unknown>): { rename?: boolean } =>
    search.rename === true || search.rename === 'true' ? { rename: true } : {},
  loader: async ({ context, params }) => {
    try {
      await context.queryClient.ensureQueryData(roadmapQuery(params.roadmapId))
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw notFound()
      throw error
    }
  },
  notFoundComponent: NotFound,
  component: EditorPage,
})

function EditorPage() {
  const { roadmapId } = Route.useParams()
  const { rename } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  const { data: roadmap } = useSuspenseQuery(roadmapQuery(roadmapId))
  const [renaming, setRenaming] = useState(rename === true)

  function onRenamingChange(editing: boolean) {
    setRenaming(editing)
    if (!editing && rename) navigate({ search: {}, replace: true })
  }

  return (
    <main className="mx-auto max-w-[1200px] px-4 pb-35 sm:px-8">
      <div className="mt-6">
        <Button variant="ghost" size="sm" asChild className="-ml-2.5">
          <Link to="/roadmaps">
            <ArrowLeft aria-hidden />
            Roadmaps
          </Link>
        </Button>
      </div>
      <div className="mt-2.5 mb-1 flex flex-wrap items-center justify-between gap-4">
        <div className="flex min-w-[260px] flex-1 items-center gap-1.5">
          <RoadmapTitle roadmap={roadmap} editing={renaming} onEditingChange={onRenamingChange} />
        </div>
      </div>
    </main>
  )
}
