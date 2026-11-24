package com.example.tickets.client;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.modelcontextprotocol.modern.client.SubscriptionEvent;

import org.springframework.ai.mcp.modern.client.McpModernConnectionInfo;
import org.springframework.ai.mcp.modern.client.SyncModernMcpChangeListener;

/**
 * 打印 modern 连接的监听流所报告的内容。{@link SyncModernMcpChangeListener} Bean 会在
 * 工具回调提供者（它会丢弃已保留的工具列表）和 {@code @McpToolListChanged} 方法之外
 * 接收到这些报告。
 */
class ListenLog implements SyncModernMcpChangeListener {

	private final CountDownLatch established = new CountDownLatch(1);

	@Override
	public void changed(McpModernConnectionInfo connection, SubscriptionEvent event) {
		System.out.println("    [listen] " + connection.name() + ": " + event.getClass().getSimpleName());
	}

	@Override
	public void streamEstablished(McpModernConnectionInfo connection) {
		System.out.println("    [listen] " + connection.name() + ": stream established");
		this.established.countDown();
	}

	/**
	 * 等待第一条监听流。流在应用启动时于后台打开，无法访问的服务器不能拖住启动过程。
	 */
	boolean awaitEstablished(Duration timeout) throws InterruptedException {
		return this.established.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

}
