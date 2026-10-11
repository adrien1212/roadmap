package fr.adriencaubel.roadmap.category;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService service;

	@GetMapping
	public List<CategoryUsageResponse> list() {
		return service.list().stream().map(CategoryUsageResponse::of).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryResponse create(@RequestBody CategoryRequest request) {
		return CategoryResponse.of(service.create(request.name(), request.colour()));
	}

	@PatchMapping("/{id}")
	public CategoryResponse update(@PathVariable Long id, @RequestBody CategoryRequest request) {
		return CategoryResponse.of(service.update(id, request.name(), request.colour()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		service.delete(id);
	}

	public record CategoryRequest(String name, String colour) {
	}

	public record CategoryResponse(Long id, String name, String colour) {

		static CategoryResponse of(Category category) {
			return new CategoryResponse(category.getId(), category.getName(), category.getColour().key());
		}
	}

	/** The Categories screen row: itemCount counts Items across all the Owner's Roadmaps. */
	public record CategoryUsageResponse(Long id, String name, String colour, long itemCount) {

		static CategoryUsageResponse of(CategoryUsage usage) {
			Category category = usage.category();
			return new CategoryUsageResponse(category.getId(), category.getName(), category.getColour().key(),
					usage.itemCount());
		}
	}
}
