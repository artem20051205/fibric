package com.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ExampleClientMod implements ClientModInitializer {

	private static final String OLLAMA_URL = "http://127.0.0.1:11434/api/generate";
	private static final String MODEL = "deepseek-r1:1.5b";

	@Override
	public void onInitializeClient() {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			String text = message.getString();

			// Пример фильтра: отвечать только если написали "Player677" (замени на свой ник)
			if (!text.contains("Player677")) return;

			// Чтобы не отвечать на свои же сообщения (грубый вариант)
			if (text.startsWith("<Player677>")) return;

			new Thread(() -> {
				String answer = askOllama(text);
				if (answer == null || answer.isEmpty()) return;

				// чуть чистим/ограничиваем длину
				answer = answer.replace("\n", " ");
				if (answer.length() > 200) answer = answer.substring(0, 200);

				MinecraftClient client = MinecraftClient.getInstance();
				client.execute(() -> {
					if (client.player != null) {
						client.player.networkHandler.sendChatMessage("AI: " + answer);
					}
				});
			}).start();
		});
	}

	private static String askOllama(String prompt) {
		try {
			URL url = new URL(OLLAMA_URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setDoOutput(true);

			JsonObject body = new JsonObject();
			body.addProperty("model", MODEL);
			body.addProperty("prompt", prompt);
			body.addProperty("stream", false);

			byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
			try (OutputStream os = con.getOutputStream()) {
				os.write(out);
			}

			InputStream is = con.getResponseCode() < 300 ? con.getInputStream() : con.getErrorStream();
			String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);

			JsonObject json = JsonParser.parseString(text).getAsJsonObject();
			return json.has("response") ? json.get("response").getAsString() : text;
		} catch (Exception e) {
			return null;
		}
	}
}
