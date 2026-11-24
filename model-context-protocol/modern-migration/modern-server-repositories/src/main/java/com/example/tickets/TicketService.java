package com.example.tickets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * 其他示例中的内存工单存储，并为每次变更增加一个 {@link TicketChanged} 事件。
 * 资源仓库会将这些事件转换为 MCP 通知，因此领域代码从不直接与 MCP 服务器交互。
 */
@Service
public class TicketService {

	public record Ticket(String id, String title, boolean resolved) {
	}

	/** 在工单被打开、解决或删除之后发布。 */
	public record TicketChanged(String id, Kind kind) {

		public enum Kind {

			ADDED, UPDATED, DELETED

		}

	}

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

	private final AtomicInteger ids = new AtomicInteger();

	private final ApplicationEventPublisher events;

	public TicketService(ApplicationEventPublisher events) {
		this.events = events;
		store(new Ticket("t-" + this.ids.incrementAndGet(), "Write the quarterly report", false));
		store(new Ticket("t-" + this.ids.incrementAndGet(), "Review the MCP migration guide", false));
		store(new Ticket("t-" + this.ids.incrementAndGet(), "Book the team offsite", true));
	}

	public Ticket open(String title) {
		Ticket ticket = store(new Ticket("t-" + this.ids.incrementAndGet(), title, false));
		this.events.publishEvent(new TicketChanged(ticket.id(), TicketChanged.Kind.ADDED));
		return ticket;
	}

	public List<Ticket> list() {
		List<Ticket> result = new ArrayList<>(this.tickets.values());
		result.sort(Comparator.comparingInt(ticket -> number(ticket.id())));
		return Collections.unmodifiableList(result);
	}

	public Optional<Ticket> find(String id) {
		return Optional.ofNullable(this.tickets.get(id));
	}

	public Optional<Ticket> resolve(String id) {
		return find(id).map(ticket -> {
			Ticket resolved = store(new Ticket(ticket.id(), ticket.title(), true));
			this.events.publishEvent(new TicketChanged(id, TicketChanged.Kind.UPDATED));
			return resolved;
		});
	}

	public boolean delete(String id) {
		boolean deleted = this.tickets.remove(id) != null;
		if (deleted) {
			this.events.publishEvent(new TicketChanged(id, TicketChanged.Kind.DELETED));
		}
		return deleted;
	}

	public String toJson(List<Ticket> tickets) {
		return JSON.writeValueAsString(tickets);
	}

	/** {@code t-} 后面的数字，用于对 id 排序和分页。 */
	static int number(String id) {
		if (!id.startsWith("t-")) {
			throw new IllegalArgumentException("Not a ticket id: " + id);
		}
		return Integer.parseInt(id.substring(2));
	}

	private Ticket store(Ticket ticket) {
		this.tickets.put(ticket.id(), ticket);
		return ticket;
	}

}
