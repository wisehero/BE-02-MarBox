package prgrms.marco.be02marbox.domain.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static prgrms.marco.be02marbox.domain.exception.custom.Message.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import prgrms.marco.be02marbox.config.QueryDslConfig;
import prgrms.marco.be02marbox.domain.movie.Genre;
import prgrms.marco.be02marbox.domain.movie.LimitAge;
import prgrms.marco.be02marbox.domain.movie.Movie;
import prgrms.marco.be02marbox.domain.movie.repository.MovieRepository;
import prgrms.marco.be02marbox.domain.reservation.Ticket;
import prgrms.marco.be02marbox.domain.reservation.dto.RequestCreateTicket;
import prgrms.marco.be02marbox.domain.reservation.dto.ResponseFindTicket;
import prgrms.marco.be02marbox.domain.reservation.repository.ReservedSeatRepository;
import prgrms.marco.be02marbox.domain.reservation.repository.TicketRepository;
import prgrms.marco.be02marbox.domain.reservation.service.utils.TicketConverter;
import prgrms.marco.be02marbox.domain.theater.Region;
import prgrms.marco.be02marbox.domain.theater.Schedule;
import prgrms.marco.be02marbox.domain.theater.Seat;
import prgrms.marco.be02marbox.domain.theater.Theater;
import prgrms.marco.be02marbox.domain.theater.TheaterRoom;
import prgrms.marco.be02marbox.domain.theater.repository.ScheduleRepository;
import prgrms.marco.be02marbox.domain.theater.repository.SeatRepository;
import prgrms.marco.be02marbox.domain.theater.repository.TheaterRepository;
import prgrms.marco.be02marbox.domain.theater.repository.TheaterRoomRepository;
import prgrms.marco.be02marbox.domain.user.Role;
import prgrms.marco.be02marbox.domain.user.User;
import prgrms.marco.be02marbox.domain.user.repository.UserRepository;

@DataJpaTest
@Import({TicketService.class, TicketConverter.class, QueryDslConfig.class})
class TicketServiceTest {

	@Autowired
	TicketRepository ticketRepository;
	@Autowired
	TheaterRepository theaterRepository;
	@Autowired
	TheaterRoomRepository theaterRoomRepository;
	@Autowired
	ScheduleRepository scheduleRepository;
	@Autowired
	SeatRepository seatRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	ReservedSeatRepository reservedSeatRepository;
	@Autowired
	MovieRepository movieRepository;
	@Autowired
	TicketService ticketService;
	User user1 = new User("iop1996@gmail.com", "password1234", "wisehero1", Role.ROLE_ADMIN);
	User user2 = new User("iop1996@naver.com", "password1234", "wisehero2", Role.ROLE_ADMIN);
	Theater theater = new Theater(Region.DAEGU, "theater0");
	Movie movie1 = new Movie("movie1", LimitAge.ADULT, Genre.ACTION, 120);
	Movie movie2 = new Movie("movie2", LimitAge.ADULT, Genre.ACTION, 120);
	TheaterRoom theaterRoom1 = new TheaterRoom(theater, "first");
	Schedule schedule1 = new Schedule.Builder()
		.movie(movie1)
		.theaterRoom(theaterRoom1)
		.startTime(LocalDateTime.now())
		.endTime(LocalDateTime.now())
		.build();
	TheaterRoom theaterRoom2 = new TheaterRoom(theater, "second");
	Schedule schedule2 = new Schedule.Builder()
		.movie(movie2)
		.theaterRoom(theaterRoom2)
		.startTime(LocalDateTime.now())
		.endTime(LocalDateTime.now())
		.build();

	@BeforeEach
	void setup() {
		userRepository.saveAll(List.of(user1, user2));
		theaterRepository.save(theater);
		movieRepository.saveAll(List.of(movie1, movie2));
		theaterRoomRepository.saveAll(List.of(theaterRoom1, theaterRoom2));
		scheduleRepository.saveAll(List.of(schedule1, schedule2));
	}

	@Test
	@DisplayName("티켓 생성 테스트")
	void testCreateTicket() {
		// given
		seatRepository.saveAll(List.of(new Seat(theaterRoom1, 0, 0), new Seat(theaterRoom1, 0, 1)));
		List<Long> collect = seatRepository.findByTheaterRoomId(theaterRoom1.getId()).stream()
			.map(Seat::getId)
			.collect(Collectors.toList());

		RequestCreateTicket request = new RequestCreateTicket(user1.getId(), schedule1.getId(), LocalDateTime.now(),
			collect);

		// when
		Long createdTicketId = ticketService.createTicket(request);
		Ticket savedTicket = ticketRepository.findById(createdTicketId)
			.orElseThrow(() -> new EntityNotFoundException(NOT_EXISTS_TICKET_EXP_MSG.getMessage()));

		// then
		assertThat(savedTicket).isNotNull();
	}

