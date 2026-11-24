package com.example.tickets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * 所有示例服务器共享的内存工单存储。它刻意保持很小：这些示例关注的是 MCP 协议，而不是工单。
 */
@Service
public class TicketService {

	public record Ticket(String id, String title, boolean resolved) {
	}

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

	private final AtomicInteger ids = new AtomicInteger();

	public TicketService() {
		open("Write the quarterly report");
		open("Review the MCP migration guide");
	}

	public Ticket open(String title) {
		Ticket ticket = new Ticket("t-" + ids.incrementAndGet(), title, false);
		this.tickets.put(ticket.id(), ticket);
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
			Ticket resolved = new Ticket(ticket.id(), ticket.title(), true);
			this.tickets.put(id, resolved);
			return resolved;
		});
	}

	public boolean delete(String id) {
		return this.tickets.remove(id) != null;
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

}
