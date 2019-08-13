package pro.gravit.launchermodules.simpleobf.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pro.gravit.utils.helper.SecurityHelper;

public class RandomHelper {
	private final static char[] ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

	private final static char[] ALPHA_NUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
			.toCharArray();

	public static final int MAX_SAFE_BYTE_COUNT = 65535;

	public static final ThreadLocal<Random> RANDOM = new ThreadLocal<Random>() {
		@Override
		protected Random initialValue() {
			return SecurityHelper.newRandom();
		}
	};

	public static double getRandomDouble() {
		return RANDOM.get().nextDouble();
	}

	public static float getRandomFloat() {
		return RANDOM.get().nextFloat();
	}

	public static int getRandomInt() {
		return RANDOM.get().nextInt();
	}

	public static int getRandomInt(final int bounds) {
		return RANDOM.get().nextInt(bounds);
	}

	public static long getRandomLong() {
		return RANDOM.get().nextLong();
	}

	public static int getUtf8CharSize(final char c) {
		if (c >= 0x0001 && c <= 0x007F)
			return 1;
		else if (c <= 0x07FF)
			return 2;
		return 3;
	}

	public static int getUtf8StringSize(final String string) {
		int byteLength = 0;
		for (int i = 0; i < string.length(); ++i) {
			final char charValue = string.charAt(i);
			byteLength += getUtf8CharSize(charValue);
		}
		return byteLength;
	}

	public static String randomAlphaNumericString(final int length) {
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++)
			sb.append(ALPHA_NUM[getRandomInt(ALPHA_NUM.length)]);

		return sb.toString();
	}

	public static String randomAlphaString(final int length) {
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++)
			sb.append(ALPHA[getRandomInt(ALPHA.length)]);
		return sb.toString();
	}

	public static String[] splitUtf8ToChunks(final String text, final int maxBytes) {
		final List<String> parts = new ArrayList<>();

		final char[] chars = text.toCharArray();

		int lastCharIndex = 0;
		int currentChunkSize = 0;

		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];
			final int charSize = getUtf8CharSize(c);
			if (currentChunkSize + charSize < maxBytes)
				currentChunkSize += charSize;
			else {
				parts.add(text.substring(lastCharIndex, i));
				currentChunkSize = 0;
				lastCharIndex = i;
			}
		}

		if (currentChunkSize != 0)
			parts.add(text.substring(lastCharIndex));

		return parts.toArray(new String[0]);
	}
}
