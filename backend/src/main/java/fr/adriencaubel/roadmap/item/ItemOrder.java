package fr.adriencaubel.roadmap.item;

import fr.adriencaubel.roadmap.common.ValidationException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The manual order of a Roadmap's Items, rewritten from a full ordered list of their ids. */
final class ItemOrder {

	private ItemOrder() {
	}

	/** The items in the order of orderedIds, which must list every item exactly once. */
	static <T> List<T> arrange(Collection<T> items, Function<T, Long> idOf, List<Long> orderedIds) {
		if (orderedIds == null) {
			throw new ValidationException("itemIds is required");
		}
		Map<Long, T> byId = items.stream().collect(Collectors.toMap(idOf, Function.identity()));
		if (orderedIds.size() != byId.size() || !new HashSet<>(orderedIds).equals(byId.keySet())) {
			throw new ValidationException("itemIds must list every Item of the Roadmap exactly once");
		}
		return orderedIds.stream().map(byId::get).toList();
	}
}