	@Test
	@DisplayName("티켓 단건 조회 테스트")
	void testFindTicketTest() {
		// given
		Ticket insertedTicket = ticketRepository.save(new Ticket(user1, schedule1, LocalDateTime.now()));

		// when
		ResponseFindTicket findTicket = ticketService.findTicket(insertedTicket.getId());

		// then
		assertAll(
			() -> assertThat(findTicket.username()).isEqualTo(insertedTicket.getUser().getName()),
			() -> assertThat(findTicket.movieName()).isEqualTo(insertedTicket.getSchedule().getMovie().getName()),
			() -> assertThat(findTicket.limitAge()).isEqualTo(
				insertedTicket.getSchedule().getMovie().getLimitAge()),
			() -> assertThat(findTicket.theaterName()).isEqualTo(
				insertedTicket.getSchedule().getTheaterRoom().getTheater().getName()),
			() -> assertThat(findTicket.startTime()).isEqualTo(insertedTicket.getSchedule().getStartTime()),
			() -> assertThat(findTicket.endTime()).isEqualTo(insertedTicket.getSchedule().getEndTime()),
			() -> assertThat(findTicket.theaterRoomName()).isEqualTo(
				insertedTicket.getSchedule().getTheaterRoom().getName())
		);

	}

	@Test
	@DisplayName("전체 티켓 정보 조회 테스트")
	void testFindAllTicket() {
		// given
		ticketRepository.saveAll(List.of(
			new Ticket(user1, schedule1, LocalDateTime.now()),
			new Ticket(user2, schedule2, LocalDateTime.now())));

		// when
		List<ResponseFindTicket> findTickets = ticketService.findTickets();

		// then
		assertThat(findTickets).hasSize(2);
	}

	@Test
	@DisplayName("특정 고객의 티켓 정보 조회")
	void testFindOneUserTickets() {
		// given
		ticketRepository.saveAll(List.of(
			new Ticket(user1, schedule1, LocalDateTime.now()),
			new Ticket(user1, schedule2, LocalDateTime.now())));

		// when
		List<ResponseFindTicket> findTickets = ticketService.findTicketsOfUser(user1.getId());

		// then
		assertAll(
			() -> assertThat(findTickets).hasSize(2),
			() -> assertThat(findTickets.get(0).username()).isEqualTo(user1.getName())
		);
	}

	@Test
	@DisplayName("특정 고객의 티켓 중 사용 가능한 티켓 조회")
	void testFindValidTicketsOfUser() {
		// given
		LocalDateTime reservedDate = LocalDateTime.of(LocalDate.of(2022, 6, 29), LocalTime.of(11, 0));
		Schedule valid = new Schedule.Builder()
			.movie(movie1)
			.theaterRoom(theaterRoom1)
			.startTime(LocalDateTime.of(LocalDate.of(2100, 6, 29), LocalTime.of(13, 0)))
			.endTime(LocalDateTime.of(LocalDate.of(2100, 6, 29), LocalTime.of(15, 0)))
			.build();
		Schedule invalid = new Schedule.Builder()
			.movie(movie2)
			.theaterRoom(theaterRoom2)
			.startTime(LocalDateTime.of(LocalDate.of(2022, 6, 29), LocalTime.of(13, 0)))
			.endTime(LocalDateTime.of(LocalDate.of(2022, 6, 29), LocalTime.of(15, 0)))
			.build();
		scheduleRepository.saveAll(List.of(valid, invalid));
		ticketRepository.saveAll(List.of(
			new Ticket(user1, valid, reservedDate),
			new Ticket(user1, invalid, reservedDate)));

		// when
		List<ResponseFindTicket> validTicketsOfUser = ticketService.findValidTicketsOfUser(user1.getId());

		// then
		assertAll(
			() -> assertThat(validTicketsOfUser).hasSize(1),
			() -> assertThat(validTicketsOfUser.get(0).username()).isEqualTo(user1.getName()),
			() -> assertThat(validTicketsOfUser.get(0).movieName()).isEqualTo(movie1.getName()),
			() -> assertThat(validTicketsOfUser.get(0).theaterRoomName()).isEqualTo(
				theaterRoom1.getName()),
			() -> assertThat(validTicketsOfUser.get(0).endTime().isAfter(LocalDateTime.now())).isTrue()
		);
	}

	@Test
	@DisplayName("특정 스케줄의 티켓 정보 조회")
	void testFindTicketsOfSchedule() {
		// given
		ticketRepository.saveAll(List.of(
			new Ticket(user1, schedule1, LocalDateTime.now()),
			new Ticket(user1, schedule1, LocalDateTime.now())));

		// when
		List<ResponseFindTicket> ticketsOfSchedule = ticketService.findTicketsOfSchedule(schedule1.getId());

		// then
		assertAll(
			() -> assertThat(ticketsOfSchedule).hasSize(2),
			() -> assertThat(ticketsOfSchedule.get(0).startTime()).isEqualTo(schedule1.getStartTime()),
			() -> assertThat(ticketsOfSchedule.get(0).endTime()).isEqualTo(schedule1.getEndTime())
		);
	}

	@Test
	@DisplayName("티켓 생성 테스트")
	void testCreateTicket_Parameters_With_User_And_Schedule() {
		// given
		int payAmount = 10000;

		// when
		Ticket createdTicket = ticketService.createTicket(user1, schedule1, payAmount);

		// then
		Ticket savedTicket = ticketRepository.findById(createdTicket.getId()).get();

		assertThat(savedTicket).isNotNull();
	}

}
