/*
* Copyright 2025 - 2025 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.springframework.ai.mcp.sample.server.providers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.ai.mcp.annotation.McpResource;

import org.springframework.stereotype.Service;

/**
 * @author Christian Tzolov
 */
@Service
public class UserProfileResourceProvider {

	private final Map<String, Map<String, String>> userProfiles = new HashMap<>();

	public UserProfileResourceProvider() {
		// 初始化一些示例数据
		Map<String, String> johnProfile = new HashMap<>();
		johnProfile.put("name", "John Smith");
		johnProfile.put("email", "john.smith@example.com");
		johnProfile.put("age", "32");
		johnProfile.put("location", "New York");

		Map<String, String> janeProfile = new HashMap<>();
		janeProfile.put("name", "Jane Doe");
		janeProfile.put("email", "jane.doe@example.com");
		janeProfile.put("age", "28");
		janeProfile.put("location", "London");

		Map<String, String> bobProfile = new HashMap<>();
		bobProfile.put("name", "Bob Johnson");
		bobProfile.put("email", "bob.johnson@example.com");
		bobProfile.put("age", "45");
		bobProfile.put("location", "Tokyo");

		Map<String, String> aliceProfile = new HashMap<>();
		aliceProfile.put("name", "Alice Brown");
		aliceProfile.put("email", "alice.brown@example.com");
		aliceProfile.put("age", "36");
		aliceProfile.put("location", "Sydney");

		userProfiles.put("john", johnProfile);
		userProfiles.put("jane", janeProfile);
		userProfiles.put("bob", bobProfile);
		userProfiles.put("alice", aliceProfile);
	}

	@McpResource(uri = "static://hello", name = "Static Resource", description = "Example static resource")
	public String staticResource() {
		return "Hello World!";
	}

	/**
	 * 接收 ReadResourceRequest 参数和 URI 变量的资源方法。
	 */
	@McpResource(uri = "user-profile://{username}", name = "User Profile",
			description = "Provides user profile information for a specific user")
	public ReadResourceResult getUserProfile(ReadResourceRequest request, String username) {
		String profileInfo = formatProfileInfo(userProfiles.getOrDefault(username.toLowerCase(), new HashMap<>()));

		return ReadResourceResult
			.builder(List.of(TextResourceContents.builder(request.uri(), profileInfo).mimeType("text/plain").build()))
			.build();
	}

	/**
	 * 直接将 URI 变量作为参数的资源方法。注解中的 URI 模板
	 * 定义了将被提取的变量。
	 */
	@McpResource(uri = "user-profile://{username}", name = "User Details",
			description = "Provides user details for a specific user using URI variables")
	public ReadResourceResult getUserDetails(String username) {
		String profileInfo = formatProfileInfo(userProfiles.getOrDefault(username.toLowerCase(), new HashMap<>()));

		return ReadResourceResult.builder(List
			.of(TextResourceContents.builder("user-profile://" + username, profileInfo).mimeType("text/plain").build()))
			.build();
	}

	/**
	 * 接收多个 URI 变量作为参数的资源方法。
	 */
	@McpResource(uri = "user-attribute://{username}/{attribute}", name = "User Attribute",
			description = "Provides a specific attribute from a user's profile")
	public ReadResourceResult getUserAttribute(String username, String attribute) {
		Map<String, String> profile = userProfiles.getOrDefault(username.toLowerCase(), new HashMap<>());
		String attributeValue = profile.getOrDefault(attribute, "Attribute not found");

		return ReadResourceResult
			.builder(
					List.of(TextResourceContents
						.builder("user-attribute://" + username + "/" + attribute,
								username + "'s " + attribute + ": " + attributeValue)
						.mimeType("text/plain")
						.build()))
			.build();
	}

	/**
	 * 接收交换上下文和 URI 变量的资源方法。
	 */
	@McpResource(uri = "user-profile-exchange://{username}", name = "User Profile with Exchange",
			description = "Provides user profile information with server exchange context")
	public ReadResourceResult getProfileWithExchange(McpSyncServerExchange exchange, String username) {
		String profileInfo = formatProfileInfo(userProfiles.getOrDefault(username.toLowerCase(), new HashMap<>()));

		return ReadResourceResult
			.builder(
					List.of(TextResourceContents
						.builder("user-profile-exchange://" + username,
								"Profile with exchange for " + username + ": " + profileInfo)
						.mimeType("text/plain")
						.build()))
			.build();
	}

	/**
	 * 接收字符串形式的 URI 变量参数的资源方法。
	 */
	@McpResource(uri = "user-connections://{username}", name = "User Connections",
			description = "Provides a list of connections for a specific user")
	public List<String> getUserConnections(String username) {
		// 根据用户名生成一个简单的连接列表
		return List.of(username + " is connected with Alice", username + " is connected with Bob",
				username + " is connected with Charlie");
	}

	/**
	 * 同时接收 McpSyncServerExchange、ReadResourceRequest 和 URI
	 * 变量参数的资源方法。
	 */
	@McpResource(uri = "user-notifications://{username}", name = "User Notifications",
			description = "Provides notifications for a specific user")
	public List<ResourceContents> getUserNotifications(ReadResourceRequest request, String username) {
		// 根据用户名生成通知
		String notifications = generateNotifications(username);

		return List.of(TextResourceContents.builder(request.uri(), notifications).mimeType("text/plain").build());
	}

	/**
	 * 返回单个 TEXT 内容类型的 ResourceContents 的资源方法。
	 */
	@McpResource(uri = "user-status://{username}", name = "User Status",
			description = "Provides the current status for a specific user")
	public ResourceContents getUserStatus(ReadResourceRequest request, String username) {
		// 根据用户名生成简单状态
		String status = generateUserStatus(username);

		return TextResourceContents.builder(request.uri(), status).mimeType("text/plain").build();
	}

	/**
	 * 返回单个 TEXT 内容类型的字符串的资源方法。
	 */
	@McpResource(uri = "user-location://{username}", name = "User Location",
			description = "Provides the current location for a specific user")
	public String getUserLocation(String username) {
		Map<String, String> profile = userProfiles.getOrDefault(username.toLowerCase(), new HashMap<>());

		// 从档案数据中提取位置
		return profile.getOrDefault("location", "Location not available");
	}

	/**
	 * 返回单个 BLOB 内容类型的字符串的资源方法。它演示了
	 * 如何将字符串视为二进制数据。
	 */
	@McpResource(uri = "user-avatar://{username}", name = "User Avatar",
			description = "Provides a base64-encoded avatar image for a specific user", mimeType = "image/png")
	public String getUserAvatar(ReadResourceRequest request, String username) {
		// 在实际实现中，这里会是 base64 编码的图片
		// 在本示例中，我们只返回一个占位字符串
		return "base64-encoded-avatar-image-for-" + username;
	}

	private String formatProfileInfo(Map<String, String> profile) {
		if (profile.isEmpty()) {
			return "User profile not found";
		}

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : profile.entrySet()) {
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		}
		return sb.toString().trim();
	}

	private String generateNotifications(String username) {
		// 生成通知的简单逻辑
		return "You have 3 new messages\n" + "2 people viewed your profile\n" + "You have 1 new connection request";
	}

	private String generateUserStatus(String username) {
		// 生成状态的简单逻辑
		if (username.equals("john")) {
			return "🟢 Online";
		}
		else if (username.equals("jane")) {
			return "🟠 Away";
		}
		else if (username.equals("bob")) {
			return "⚪ Offline";
		}
		else if (username.equals("alice")) {
			return "🔴 Busy";
		}
		else {
			return "⚪ Offline";
		}
	}

}
