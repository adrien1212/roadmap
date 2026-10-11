package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.category.Category;
import fr.adriencaubel.roadmap.category.CategoryRepository;
import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.common.ValidationException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import fr.adriencaubel.roadmap.roadmap.Roadmap;
import fr.adriencaubel.roadmap.roadmap.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

	private final ItemRepository items;
	private final RoadmapService roadmaps;
	private final CategoryRepository categories;
	private final CurrentOwner currentOwner;

	@Transactional(readOnly = true)
	public List<Item> list(Long roadmapId) {
		return items.findAllByRoadmapOrderByPositionAsc(roadmaps.get(roadmapId));
	}

	@Transactional(readOnly = true)
	public Item get(Long roadmapId, Long itemId) {
		return items.findByIdAndRoadmap(itemId, roadmaps.get(roadmapId))
				.orElseThrow(() -> new NotFoundException("Item not found"));
	}

	public Item create(Long roadmapId, ItemRequest request) {
		Roadmap roadmap = roadmaps.get(roadmapId);
		Item item = Item.append(roadmap, request.title(), items.findLastPosition(roadmap));
		item.describe(request.description());
		item.schedule(request.startDate(), request.endDate());
		if (request.status() != null) {
			item.changeStatus(ItemStatus.fromKey(request.status()), Instant.now());
		}
		item.categorize(category(request.categoryId()));
		item.markInternal(Boolean.TRUE.equals(request.internal()));
		return items.save(item);
	}

	/** Replaces every field except the position. */
	public Item replace(Long roadmapId, Long itemId, ItemRequest request) {
		Item item = get(roadmapId, itemId);
		item.retitle(request.title());
		item.describe(request.description());
		item.schedule(request.startDate(), request.endDate());
		item.changeStatus(ItemStatus.fromKey(request.status()), Instant.now());
		item.categorize(category(request.categoryId()));
		item.markInternal(Boolean.TRUE.equals(request.internal()));
		return item;
	}

	/** Rewrites the manual order; orderedIds must list every Item of the Roadmap exactly once. */
	public List<Item> reorder(Long roadmapId, List<Long> orderedIds) {
		List<Item> ordered = ItemOrder.arrange(list(roadmapId), Item::getId, orderedIds);
		for (int i = 0; i < ordered.size(); i++) {
			ordered.get(i).moveTo(i);
		}
		return ordered;
	}

	public void delete(Long roadmapId, Long itemId) {
		items.delete(get(roadmapId, itemId));
	}

	/** Only the current Owner's Categories; an unknown or foreign id is the same 400. */
	private Category category(Long categoryId) {
		if (categoryId == null) {
			return null;
		}
		return categories.findByIdAndOwner(categoryId, currentOwner.get())
				.orElseThrow(() -> new ValidationException("Category not found"));
	}
}
