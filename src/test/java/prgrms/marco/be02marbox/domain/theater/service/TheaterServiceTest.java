package prgrms.marco.be02marbox.domain.theater.service;

import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import prgrms.marco.be02marbox.domain.theater.Region;
import prgrms.marco.be02marbox.domain.theater.Theater;
import prgrms.marco.be02marbox.domain.theater.dto.RequestCreateTheater;
import prgrms.marco.be02marbox.domain.theater.dto.ResponseFindTheater;
import prgrms.marco.be02marbox.domain.theater.repository.TheaterRepository;
import prgrms.marco.be02marbox.domain.theater.service.utils.TheaterConverter;

@DataJpaTest
@Import({TheaterService.class, TheaterConverter.class})
class TheaterServiceTest {

	@Autowired
	TheaterRepository theaterRepository;
	@Autowired
	TheaterService theaterService;

	@Test
	@DisplayName("영화관 추가 성공")
	void testCreateTheaterSuccess() {
		// given
		RequestCreateTheater request = new RequestCreateTheater("SEOUL", "CGV 강남점");
		// when
		Long savedTheaterId = theaterService.createTheater(request);
		Theater findTheater = theaterRepository.findById(savedTheaterId)
			.orElseThrow(EntityNotFoundException::new);
		// then
		assertAll(
			() -> assertThat(findTheater.getId()).isEqualTo(savedTheaterId),
			() -> assertThat(findTheater.getRegion()).isEqualTo(Region.valueOf("SEOUL")),
			() -> assertThat(findTheater.getName()).isEqualTo("CGV 강남점")
		);
	}

	@Test
	@DisplayName("관리자 영화관 전체 조회 - 페이징 X")
	void testGetAllTheater() {
		// given
		List<Theater> theaters = IntStream.range(0, 20)
			.mapToObj(i -> new Theater(Region.valueOf("SEOUL"), "theater" + i)).collect(toList());
		theaterRepository.saveAll(theaters);

		// when
		List<ResponseFindTheater> findTheaters = theaterService.findTheaters();

		// then
		assertAll(
			() -> assertThat(findTheaters.size()).isEqualTo(20),
			() -> assertThat(findTheaters.get(0).region().toString()).isEqualTo("SEOUL"),
			() -> assertThat(findTheaters.get(0).theaterName()).isEqualTo("theater0")
		);
	}
}
