package fr.adriencaubel.roadmap.category;

import fr.adriencaubel.roadmap.common.ConflictException;
import fr.adriencaubel.roadmap.common.NotFoundException;
import fr.adriencaubel.roadmap.owner.CurrentOwner;
import fr.adriencaubel.roadmap.owner.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

	private final CategoryRepository categories;
	private final CurrentOwner currentOwner;

	/** Each Category with the number of Items using it across all Roadmaps. */
	@Transactional(readOnly = true)
	public List<CategoryUsage> list() {
		Owner owner = currentOwner.get();
		Map<Long, Long> counts = categories.countItems(owner).stream()
				.collect(Collectors.toMap(CategoryItemCount::categoryId, CategoryItemCount::itemCount));
		return categories.findAllByOwnerOrderByIdAsc(owner).stream()
				.map(category -> new CategoryUsage(category, counts.getOrDefault(category.getId(), 0L)))
				.toList();
	}

	public Category create(String name, String colourKey) {
		Owner owner = currentOwner.get();
		Category category = new Category(owner, name, CategoryColour.fromKey(colourKey));
		if (categories.existsByOwnerAndName(owner, category.getName())) {
			throw duplicate(category.getName());
		}
		return categories.save(category);
	}

	/** Null fields are left unchanged. */
	public Category update(Long id, String name, String colourKey) {
		Owner owner = currentOwner.get();
		Category category = get(id);
		if (name != null) {
			// Check before renaming: the query would otherwise flush the new name and hit the unique key first.
			String normalized = Category.normalizeName(name);
			if (categories.existsByOwnerAndNameAndIdNot(owner, normalized, id)) {
				throw duplicate(normalized);
			}
			category.rename(normalized);
		}
		if (colourKey != null) {
			category.recolour(CategoryColour.fromKey(colourKey));
		}
		return category;
	}

	/** Items that used the Category keep existing; the FK sets their category to null. */
	public void delete(Long id) {
		categories.delete(get(id));
	}

	@Transactional(readOnly = true)
	public Category get(Long id) {
		return categories.findByIdAndOwner(id, currentOwner.get())
				.orElseThrow(() -> new NotFoundException("Category not found"));
	}

	private static ConflictException duplicate(String name) {
		return new ConflictException("A Category named \"" + name + "\" already exists");
	}
}
