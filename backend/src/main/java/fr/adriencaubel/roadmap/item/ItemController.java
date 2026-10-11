package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.category.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/roadmaps/{roadmapId}/items")
@RequiredArgsConstructor
public class ItemController {

	private final ItemService service;

	@GetMapping
	public List<ItemResponse> list(@PathVariable Long roadmapId) {
		return service.list(roadmapId).stream().map(ItemResponse::of).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ItemResponse create(@PathVariable Long roadmapId, @RequestBody ItemRequest request) {
		return ItemResponse.of(service.create(roadmapId, request));
	}

	@GetMapping("/{itemId}")
	public ItemResponse get(@PathVariable Long roadmapId, @PathVariable Long itemId) {
		return ItemResponse.of(service.get(roadmapId, itemId));
	}

	@PutMapping("/{itemId}")
	public ItemResponse replace(@PathVariable Long roadmapId, @PathVariable Long itemId,
			@RequestBody ItemRequest request) {
		return ItemResponse.of(service.replace(roadmapId, itemId, request));
	}

	/** The body lists every Item id of the Roadmap in the new order. */
	@PutMapping("/order")
	public List<ItemResponse> reorder(@PathVariable Long roadmapId, @RequestBody OrderRequest request) {
		return service.reorder(roadmapId, request.itemIds()).stream().map(ItemResponse::of).toList();
	}

	@DeleteMapping("/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long roadmapId, @PathVariable Long itemId) {
		service.delete(roadmapId, itemId);
	}

	public record OrderRequest(List<Long> itemIds) {
	}

	public record ItemResponse(Long id, String title, String description, LocalDate startDate, LocalDate endDate,
			String status, Long categoryId, boolean internal, int position) {

		static ItemResponse of(Item item) {
			Category category = item.getCategory();
			return new ItemResponse(item.getId(), item.getTitle(), item.getDescription(), item.getStartDate(),
					item.getEndDate(), item.getStatus().key(), category == null ? null : category.getId(),
					item.isInternal(), item.getPosition());
		}
	}
}
