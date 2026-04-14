package com.felipe.webhook_listener_api.service;

import com.felipe.webhook_listener_api.exception.InvalidPayloadException;
import com.felipe.webhook_listener_api.exception.InvalidSignatureException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SignatureValidationService {

	private static final String SIGNATURE_PREFIX = "sha256=";
	private static final HexFormat HEX_FORMAT = HexFormat.of();

	private final byte[] secretKey;

	public SignatureValidationService(@Value("${webhook.github.secret}") String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("webhook.github.secret must not be blank");
		}
		this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
	}

	public void validate(String signatureHeader, String rawBody) {
		if (signatureHeader == null || signatureHeader.isBlank()) {
			throw new InvalidPayloadException("X-Signature header is required");
		}

		if (!signatureHeader.startsWith(SIGNATURE_PREFIX) || signatureHeader.length() != SIGNATURE_PREFIX.length() + 64) {
			throw new InvalidPayloadException("X-Signature header must follow the format sha256=<hex>");
		}

		byte[] expectedSignature = sign(rawBody);
		byte[] receivedSignature;

		try {
			receivedSignature = HEX_FORMAT.parseHex(signatureHeader.substring(SIGNATURE_PREFIX.length()));
		} catch (IllegalArgumentException exception) {
			throw new InvalidPayloadException("X-Signature header must contain a valid hexadecimal signature");
		}

		// The signature must be calculated from the exact JSON body received by the endpoint.
		if (!MessageDigest.isEqual(expectedSignature, receivedSignature)) {
			throw new InvalidSignatureException("Invalid webhook signature");
		}
	}

	private byte[] sign(String rawBody) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
			return mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to validate webhook signature", exception);
		}
	}
}
