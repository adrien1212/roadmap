package fr.adriencaubel.roadmap.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Owner's Brand, set in Settings. A blank or null name clears it. */
@RestController
@RequestMapping("/api/brand")
@RequiredArgsConstructor
public class BrandController {

	private final BrandService service;

	@GetMapping
	public BrandBody get() {
		return new BrandBody(service.get());
	}

	@PutMapping
	public BrandBody change(@RequestBody BrandBody body) {
		return new BrandBody(service.change(body.name()));
	}

	public record BrandBody(String name) {
	}
}
