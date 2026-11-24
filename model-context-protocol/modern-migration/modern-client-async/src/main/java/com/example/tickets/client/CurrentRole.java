package com.example.tickets.client;

import java.util.function.Supplier;

/**
 * 当前线程所服务的调用者的角色。它代表 Web 应用的安全上下文，在那里调用者在处理请求的
 * 线程上即可获知。
 */
final class CurrentRole {

	private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

	private CurrentRole() {
	}

	/** 当前调用者的角色；没有角色的调用者返回 {@code null}。 */
	static String get() {
		return ROLE.get();
	}

	/** 以具有给定角色的调用者身份运行操作。 */
	static <T> T callAs(String role, Supplier<T> action) {
		ROLE.set(role);
		try {
			return action.get();
		}
		finally {
			ROLE.remove();
		}
	}

}
