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
import java.util.UUID;

public class ExampleClientMod implements ClientModInitializer {

	private static final String OLLAMA_URL = "http://127.0.0.1:11434/api/generate";
	private static final String MODEL = "deepseek-r1:1.5b";

	@Override
	public void onInitializeClient() {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, senderProfile, params, ts) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null) return;

			String text = message.getString();

			// не отвечать самому себе (по UUID отправителя)
			if (senderProfile != null) {
				UUID senderId = senderProfile.id();
				if (senderId != null && senderId.equals(client.player.getUuid())) return;
			}

			new Thread(() -> {
				String answer = askOllama(text);
				if (answer == null || answer.isEmpty()) return;

				answer = answer.replace("\n", " ").replace("\r", " ");
				if (answer.length() > 180) answer = answer.substring(0, 180);

				String finalAnswer = answer;

				client.execute(() -> {
					if (client.player != null) {
						client.player.networkHandler.sendChatMessage("AI: " + finalAnswer);
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

			try (OutputStream os = con.getOutputStream()) {
				os.write(body.toString().getBytes(StandardCharsets.UTF_8));
			}

			InputStream is = (con.getResponseCode() < 300)
					? con.getInputStream()
					: con.getErrorStream();

			String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			JsonObject json = JsonParser.parseString(text).getAsJsonObject();

			if (json.has("response")) return json.get("response").getAsString();
			return null;
		} catch (Exception e) {
			return null;
		}
	}
}
